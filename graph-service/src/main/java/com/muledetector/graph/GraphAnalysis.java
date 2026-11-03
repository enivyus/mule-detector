package com.muledetector.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

/**
 * Questions about the money graph in Neo4j:
 *  - suspects(): rule-engine's suspects, then suspicion that follows the money (simulator/graph_rules.py
 *    is the batch reference and must stay identical);
 *  - trail(): where the money of one transfer went next;
 *  - communities(): Louvain communities of personal accounts, for investigators.
 * The cash vault (SYSTEM) and BUSINESS accounts are hubs that everybody touches, so trails stop at the
 * vault and communities leave both out.
 */
@Service
public class GraphAnalysis {

    static final String FROM_SUSPECT = "from_suspect";
    static final int SCORE_THRESHOLD = 2;          // same as RuleEngine.SCORE_THRESHOLD
    static final int TRAIL_MAX_DEPTH = 4;

    /** Personal accounts that got >= 1,000 THB from a suspect and passed >= 90% of that amount on within 2 h. */
    static final String FROM_SUSPECT_QUERY = """
            MATCH (s:Account)-[in:SENT]->(m:Account {type: 'CUSTOMER'})
            WHERE s.accountNo IN $suspects AND in.amountSatang >= 100000 AND NOT m.accountNo IN $suspects
            CALL {
              WITH m, in
              OPTIONAL MATCH (m)-[out:SENT]->()
              WHERE out.at >= in.at AND out.at <= in.at + duration('PT2H')
              RETURN coalesce(sum(out.amountSatang), 0) AS forwarded
            }
            WITH m, in, forwarded WHERE forwarded * 10 >= in.amountSatang * 9
            RETURN DISTINCT m.accountNo AS accountNo
            """;

    static final String TRAIL_START = """
            MATCH (a:Account)-[s:SENT {transferId: $id}]->(b:Account)
            RETURN s.transferId AS transferId, a.accountNo AS fromAccountNo, b.accountNo AS toAccountNo,
                   b.type AS toAccountType, s.amountSatang AS amountSatang, toString(s.at) AS at
            """;

    /** Next hops: money leaving the account after it arrived, within 2 h; paying a shop is not a hop. */
    static final String TRAIL_NEXT = """
            MATCH (a:Account {accountNo: $account})-[s:SENT]->(b:Account)
            WHERE s.at > datetime($at) AND s.at <= datetime($at) + duration('PT2H') AND b.type <> 'BUSINESS'
            RETURN s.transferId AS transferId, a.accountNo AS fromAccountNo, b.accountNo AS toAccountNo,
                   b.type AS toAccountType, s.amountSatang AS amountSatang, toString(s.at) AS at
            ORDER BY s.at
            """;

    public record Hop(long transferId, String fromAccountNo, String toAccountNo, String toAccountType,
                      long amountSatang, String at, int depth) {}

    private final Driver neo4j;
    private final RestClient ruleEngine;

    public GraphAnalysis(Driver neo4j, @Value("${mulebank.rule-engine-url}") String ruleEngineUrl,
                         @Value("${mulebank.rule-engine-password}") String password) {
        this.neo4j = neo4j;
        this.ruleEngine = RestClient.builder().baseUrl(ruleEngineUrl)
                .defaultHeaders(h -> h.setBasicAuth("graph-service", password)).build();
    }

