# Mule Account Detector

A mini core banking system and a detector for **mule accounts**: bank accounts that receive money
from scam victims and quickly pass it through more accounts before it is cashed out.

> Portfolio project. All data is synthetic.

## In one minute

- **A real little bank:** Spring Boot + PostgreSQL, double-entry ledger, row locks in a fixed order,
  idempotency keys. 1,000 concurrent transfers balance to the satang.
- **A month of fake but hard data:** 2,000 customers, six scam rings. 40% of the mule accounts are old
  accounts rented from real customers, half the rings work office hours, and some honest people bank at 2 a.m.
- **Detection in three layers:** streaming rules over Redpanda, then a graph step in Neo4j (suspicion follows
  the money), then two risk tiers: *freeze* now, or *review* by a person.
- **Final result on a seed never looked at before (9999):**

  | | Precision | Recall |
  |---|---|---|
  | Simple rules (baseline) | 32% | 100% |
  | Two-rule score, one account at a time | 91% | 87% |
  | **Score + graph** | **87%** | **100%** |
  | **Auto-freeze tier** (3+ rules) | **100%** (27 mules, 0 genuine accounts frozen) | 59% |

  Every mule lands in one of the two tiers; the review queue holds 26 accounts, 7 of them genuine.
- **Money, measured live:** replaying seed 9999 into an empty bank with auto-freeze on, the bank froze 23
  accounts (all mules, no genuine customer) and refused **89% of the rings' cash-out withdrawals**
  (1,283,410 of 1,448,206 THB) plus 30 victim payments (661,348 THB) that were still on their way to a mule.
- **Honest about the numbers:** each rule exists in two implementations (stream/graph and a batch script) that
  must agree account by account; thresholds were chosen on seed 42, and each new result was scored once on a
  fresh seed. The first, easier version of the simulator flattered the detector; this README says by how much.

## Why

Mule accounts are how stolen money disappears. A victim transfers to account A, and within minutes the money
is split across B, C and D and withdrawn. Catching this means looking at *how money moves between accounts*,
not at single transactions.

## Architecture

```
Transaction simulator ──► Core banking API ──► Event stream ──► Rule engine + Graph analysis ──► Risk score ──► Dashboard
   (labeled scams)       (double-entry ledger)   (Redpanda)         (real-time)    (Neo4j)       auto-freeze    follow the money
```

## What works today

**Core banking API** (`core-banking/`, Spring Boot 3, Java 21, PostgreSQL)
- Open account, deposit, withdraw, transfer, paged statement (`?page=0&size=50`, newest first), freeze/unfreeze
- Double-entry ledger: every transfer is one DEBIT + one CREDIT; balances can be rebuilt from the ledger
- Row-level locking in a fixed order: no lost updates, no deadlocks
- Idempotency keys: a retried request never moves money twice
- Money stored as integer satang, never floating point
- Concurrency test: 1,000 simultaneous transfers, books must balance to the satang
- Every caller logs in (HTTP Basic, Spring Security), and each role can do only its own job:

  | Role | User | Can |
  |---|---|---|
  | `CHANNEL` | the bank's app / ATM / branch (here: the simulator) | open accounts, move money, read accounts |
  | `INVESTIGATOR` | fraud staff (the dashboard) | read accounts and statements, freeze, unfreeze, read the audit log |
  | `RULE_ENGINE` | the rule engine | freeze only; lifting a freeze always takes a person |

- **Audit log:** every freeze and unfreeze stores who did it (the logged-in user, never a field the caller sends),
  when and why. A reason is required; the rule engine sends the rules that fired. There is no API to change or
  delete a row.
- rule-engine and graph-service name suspected accounts, so they need a login too; graph-service logs in to the
  rule engine with its own user.
- Still a prototype: Basic over plain HTTP with users from config, so every service listens on 127.0.0.1 only.
  Before exposing anything: TLS and tokens from the bank's identity provider (OIDC). The channel is trusted to act
  for any customer; a real one would carry the customer's own login.

