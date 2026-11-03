"""
Score graph-service against the simulator's labels:
  1. suspects after suspicion follows the money, with parity against graph_rules.py;
  2. Louvain communities: does each ring end up in one community, and how many genuine accounts share it;
  3. one money trail from a scam victim's payment, with the true role of every hop.

Run with core-banking replayed (kafka profile), rule-engine and graph-service up:

    python evaluate_graph.py --data ../data

Standard library only.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from urllib.request import Request, urlopen

from baseline_rules import load
from credentials import basic_auth
from graph_rules import FROM_SUSPECT, detect
from scored_rules import suspects


def call(url: str, method: str = "GET"):
    with urlopen(Request(url, method=method, headers=basic_auth("investigator")), timeout=120) as r:
        return json.load(r)


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    p.add_argument("--graph", default="http://127.0.0.1:8083")  # IPv4, like the service (see replay.py)
    args = p.parse_args()

    accounts, txns = load(args.data)
    key = hashlib.sha256((args.data / "transactions.csv").read_bytes()).hexdigest()[:8]
    with open(args.data / f"account_map_{key}.csv", encoding="utf-8") as f:
        to_sim = {r["api_account_no"]: r["sim_account_no"] for r in csv.DictReader(f)}
    truth = {no for no, r in accounts.items() if r["is_mule"] == "1"}

    # 1. Suspects
    graph = {to_sim[no]: set(rs) for no, rs in call(f"{args.graph}/suspects").items() if no in to_sim}
    tp = len(truth & set(graph))
    print(f"true mules {len(truth)}")
    print(f"  graph suspects       flagged {len(graph):>4} | precision {tp / len(graph) if graph else 0:7.2%}"
          f" | recall {tp / len(truth):7.2%}")
    added = {a for a, rs in graph.items() if FROM_SUSPECT in rs}
    print(f"  {FROM_SUSPECT} added {len(added & truth)} mules and {len(added - truth)} genuine accounts")

    batch = detect(accounts, txns)
    ok = suspects(batch) == set(graph) and all(graph[a] == batch[a] for a in graph)
    if not ok:
        b = suspects(batch)
        print(f"  parity: batch only {sorted(b - set(graph))[:5]} | graph only {sorted(set(graph) - b)[:5]}")
    print("parity with graph_rules.py:", "identical" if ok else "DIFFERENT")

    # 2. Communities
    community = {to_sim[no]: c for no, c in call(f"{args.graph}/communities", "POST").items() if no in to_sim}
    sizes = Counter(community.values())
    print(f"\nLouvain: {len(community)} personal accounts in {len(sizes)} communities")
    rings = defaultdict(list)
    for no, r in accounts.items():
        if r["ring_id"]:
            rings[r["ring_id"]].append(no)
    for ring, members in sorted(rings.items()):
        cs = Counter(community.get(m) for m in members)
        main_c, n = cs.most_common(1)[0]
        genuine = sum(1 for a, c in community.items() if c == main_c and a not in truth)
        print(f"  {ring}: {n}/{len(members)} mules in one community of {sizes.get(main_c, 0)} accounts"
              f" ({genuine} genuine)")

    # 3. One money trail (transfer ids follow CSV order, since replay posts them in order into a fresh database)
    victim = next(i for i, t in enumerate(txns, 1) if t["scenario"] == "scam_victim")
    print(f"\nmoney trail from transfer {victim} (a scam victim's payment):")
    for h in call(f"{args.graph}/trail/{victim}")[:15]:
        role = accounts.get(to_sim.get(h["toAccountNo"], ""), {}).get("role", h["toAccountType"].lower())
        print(f"  {'  ' * h['depth']}-> {role:<9} {h['amountSatang'] / 100:>12,.2f} THB at {h['at']}")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
