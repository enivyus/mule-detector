# Mule Account Detector

A mini core banking system and a detector for **mule accounts**: bank accounts that receive money
from scam victims and quickly pass it through more accounts before it is cashed out.

> Work in progress — portfolio project. All data is synthetic.

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

**Transaction simulator** (`simulator/`, Python)
- A month of normal activity for 2,000 customers: salary, bills, shopping, transfers between friends
- Hidden scam rings: victims → layer-1 mules → layer-2 mules → cash-out
- Ground-truth labels for every account, so detection can be measured

**Baseline rules** (`simulator/baseline_rules.py`)

| Version | Precision | Recall |
|---|---|---|
| Baseline rules (pass-through, fan-in, new-account burst) | 29% | 100% |

## Run it

```bash
docker compose up -d postgres
cd core-banking && ./mvnw spring-boot:run
```

API docs and a try-it-out UI: http://localhost:8080/swagger-ui.html (OpenAPI spec at `/v3/api-docs`).

```bash
# Open two accounts
curl -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"ownerName":"Alice"}'
curl -X POST localhost:8080/api/accounts -H "Content-Type: application/json" -d '{"ownerName":"Bob"}'

# Deposit 1,000.00 THB, then transfer 250.00 THB (amounts are in satang)
curl -X POST localhost:8080/api/accounts/<ALICE_NO>/deposit -H "Idempotency-Key: dep-1" \
     -H "Content-Type: application/json" -d '{"amountSatang":100000}'
curl -X POST localhost:8080/api/transfers -H "Idempotency-Key: tr-1" -H "Content-Type: application/json" \
     -d '{"fromAccountNo":"<ALICE_NO>","toAccountNo":"<BOB_NO>","amountSatang":25000}'
```

```bash
cd simulator
python simulate.py          # writes data/accounts.csv, data/transactions.csv
python baseline_rules.py    # precision / recall of the baseline rules
```

## Roadmap

- [x] Core banking API with ledger, locking, idempotency
- [x] Labeled transaction simulator + baseline rules
- [ ] Simulator posts to the API
- [ ] Streaming rule engine on Redpanda
- [ ] Graph analysis in Neo4j (community detection, money trail)
- [ ] Risk scoring, alerts, auto-freeze
- [ ] Investigator dashboard
