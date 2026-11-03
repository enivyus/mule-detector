"""
Graph step: suspicion follows the money. Starting from the suspects of scored_rules.py, a personal
account gets the extra signal from_suspect when it receives >= 1,000 THB from a suspect and at least
90% of *that amount* leaves again within 2 hours (withdrawals count). The score (>= 2 different
signals) is applied again, and this repeats until no new suspect appears; suspicion only grows, so it
always stops.

Looking at one account at a time cannot separate a rented, old mule account from a customer who
spends their salary fast; where the money came from can.

This is the batch reference for graph-service's Cypher implementation (evaluate_graph.py checks they agree).

    python graph_rules.py --data ../data
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import timedelta
from pathlib import Path

from baseline_rules import VAULT, load
from scored_rules import detect as detect_scored, suspects

FROM_SUSPECT = "from_suspect"
WINDOW = timedelta(hours=2)
MIN_SATANG = 100_000


def detect(accounts: dict, txns: list) -> dict[str, set[str]]:
    reasons = detect_scored(accounts, txns)
    business = {no for no, r in accounts.items() if r["role"] in ("merchant", "employer")}
    incoming, outgoing = defaultdict(list), defaultdict(list)
    for t in txns:
        if t["from_account"] != VAULT:
            outgoing[t["from_account"]].append(t)
        if t["to_account"] != VAULT:
            incoming[t["to_account"]].append(t)

    current = suspects(reasons)
    while True:
        for acc, ins in incoming.items():
            if acc in business or acc in current or FROM_SUSPECT in reasons.get(acc, ()):
                continue
            for t_in in ins:
                if t_in["from_account"] not in current or t_in["amount"] < MIN_SATANG:
                    continue
                forwarded = sum(o["amount"] for o in outgoing.get(acc, [])
                                if t_in["ts"] <= o["ts"] <= t_in["ts"] + WINDOW)
                if forwarded * 10 >= t_in["amount"] * 9:
                    reasons.setdefault(acc, set()).add(FROM_SUSPECT)
                    break
        grown = suspects(reasons)
        if grown == current:
            return reasons
        current = grown


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    args = p.parse_args()

    accounts, txns = load(args.data)
    truth = {a for a, r in accounts.items() if r["is_mule"] == "1"}
    before = suspects(detect_scored(accounts, txns))
    after = suspects(detect(accounts, txns))

    def score(label: str, predicted: set[str]) -> None:
        tp = len(truth & predicted)
        print(f"  {label:<26} flagged {len(predicted):>4} | precision {tp / len(predicted) if predicted else 0:7.2%}"
              f" | recall {tp / len(truth) if truth else 0:7.2%}")

    print(f"true mules {len(truth)}")
    score("score, account by account", before)
    score("score + from_suspect", after)
    added = after - before
    print(f"  added by the graph step: {len(added & truth)} mules, {len(added - truth)} genuine accounts")


if __name__ == "__main__":
    main()
