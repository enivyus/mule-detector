# Mule Account Detector

Portfolio project for a software engineering student applying for internships (banks, fintech, tech).
The owner communicates in Thai — reply in Thai, keep code, comments and commit messages in English.

## What this project is

A mini bank plus a system that detects **mule accounts** (บัญชีม้า): accounts that receive money
from scam victims and pass it on through more accounts within minutes so it can be cashed out.

```
Transaction simulator (Python, labeled scam rings)
        │  REST
        ▼
Core banking API (Spring Boot, PostgreSQL, double-entry ledger)
        │  TransferCompletedEvent → Kafka/Redpanda topic "transactions"   (week 5)
        ▼
 ┌──────────────────────┬──────────────────────────┐
 Real-time rule engine   Graph analysis (Neo4j)       (weeks 5–7)
 └──────────┬───────────┴──────────────────────────┘
            ▼
 Risk scoring + alerts → auto-freeze via /api/admin/.../freeze   (week 8)
            ▼
 Investigator dashboard (React + Cytoscape.js, "follow the money") (week 9)
```

## Repository layout

| Path | What |
|---|---|
| `core-banking/` | Spring Boot 3.3, Java 21, Maven. Accounts (CUSTOMER/BUSINESS), transfers, ledger, freeze. Profiles: `sim`, `kafka`. |
| `core-banking/src/main/resources/db/migration/` | Flyway migrations. Never edit an applied migration; add `V2__...sql`. |
| `simulator/simulate.py` | Generates `data/accounts.csv` and `data/transactions.csv` with ground-truth labels. Stdlib only. |
| `simulator/baseline_rules.py` | Simple rules scored against labels (precision/recall). The baseline to beat. |
| `simulator/replay.py` | Replays the CSVs through the API (sim profile, `X-Simulated-Time`), writes `data/account_map_<hash>.csv`, verifies balances. |
| `simulator/scored_rules.py` | Batch reference: baseline rules + fan_out + odd_hours, suspect when ≥ 2 rules fire. |
| `simulator/evaluate_stream.py` | Scores `rule-engine` alerts against labels and checks parity with `scored_rules.py`. |
| `simulator/graph_rules.py` | Batch reference for the graph step: `from_suspect`, iterated to a fixed point. |
| `simulator/evaluate_graph.py` | Scores graph-service, parity with `graph_rules.py`, Louvain per ring, one money trail. |
| `graph-service/` | Spring Boot :8083. Topic → Neo4j (`GraphLoader`); `GraphAnalysis`: `GET /suspects`, `GET /trail/{transferId}`, `POST /communities`. |
| `rule-engine/` | Spring Boot :8082. Consumes topic `transactions`, streaming rules + score in `RuleEngine`, `GET /alerts`, `/suspects`, `/stats`. |
| `docker-compose.yml` | postgres (default); redpanda, redpanda-console, neo4j under `--profile full`. |
| `data/` | Generated CSVs (git-ignored). |

## Commands

```bash
docker compose up -d postgres                 # DB for the API
cd core-banking && ./mvnw spring-boot:run       # API on :8080
cd core-banking && ./mvnw test                  # tests run on H2 (PostgreSQL mode), no Docker needed
cd simulator && python simulate.py            # generate data
cd simulator && python baseline_rules.py      # score baseline rules
cd simulator && python replay.py              # load CSVs into the API (API must run with -Dspring-boot.run.profiles=sim)
docker compose --profile full up -d           # + Redpanda (:9092, console :8081) + Neo4j (:7474)
cd core-banking && ./mvnw spring-boot:run -Dspring-boot.run.profiles=sim,kafka   # publish events
cd rule-engine && ./mvnw spring-boot:run        # streaming rules on :8082
cd simulator && python evaluate_stream.py      # precision/recall + parity with the batch baseline
cd graph-service && ./mvnw spring-boot:run      # topic -> Neo4j, graph suspects on :8083
cd simulator && python evaluate_graph.py       # graph suspects + parity, Louvain per ring, a money trail
```

## Core banking invariants — do not break these

1. **Money is `long` satang** (1 THB = 100 satang). Never `float`/`double`. API fields end in `Satang`.
2. **Double-entry**: every transfer writes exactly one DEBIT and one CREDIT ledger row of the same amount.
   `ledger_entries` is the source of truth; `accounts.balance_satang` is a cache updated in the same transaction.
   `LedgerEntryRepository.globalImbalance()` must always be 0.
