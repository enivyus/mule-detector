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
| `core-banking/` | Spring Boot 3.3, Java 21, Maven. Accounts, deposits, withdrawals, transfers, ledger, freeze. |
| `core-banking/src/main/resources/db/migration/` | Flyway migrations. Never edit an applied migration; add `V2__...sql`. |
| `simulator/simulate.py` | Generates `data/accounts.csv` and `data/transactions.csv` with ground-truth labels. Stdlib only. |
| `simulator/baseline_rules.py` | Simple rules scored against labels (precision/recall). The baseline to beat. |
| `docker-compose.yml` | postgres (default); redpanda, redpanda-console, neo4j under `--profile full`. |
| `data/` | Generated CSVs (git-ignored). |

## Commands

```bash
docker compose up -d postgres                 # DB for the API
cd core-banking && ./mvnw spring-boot:run       # API on :8080
cd core-banking && ./mvnw test                  # tests run on H2 (PostgreSQL mode), no Docker needed
cd simulator && python simulate.py            # generate data
cd simulator && python baseline_rules.py      # score baseline rules
docker compose --profile full up -d           # + Redpanda (:9092, console :8081) + Neo4j (:7474)
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

`TransferServiceTest.concurrentTransfersKeepBooksBalanced` fires 1,000 transfers from 16 threads and checks the
invariants. It must stay green.

## Simulator labels

`accounts.csv`: `role` ∈ customer, merchant, employer, mule_l1, mule_l2, cashout; `is_mule`; `ring_id`.
`transactions.csv`: `scenario` ∈ salary, bill, shopping, p2p, atm, funding, warmup, scam_victim, scam_layer, scam_cashout.
Scam pattern: 5–15 victims → L1 mule (sometimes 00:00–04:59) → 90–98% forwarded in 2–20 min to 2–3 L2 mules
→ cash-out account → withdrawal. ~10% of genuine customers are also new accounts, so "new account" alone is not proof.

Baseline (seed 42, 2,000 customers, 6 rings): precision ≈ 29%, recall 100%. Goal: raise precision without losing recall
(combine rules into a score, then add graph features).

## Detection rules (week 5 target, streaming)

| Rule | Signal |
|---|---|
| pass_through | ≥ 90% of an incoming amount leaves within a short window |
| fan_in | money from ≥ N distinct, unrelated senders in a short window |
| fan_out | money split to several accounts right after arriving |
| new_account_burst | young account with unusually high inflow |
| odd_hours | many transfers between 00:00 and 05:00 |

## Plan (10 weeks)

| Week | Work | Status |
|---|---|---|
| 1 | Research mule patterns, schema, docker-compose | ✅ scaffold done |
| 2–3 | Core banking API: accounts, transfers, ledger, idempotency, tests | ✅ tests green, Swagger, paged statement, verified on Postgres |
| 4 | Simulator: post generated transactions to the API (not only CSV) | 🟡 CSV version done |
| 5 | Publish events to Redpanda, streaming rule engine | ⬜ |
| 6–7 | Neo4j: load graph, community detection (Louvain), money-trail query | ⬜ |
| 8 | Risk score, alerts, auto-freeze | ⬜ |
| 9 | Investigator dashboard | ⬜ |
| 10 | Measure, README with numbers, demo video | ⬜ |

Keep the MVP working end-to-end before adding Kafka/Neo4j.

## Conventions

- Synthetic data only. Never use real names, real account numbers or real bank data.
- Small, focused commits with clear messages (the commit history is part of the portfolio).
- Every new rule or score change: rerun the simulator + evaluation and record precision/recall in the README.