    // shortcut: runs on request over the whole graph, because an upstream account can become a suspect after
    // the downstream transfer already happened; run it on a schedule and store the result once it gets slow
    public Map<String, Set<String>> suspects() {
        List<Map<String, Object>> alerts = ruleEngine.get().uri("/alerts").retrieve()
                .body(new ParameterizedTypeReference<>() {});
        Map<String, Set<String>> rules = new TreeMap<>();
        for (Map<String, Object> a : Objects.requireNonNull(alerts)) {
            rules.computeIfAbsent((String) a.get("accountNo"), k -> new TreeSet<>()).add((String) a.get("rule"));
        }

        Set<String> current = suspectsOf(rules);
        while (true) {
            for (Record r : neo4j.executableQuery(FROM_SUSPECT_QUERY)
                    .withParameters(Map.of("suspects", List.copyOf(current))).execute().records()) {
                rules.computeIfAbsent(r.get("accountNo").asString(), k -> new TreeSet<>()).add(FROM_SUSPECT);
            }
            Set<String> grown = suspectsOf(rules);
            if (grown.equals(current)) break;  // suspicion only grows, so this always stops
            current = grown;
        }
        Map<String, Set<String>> out = new TreeMap<>(rules);
        out.keySet().retainAll(current);
        return out;
    }

    private static Set<String> suspectsOf(Map<String, Set<String>> rules) {
        Set<String> s = new TreeSet<>();
        rules.forEach((no, rs) -> { if (rs.size() >= SCORE_THRESHOLD) s.add(no); });
        return s;
    }

    /**
     * Follow the money of one transfer forward, hop by hop, up to TRAIL_MAX_DEPTH hops; stops at the vault.
     * Money is fungible, so a hop is any personal transfer that left within 2 h after the money arrived,
     * which can include other victims' money passing through the same mule.
     */
    public List<Hop> trail(long transferId) {
        List<Hop> hops = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Deque<Hop> frontier = new ArrayDeque<>();
        for (Record r : neo4j.executableQuery(TRAIL_START).withParameters(Map.of("id", transferId)).execute().records()) {
            frontier.add(hop(r, 0));
        }
        while (!frontier.isEmpty()) {
            Hop h = frontier.removeFirst();
            if (!seen.add(h.transferId())) continue;
            hops.add(h);
            if (h.depth() >= TRAIL_MAX_DEPTH || "SYSTEM".equals(h.toAccountType())) continue;  // cashed out
            for (Record r : neo4j.executableQuery(TRAIL_NEXT)
                    .withParameters(Map.of("account", h.toAccountNo(), "at", h.at())).execute().records()) {
                frontier.add(hop(r, h.depth() + 1));
            }
        }
        return hops;
    }

    private static Hop hop(Record r, int depth) {
        return new Hop(r.get("transferId").asLong(), r.get("fromAccountNo").asString(), r.get("toAccountNo").asString(),
                r.get("toAccountType").asString(), r.get("amountSatang").asLong(), r.get("at").asString(), depth);
    }

    /** Louvain over transfers between personal accounts, weighted by amount; writes `community` on each node. */
    public Map<String, Long> communities() {
        neo4j.executableQuery("CALL gds.graph.drop('mule-tx', false) YIELD graphName RETURN graphName").execute();
        neo4j.executableQuery("""
                MATCH (a:Account {type: 'CUSTOMER'})-[s:SENT]->(b:Account {type: 'CUSTOMER'})
                WITH gds.graph.project('mule-tx', a, b, {relationshipProperties: {amount: toFloat(s.amountSatang)}},
                                       {undirectedRelationshipTypes: ['*']}) AS g
                RETURN g.graphName
                """).execute();
        try {
            neo4j.executableQuery("""
                    CALL gds.louvain.write('mule-tx', {writeProperty: 'community', relationshipWeightProperty: 'amount'})
                    YIELD communityCount RETURN communityCount
                    """).execute();
        } finally {
            neo4j.executableQuery("CALL gds.graph.drop('mule-tx', false) YIELD graphName RETURN graphName").execute();
        }
        Map<String, Long> out = new TreeMap<>();
        for (Record r : neo4j.executableQuery(
                "MATCH (a:Account) WHERE a.community IS NOT NULL RETURN a.accountNo AS no, a.community AS c").execute().records()) {
            out.put(r.get("no").asString(), r.get("c").asLong());
        }
        return out;
    }
}