**Transaction simulator** (`simulator/`, Python)
- A month of normal activity for 2,000 customers: salary, bills, shopping, transfers between friends
- Hidden scam rings: victims → layer-1 mules → layer-2 mules → cash-out
- Made hard on purpose: about 40% of mule accounts are old accounts rented from real customers (they keep
  their owner's everyday activity), half the rings work office hours, and 5% of genuine customers are
  night owls who also bank after midnight
- Ground-truth labels for every account, so detection can be measured
- Replay through the real API (`replay.py`) with the original timestamps: about 2,100 accounts and 53,000
  transactions in about 11 minutes, every balance checked against the CSV afterwards

**Event stream + rule engine** (`rule-engine/`, Spring Boot, Redpanda)
- core-banking publishes every committed transfer to topic `transactions` (after commit, so a rolled-back
  transfer is never published)
- The rule engine consumes the stream and runs five rules per account in event time (Bangkok days):
  pass-through, fan-in, fan-out, new-account burst, odd hours
- An account is a **suspected mule when at least two different rules fire** (`GET /suspects`): one rule
  alone is a common coincidence for ordinary customers, two independent signals rarely are
- Parity check: on the full month, every event reaches the engine (same count in the database, the
  topic and the engine), and it flags exactly the same accounts as the batch reference, rule by rule

**Graph analysis** (`graph-service/`, Spring Boot, Neo4j + Graph Data Science)
- Loads the same `transactions` topic into Neo4j as `(:Account)-[:SENT]->(:Account)` with its own consumer
  group (idempotent: re-reading the whole topic leaves 52,622 relationships)
- **Suspicion follows the money** (`GET /suspects`): an account that receives at least 1,000 THB from a suspect
  and passes 90% of it on within 2 hours gets the signal `from_suspect`; repeated until nothing changes.
  Such an account always breaks pass-through too, so `from_suspect` alone makes it a suspect: in effect it
  counts as one strong signal, not as two independent ones
- **Follow the money** (`GET /trail/{transferId}`): victim → layer-1 → layer-2 → cash-out → withdrawal,
  each hop later than the one before and within 2 hours; shops and the cash vault are not hops
- **Communities** (`POST /communities`): Louvain over transfers between personal accounts, weighted by amount

**Risk tiers and auto-freeze** (`rule-engine`, profile `autofreeze`)
- **Freeze:** an account that breaks at least **three** different stream rules is frozen in core-banking the
  moment the third rule fires (`POST /api/admin/accounts/{no}/freeze`), while the money is still moving
- **Review queue:** two rules, or suspicion from the graph, never freezes; an investigator decides
- On seed 42 and on the unseen seed 5150, **no genuine account reaches the freeze tier** (26 and 23 accounts
  frozen, all mules); the review queue holds the rest (30 accounts with 8 genuine, 39 with 7); no mule is in
  neither tier
- Replaying the topic after a restart never freezes again, so an account an investigator unfroze stays unfrozen

**Investigator dashboard** (`dashboard/`, React + Cytoscape.js)
- Thai / English switch; every signal explained in plain words (hover a chip, or read "why this account was flagged")
- Overview cards, then the review queue of every suspect with tabs (freeze tier, review, frozen) and search
- Sign-in for investigators only; credentials stay in memory, so a reload asks again
- Freeze / unfreeze with a confirmation step and a required reason, and the account's freeze history
  (who, when, why) from the audit log
- Account view: balance, signals, a timeline of money in and out; click an incoming payment to draw
  **follow the money** left to right (suspects red, frozen accounts outlined, edge width = amount), click any
  node to jump to it
- Light and dark mode, works on a narrow screen
- Louvain community of the account and the other suspects in it
- Shows only what the bank knows: no simulator labels reach the dashboard

**Live demo** (seed 9999, `replay.py --live` with auto-freeze on): 23 accounts frozen, all mules (13 layer-1,
4 layer-2, 6 cash-out). The bank refused 89% of the rings' cash-outs and 30 victim payments still heading to a
mule. 68 everyday transfers were refused too, all touching those frozen mule accounts: the real owners of
rented accounts still use them, and friends still pay them.

**Money stopped, estimated from alerts** (seed 42, `evaluate_freeze.py`, lower bound): the rings stole 1,985,407 THB and cashed out
1,764,355 THB. Freezing at detection time would have blocked at least **1,059,527 THB (60%)** of those cash-outs,
counting only withdrawals by the frozen account itself. In 5 of 6 rings an account was frozen 8 to 52 minutes
before the ring's first cash-out; in the sixth, 67 minutes after it.

**Detection results** (seed 42: 2,000 customers, 6 rings, 48 mule accounts, 20 of them rented;
streaming and batch identical)

| Version | Flagged | Precision | Recall |
|---|---|---|---|
| Baseline: any of pass-through, fan-in, new-account burst | 171 | 28% | 100% |
| Any of the five rules | 229 | 21% | 100% |
| **Score: at least two rules** | **44** | **89%** | **81%** |
| Score on seed 4242, never looked at before this run | 55 | 98% | 95% |
| **Score + graph (`from_suspect`)** | **56** | **86%** | **100%** |
| Score / score + graph on seed 1313, never looked at before this run | 55 / 62 | 93% / 92% | 89% / **100%** |

Single rules on seed 42: pass-through 31% precision at 100% recall (ordinary people also spend money they
just received), new-account burst 65% / 58% (rented mules are old accounts), odd hours 22% / 35% (night
owls, daytime rings), fan-in and fan-out 100% precision but only 15% / 40% recall.

**What the graph adds.** Account by account, the score misses rented, old layer-2 and cash-out accounts
whose only signal is pass-through: they look like a customer who spends their salary fast. What gives them
away is where the money came from. Following the money from suspects catches all of them (9 on seed 42,
6 on the unseen seed 1313) at the cost of a few genuine accounts that also received money from a suspect
and passed it on (3 and 1).

Louvain puts nearly every ring into a single community (47 of 48 mules on seed 42), but each
of those communities also holds 18 to 37 genuine accounts, so it stays an investigator aid (who else is
around this suspect?) and is not part of the score.

History: on the first, easier simulator (every mule a new account, attacks starting at midnight, nobody else
awake at night) the same score showed 98% / 100%. Making the data honest cost 19 points of recall.

## Run it

```bash
docker compose up -d postgres
cd core-banking && ./mvnw spring-boot:run
```

API docs and a try-it-out UI: http://localhost:8080/swagger-ui.html (OpenAPI spec at `/v3/api-docs`; use
"Authorize" to log in).

