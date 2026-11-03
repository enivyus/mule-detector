"""
Replay the simulator's CSV output through the core banking API, so the bank's own database
(and later its event stream) holds the same month of activity with the original timestamps.

The API must run with the sim profile, which honors the X-Simulated-Time header:

    cd core-banking && ./mvnw spring-boot:run -Dspring-boot.run.profiles=sim
    cd simulator && python replay.py --data ../data

Safe to rerun: accounts already opened are read from account_map_<dataset hash>.csv (next to
the CSVs), and every transaction uses an idempotency key derived from the same hash and its
txn_id, so a retry never moves money twice. Standard library only.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import http.client
import json
import sys
from collections import defaultdict
from pathlib import Path
from urllib.parse import urlsplit

VAULT = "0000000000"
TZ = "+07:00"  # simulated times are Bangkok local time; night-time rules depend on it


class Api:
    def __init__(self, base: str):
        u = urlsplit(base)
        self.host, self.port = u.hostname, u.port or 80
        self.conn = http.client.HTTPConnection(self.host, self.port, timeout=30)

    def call(self, method: str, path: str, body: dict | None = None, headers: dict | None = None) -> tuple[int, dict]:
        data = json.dumps(body).encode() if body is not None else None
        h = {"Content-Type": "application/json", **(headers or {})}
        for attempt in (1, 2):  # one reconnect if the keep-alive connection was dropped
            try:
                self.conn.request(method, path, body=data, headers=h)
                resp = self.conn.getresponse()
                raw = resp.read()
                return resp.status, json.loads(raw) if raw else {}
            except (ConnectionError, http.client.HTTPException):
                self.conn.close()
                self.conn = http.client.HTTPConnection(self.host, self.port, timeout=30)
                if attempt == 2:
                    raise
        raise AssertionError("unreachable")


def fail(msg: str) -> None:
    print(f"error: {msg}", file=sys.stderr)
    sys.exit(1)


def open_accounts(api: Api, accounts: list[dict], map_path: Path) -> dict[str, str]:
    mapping = {VAULT: VAULT}
    if map_path.exists():
        with open(map_path, encoding="utf-8") as f:
            mapping.update({r["sim_account_no"]: r["api_account_no"] for r in csv.DictReader(f)})
        probe = next((no for no in mapping.values() if no != VAULT), None)
        if probe and api.call("GET", f"/api/accounts/{probe}")[0] == 404:
            fail(f"{map_path.name} points at accounts this database does not have (was it reset?). Delete it and rerun.")
    todo = [a for a in accounts if a["account_no"] not in mapping]
    new_file = not map_path.exists()
    with open(map_path, "a", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        if new_file:
            w.writerow(["sim_account_no", "api_account_no"])
        for i, a in enumerate(todo, 1):
            # Neutral owner name: the simulator's names ("Mule R01-A0") would leak labels into the bank
            # Banks know which accounts are businesses; mules are ordinary customer accounts
            kind = "BUSINESS" if a["role"] in ("merchant", "employer") else "CUSTOMER"
            status, res = api.call("POST", "/api/accounts",
                                   {"ownerName": f"Holder {a['account_no'][-6:]}", "type": kind},
                                   {"X-Simulated-Time": a["opened_at"] + TZ})
            if status != 201:
                fail(f"opening {a['account_no']}: HTTP {status} {res}")
            mapping[a["account_no"]] = res["accountNo"]
            w.writerow([a["account_no"], res["accountNo"]])
            f.flush()  # a crash mid-run must not lose which accounts already exist
    print(f"accounts: {len(todo):,} opened, {len(accounts) - len(todo):,} already mapped")
    return mapping


def replay_txns(api: Api, txns: list[dict], mapping: dict[str, str], key_prefix: str) -> None:
    for i, t in enumerate(txns, 1):  # file order: ties replay exactly as the simulator checked them
        src, dst, amount = mapping[t["from_account"]], mapping[t["to_account"]], int(t["amount_satang"])
        headers = {"Idempotency-Key": f"{key_prefix}-{t['txn_id']}", "X-Simulated-Time": t["timestamp"] + TZ}
        if t["kind"] == "DEPOSIT":
            status, res = api.call("POST", f"/api/accounts/{dst}/deposit", {"amountSatang": amount}, headers)
        elif t["kind"] == "WITHDRAWAL":
            status, res = api.call("POST", f"/api/accounts/{src}/withdraw", {"amountSatang": amount}, headers)
        else:
            status, res = api.call("POST", "/api/transfers",
                                   {"fromAccountNo": src, "toAccountNo": dst, "amountSatang": amount}, headers)
        if status != 200:
            fail(f"{t['txn_id']} ({t['scenario']}): HTTP {status} {res}")
        if i % 5000 == 0 or i == len(txns):
            print(f"transactions: {i:,} / {len(txns):,}")


def verify(api: Api, txns: list[dict], mapping: dict[str, str]) -> None:
    expected: dict[str, int] = defaultdict(int)
    for t in txns:
        expected[t["from_account"]] -= int(t["amount_satang"])
        expected[t["to_account"]] += int(t["amount_satang"])
    wrong = 0
    for sim_no, want in expected.items():
        if sim_no == VAULT:
            continue  # the vault is shared with anything else this database has seen
        status, res = api.call("GET", f"/api/accounts/{mapping[sim_no]}")
        if status != 200 or res["balanceSatang"] != want:
            wrong += 1
            print(f"  {sim_no} -> {mapping[sim_no]}: API {res.get('balanceSatang')} expected {want}")
    if wrong:
        fail(f"{wrong} account balances differ from the CSV")
    print(f"verified: {len(expected) - 1:,} account balances match the CSV")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    p.add_argument("--api", default="http://localhost:8080")
    args = p.parse_args()

    with open(args.data / "accounts.csv", encoding="utf-8") as f:
        accounts = list(csv.DictReader(f))
    tx_path = args.data / "transactions.csv"
    with open(tx_path, encoding="utf-8") as f:
        txns = list(csv.DictReader(f))
    # Keys are unique per dataset, so replaying a different dataset into the same database can't collide
    key_prefix = hashlib.sha256(tx_path.read_bytes()).hexdigest()[:8]

    api = Api(args.api)
    # One map per dataset: a regenerated CSV reuses many sim account numbers and must not inherit old accounts
    mapping = open_accounts(api, accounts, args.data / f"account_map_{key_prefix}.csv")
    replay_txns(api, txns, mapping, key_prefix)
    verify(api, txns, mapping)


if __name__ == "__main__":
    main()