3. **Deposits/withdrawals are transfers** to/from the SYSTEM cash vault account `0000000000` (the only account allowed below zero).
4. **Locking**: both accounts are locked with `SELECT ... FOR UPDATE` (`findByIdForUpdate`) in ascending id order to avoid deadlocks.
5. **Look up ids, not entities, before locking** (`findIdByAccountNo`). If an `Account` entity is already in the
   persistence context, Hibernate returns that stale copy from the locking query and concurrent updates are lost.
6. **Idempotency**: every money-moving endpoint requires an `Idempotency-Key` header. Same key + same payload → return
   the original transfer; same key + different payload → 409.
7. `TransferCompletedEvent` is published inside the transaction. Any listener that talks to Kafka must use
   `@TransactionalEventListener(phase = AFTER_COMMIT)` so rolled-back transfers are never published.
8. **Simulated time**: `X-Simulated-Time` may set `created_at` only under the `sim` profile; otherwise 400. Never let
   production clients choose booking times.

`TransferServiceTest.concurrentTransfersKeepBooksBalanced` fires 1,000 transfers from 16 threads and checks the
invariants. It must stay green.

## Simulator labels

`accounts.csv`: `role` ∈ customer, merchant, employer, mule_l1, mule_l2, cashout; `is_mule`; `ring_id`.
`transactions.csv`: `scenario` ∈ salary, bill, shopping, p2p, atm, funding, warmup, scam_victim, scam_layer, scam_cashout.
Scam pattern: 5–15 victims → L1 mule (sometimes 00:00–04:59) → 90–98% forwarded in 2–20 min to 2–3 L2 mules
→ cash-out account → withdrawal. ~10% of genuine customers are also new accounts, so "new account" alone is not proof.
Customers start with savings (40k–400k THB) and any transaction that would overdraw is dropped, so the CSV replays
cleanly through the real ledger.
Harder on purpose: ~40% of mule accounts are existing customers' accounts rented for the ring (old, with normal
activity; no warm-up), attacks start between 08:00 and 16:00 and each ring sends 0%, 35% or 60% of victims at night,
and ~5% of genuine customers are night owls. Amounts are integer satang (`_baht` uses `randint`, no float).

Seed 42, 2,000 customers, 6 rings, 48 mules (20 rented): baseline precision ≈ 28%, recall 100%; score (≥ 2 rules)
≈ 89% / 81%; unseen seed 4242: 98% / 95%. The missed mules are rented L2/cash-out accounts with only pass_through:
graph step (`from_suspect`): seed 42 86% / 100%, unseen seed 1313 92% / 100% (score alone 93% / 89%).

## Detection rules (streaming in `RuleEngine`, batch reference in `scored_rules.py` — keep them identical)

| Rule | Signal |
|---|---|
| pass_through | ≥ 90% of an incoming ≥ 1,000 THB leaves within 2 h |
| fan_in | ≥ 4 distinct senders (cash deposits excluded) within any 2 h |
| fan_out | an incoming ≥ 1,000 THB followed within 1 h by transfers to ≥ 2 different personal accounts |
| new_account_burst | account < 30 days old receives ≥ 20,000 THB from others in one Bangkok day |
| odd_hours | ≥ 2 transfers in or out between 00:00 and 05:00 Bangkok |
| from_suspect (graph, batch) | receives ≥ 1,000 THB from a suspect and ≥ 90% of that amount leaves within 2 h; iterate |

Only personal (CUSTOMER) accounts are judged. Suspected mule = at least 2 different rules (`SCORE_THRESHOLD`).
Any rule change goes into both implementations; `evaluate_stream.py` must still print "identical".

## Plan (10 weeks)

| Week | Work | Status |
|---|---|---|
| 1 | Research mule patterns, schema, docker-compose | ✅ scaffold done |
| 2–3 | Core banking API: accounts, transfers, ledger, idempotency, tests | ✅ tests green, Swagger, paged statement, verified on Postgres |
| 4 | Simulator: post generated transactions to the API (not only CSV) | ✅ replay.py, sim-time header, balances verified |
| 5 | Publish events to Redpanda, streaming rule engine | ✅ publisher, 5 streaming rules + score, parity with batch verified |
| 6–7 | Neo4j: load graph, community detection (Louvain), money-trail query | ✅ from_suspect, trail, Louvain; parity verified |
| 8 | Risk score, alerts, auto-freeze | ⬜ |
| 9 | Investigator dashboard | ⬜ |
| 10 | Measure, README with numbers, demo video | ⬜ |

Keep the MVP working end-to-end before adding Kafka/Neo4j.

## Conventions

- Synthetic data only. Never use real names, real account numbers or real bank data.
- Small, focused commits with clear messages (the commit history is part of the portfolio).
- Every new rule or score change: rerun the simulator + evaluation and record precision/recall in the README.