Local dev users, the same in every service (set `CHANNEL_PASSWORD`, `INVESTIGATOR_PASSWORD`,
`RULE_ENGINE_PASSWORD` and `GRAPH_SERVICE_PASSWORD` to change them; the scripts read the same variables):

| User | Password | Used by |
|---|---|---|
| `channel` | `channel-dev` | the simulator, curl below |
| `investigator` | `investigator-dev` | the dashboard, the `evaluate_*.py` scripts |
| `rule-engine` | `rule-engine-dev` | the rule engine, to freeze |
| `graph-service` | `graph-service-dev` | graph-service, to read alerts |

```bash
# Open two accounts
curl -u channel:channel-dev -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"ownerName":"Alice"}'
curl -u channel:channel-dev -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"ownerName":"Bob"}'

# Deposit 1,000.00 THB, then transfer 250.00 THB (amounts are in satang)
curl -u channel:channel-dev -X POST localhost:8080/api/accounts/<ALICE_NO>/deposit -H "Idempotency-Key: dep-1" \
     -H "Content-Type: application/json" -d '{"amountSatang":100000}'
curl -u channel:channel-dev -X POST localhost:8080/api/transfers -H "Idempotency-Key: tr-1" -H "Content-Type: application/json" \
     -d '{"fromAccountNo":"<ALICE_NO>","toAccountNo":"<BOB_NO>","amountSatang":25000}'

# Freeze Bob as an investigator, then read who did it and why
curl -u investigator:investigator-dev -X POST localhost:8080/api/admin/accounts/<BOB_NO>/freeze \
     -H "Content-Type: application/json" -d '{"reason":"victim reported the transfer"}'
curl -u investigator:investigator-dev localhost:8080/api/admin/accounts/<BOB_NO>/audit
```

