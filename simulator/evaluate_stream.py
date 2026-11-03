"""
Score the streaming rule engine against the simulator's ground-truth labels, and check that it
flags exactly the accounts the batch baseline (baseline_rules.py) flags for the same rules.

Run after replay.py has pushed the dataset through core-banking with the kafka profile on:

    python evaluate_stream.py --data ../data

Standard library only.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from urllib.request import urlopen

from baseline_rules import detect, load


def get(url: str):
    with urlopen(url, timeout=30) as r:
        return json.load(r)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    p.add_argument("--rules", default="http://localhost:8082")
    args = p.parse_args()

    accounts, txns = load(args.data)
    key = hashlib.sha256((args.data / "transactions.csv").read_bytes()).hexdigest()[:8]
    with open(args.data / f"account_map_{key}.csv", encoding="utf-8") as f:
        to_sim = {r["api_account_no"]: r["sim_account_no"] for r in csv.DictReader(f)}

    stats, alerts = get(f"{args.rules}/stats"), get(f"{args.rules}/alerts")
    print(f"events processed {stats['processedEvents']:,} | transactions in CSV {len(txns):,}")
    foreign = {a["accountNo"] for a in alerts if a["accountNo"] not in to_sim}
    if foreign:
        print(f"warning: {len(foreign)} flagged accounts are not in this dataset (old data in the topic?)")

    streamed: dict[str, set[str]] = defaultdict(set)
    for a in alerts:
        if a["accountNo"] in to_sim:
            streamed[to_sim[a["accountNo"]]].add(a["rule"])

    truth = {no for no, r in accounts.items() if r["is_mule"] == "1"}
    flagged = set(streamed)
    tp = len(truth & flagged)
    print(f"flagged {len(flagged)} accounts | true mules {len(truth)}")
    print(f"precision {tp / len(flagged) if flagged else 0:.2%} | recall {tp / len(truth) if truth else 0:.2%}")
    hits = defaultdict(lambda: [0, 0])
    for acc, rules in streamed.items():
        for r in rules:
            hits[r][0 if acc in truth else 1] += 1
    for r, (good, bad) in sorted(hits.items()):
        print(f"  {r:<18} true hits {good:>4}  false alarms {bad:>4}")

    # Parity with the batch baseline, rule by rule
    batch = detect(accounts, txns)
    ok = stats["processedEvents"] == len(txns)
    for rule in sorted({r for rs in batch.values() for r in rs} | set(hits)):
        b = {a for a, rs in batch.items() if rule in rs}
        s = {a for a, rs in streamed.items() if rule in rs}
        if b != s:
            ok = False
            print(f"  parity {rule}: batch only {sorted(b - s)[:5]} | stream only {sorted(s - b)[:5]}")
    print("parity with baseline_rules.py: identical" if ok else "parity with baseline_rules.py: DIFFERENT")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
