package com.muledetector.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.neo4j.driver.Driver;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Copies every committed transfer from topic "transactions" into Neo4j as
 * (:Account)-[:SENT {transferId, amountSatang, kind, at}]->(:Account).
 * MERGE on accountNo and transferId makes re-reading the topic harmless. No labels are stored.
 */
@Component
public class GraphLoader {

    static final String UPSERT = """
            UNWIND $rows AS r
            MERGE (a:Account {accountNo: r.fromAccountNo})
              ON CREATE SET a.type = r.fromAccountType, a.openedAt = datetime(r.fromOpenedAt)
            MERGE (b:Account {accountNo: r.toAccountNo})
              ON CREATE SET b.type = r.toAccountType, b.openedAt = datetime(r.toOpenedAt)
            MERGE (a)-[s:SENT {transferId: r.transferId}]->(b)
              ON CREATE SET s.amountSatang = r.amountSatang, s.kind = r.kind, s.at = datetime(r.occurredAt)
            """;

    private final Driver neo4j;
    private final ObjectMapper json;

    public GraphLoader(Driver neo4j, ObjectMapper json) {
        this.neo4j = neo4j;
        this.json = json;
    }

    /** Before the first write: without the constraint every MERGE scans all accounts. */
    @PostConstruct
    void createConstraint() {
        neo4j.executableQuery("CREATE CONSTRAINT account_no IF NOT EXISTS FOR (a:Account) REQUIRE a.accountNo IS UNIQUE")
                .execute();
        // Money trails start from one transfer
        neo4j.executableQuery("CREATE INDEX sent_transfer_id IF NOT EXISTS FOR ()-[s:SENT]-() ON (s.transferId)").execute();
    }

    // shortcut: the consumer group's committed offsets live in Redpanda; if Neo4j is wiped but the topic is not,
    // delete the group (rpk group delete graph-loader) so the loader starts again from offset 0
    @KafkaListener(topics = "transactions", groupId = "graph-loader", batch = "true")
    public void load(List<String> messages) throws JsonProcessingException {
        List<Map<String, Object>> rows = new ArrayList<>(messages.size());
        for (String m : messages) rows.add(json.readValue(m, new TypeReference<>() {}));
        neo4j.executableQuery(UPSERT).withParameters(Map.of("rows", rows)).execute();
    }
}