```bash
cd simulator
python simulate.py          # writes data/accounts.csv, data/transactions.csv
python baseline_rules.py    # precision / recall of the baseline rules
python scored_rules.py      # five rules + score (the batch reference for the rule engine)
```

To load the same month into the bank, start the API with the `sim` profile. Only that profile
accepts the `X-Simulated-Time` header, which keeps each transaction's original time:

```bash
cd core-banking && ./mvnw spring-boot:run -Dspring-boot.run.profiles=sim
cd simulator && python replay.py   # safe to rerun: idempotency keys + data/account_map_<hash>.csv
```

To run the streaming detector, add Redpanda and the `kafka` profile, start the rule engine, then replay
into a fresh database (replaying data the bank already holds publishes nothing, by design):

```bash
docker compose up -d postgres redpanda redpanda-console      # console: http://localhost:8081
cd core-banking && ./mvnw spring-boot:run -Dspring-boot.run.profiles=sim,kafka
cd rule-engine && ./mvnw spring-boot:run                     # curl -u investigator:investigator-dev 127.0.0.1:8082/alerts
cd simulator && python replay.py && python evaluate_stream.py   # after a rule change, just restart the engine
```

Graph analysis reads the same topic, so no new replay is needed:

```bash
docker compose --profile full up -d neo4j                    # browser: http://localhost:7474
cd graph-service && ./mvnw spring-boot:run                   # curl -u investigator:investigator-dev 127.0.0.1:8083/suspects
cd simulator && python graph_rules.py && python evaluate_graph.py
```

The dashboard talks to all three services through the Vite dev server:

```bash
cd dashboard && npm install && npm run dev                   # http://127.0.0.1:5173, log in as investigator
```

Auto-freeze is off by default (a frozen account would break a replay). To turn it on for live traffic:

```bash
cd rule-engine && ./mvnw spring-boot:run -Dspring-boot.run.profiles=autofreeze   # frozen: curl -u investigator:investigator-dev 127.0.0.1:8082/freezes
cd simulator && python evaluate_freeze.py   # what freezing would have stopped, from the alerts already there
```

For a live demo, replay a fresh dataset into an empty bank while auto-freeze is on. `--live` counts the transfers
the bank refuses (frozen accounts, and accounts that never received the money a frozen one would have passed on)
instead of stopping:

```bash
cd simulator && python simulate.py --seed 9999 --out ../data/demo && python replay.py --data ../data/demo --live
```

## Roadmap

- [x] Core banking API with ledger, locking, idempotency
- [x] Labeled transaction simulator + baseline rules
- [x] Simulator posts to the API
- [x] Streaming rule engine on Redpanda (baseline rules, verified identical to batch)
- [x] More streaming rules (fan-out, odd hours) combined into a score
- [x] Harder simulator: old rented mule accounts, daytime rings, night-owl customers
- [x] Graph analysis in Neo4j (suspicion follows the money, money trail, Louvain communities)
- [x] Risk tiers, review queue, auto-freeze (60% of cash-outs stopped, no genuine account frozen)
- [x] Investigator dashboard (review queue, freeze/unfreeze, follow-the-money graph, communities)
- [x] Login and roles in every service, audit log of every freeze and unfreeze
