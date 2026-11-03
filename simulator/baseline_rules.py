"""
Baseline detector: a few simple rules run over the simulator's CSV output, scored against
the ground-truth labels. This is the number every later version (streaming rules, graph
analysis) has to beat.

    python baseline_rules.py --data ../data
"""
from __future__ import annotations

import argparse
import csv
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path

VAULT = "0000000000"
WINDOW = timedelta(hours=2)


def load(data: Path):
    with open(data / "accounts.csv", encoding="utf-8") as f:
        accounts = {r["account_no"]: r for r in csv.DictReader(f)}
    with open(data / "transactions.csv", encoding="utf-8") as f:
        txns = [dict(r, ts=datetime.fromisoformat(r["timestamp"]), amount=int(r["amount_satang"]))
                for r in csv.DictReader(f)]
    return accounts, txns


def detect(accounts: dict, txns: list) -> dict[str, list[str]]:
    incoming, outgoing = defaultdict(list), defaultdict(list)
    for t in txns:
        if t["from_account"] != VAULT:
            outgoing[t["from_account"]].append(t)
        if t["to_account"] != VAULT:
            incoming[t["to_account"]].append(t)

    reasons: dict[str, list[str]] = defaultdict(list)
    for acc_no, ins in incoming.items():
        role = accounts.get(acc_no, {}).get("role")
        if role in ("merchant", "employer"):
            continue
        outs = outgoing.get(acc_no, [])

        # Rule 1: pass-through: >= 90% of an incoming amount leaves within WINDOW
        for t_in in ins:
            if t_in["scenario"] in ("warmup",):
                continue
            out_sum = sum(o["amount"] for o in outs if t_in["ts"] <= o["ts"] <= t_in["ts"] + WINDOW)
            if t_in["amount"] >= 100_000 and out_sum >= 0.9 * t_in["amount"]:
                reasons[acc_no].append("pass_through")
                break

        # Rule 2: fan-in: money from >= 4 distinct senders within WINDOW
        senders = sorted(((t["ts"], t["from_account"]) for t in ins if t["from_account"] != VAULT))
        for i, (ts, _) in enumerate(senders):
            distinct = {s for t2, s in senders[i:] if t2 <= ts + WINDOW}
            if len(distinct) >= 4:
                reasons[acc_no].append("fan_in")
                break

        # Rule 3: new account burst: opened < 30 days before receiving >= 20,000 THB in a day
        # from other accounts (an own cash deposit is not a burst of transfers)
        opened = datetime.fromisoformat(accounts[acc_no]["opened_at"])
        by_day = defaultdict(int)
        for t in ins:
            if t["from_account"] != VAULT and t["ts"] - opened < timedelta(days=30):
                by_day[t["ts"].date()] += t["amount"]
        if by_day and max(by_day.values()) >= 2_000_000:
            reasons[acc_no].append("new_account_burst")

    return reasons


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    args = p.parse_args()

    accounts, txns = load(args.data)
    flagged = detect(accounts, txns)
    truth = {a for a, r in accounts.items() if r["is_mule"] == "1"}
    predicted = set(flagged)

    tp = len(truth & predicted)
    precision = tp / len(predicted) if predicted else 0.0
    recall = tp / len(truth) if truth else 0.0
    print(f"flagged {len(predicted)} accounts | true mules {len(truth)}")
    print(f"precision {precision:.2%} | recall {recall:.2%}")

    rule_hits = defaultdict(lambda: [0, 0])
    for acc, rs in flagged.items():
        for r in set(rs):
            rule_hits[r][0 if acc in truth else 1] += 1
    for r, (good, bad) in sorted(rule_hits.items()):
        print(f"  {r:<18} true hits {good:>4}  false alarms {bad:>4}")
    missed = truth - predicted
    if missed:
        print(f"missed: {', '.join(sorted(accounts[m]['role'] for m in missed))}")


if __name__ == "__main__":
    main()
