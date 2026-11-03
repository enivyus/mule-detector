"""
How much stolen money auto-freeze would have stopped, measured from the rule engine's own alerts.

An account is frozen at the moment of the alert that gives it its third rule (RuleEngine.FREEZE_THRESHOLD).
"Stopped" counts only the account's own cash-out withdrawals after that moment, so it is a lower bound:
freezing an upstream mule also starves everyone downstream of it, but money is fungible and crediting
that would be a guess.

Uses the replayed data already in the topic; nothing is frozen for real. Standard library only.

    python evaluate_freeze.py --data ../data
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from urllib.request import Request, urlopen

from baseline_rules import load
from credentials import basic_auth

FREEZE_THRESHOLD = 3
BANGKOK = timezone(timedelta(hours=7))  # no daylight saving; simulated times are naive Bangkok local


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    p.add_argument("--rules", default="http://127.0.0.1:8082")  # IPv4, like the service (see replay.py)
    args = p.parse_args()

    accounts, txns = load(args.data)
    key = hashlib.sha256((args.data / "transactions.csv").read_bytes()).hexdigest()[:8]
    with open(args.data / f"account_map_{key}.csv", encoding="utf-8") as f:
        to_sim = {r["api_account_no"]: r["sim_account_no"] for r in csv.DictReader(f)}
    with urlopen(Request(f"{args.rules}/alerts", headers=basic_auth("investigator")), timeout=30) as r:
        alerts = json.load(r)

    # Freeze time: the alert that brought the account to FREEZE_THRESHOLD different rules
    by_account = defaultdict(list)
    for a in alerts:
        if a["accountNo"] in to_sim:
            at = datetime.fromisoformat(a["at"]).astimezone(BANGKOK).replace(tzinfo=None)
            by_account[to_sim[a["accountNo"]]].append(at)
    frozen = {acc: sorted(ts)[FREEZE_THRESHOLD - 1] for acc, ts in by_account.items() if len(ts) >= FREEZE_THRESHOLD}

    truth = {no for no, r in accounts.items() if r["is_mule"] == "1"}
    stolen = sum(t["amount"] for t in txns if t["scenario"] == "scam_victim")
    cashouts = [t for t in txns if t["scenario"] == "scam_cashout"]
    cashed = sum(t["amount"] for t in cashouts)
    stopped = sum(t["amount"] for t in cashouts
                  if t["from_account"] in frozen and t["ts"] > frozen[t["from_account"]])

    print(f"frozen {len(frozen)} accounts: {len(set(frozen) & truth)} mules, {len(set(frozen) - truth)} genuine")
    print(f"stolen from victims {stolen / 100:>14,.2f} THB")
    print(f"cashed out by rings {cashed / 100:>14,.2f} THB")
    print(f"stopped (lower bound) {stopped / 100:>12,.2f} THB = {stopped / cashed:.0%} of what was cashed out")

    first_cashout = {}
    for t in cashouts:
        first_cashout.setdefault(t["ring_id"], t["ts"])
    rings = sorted(first_cashout)
    early = [ring for ring in rings
             if any(accounts[a]["ring_id"] == ring and ts < first_cashout[ring] for a, ts in frozen.items())]
    print(f"rings with an account frozen before their first cash-out: {len(early)} of {len(rings)}")
    for ring in rings:
        members = [a for a in frozen if accounts[a]["ring_id"] == ring]
        first = min((frozen[a] for a in members), default=None)
        minutes = (first_cashout[ring] - first).total_seconds() / 60 if first else 0
        lead = ("no account frozen" if first is None else
                f"{minutes:.0f} min before first cash-out" if minutes > 0 else f"{-minutes:.0f} min after first cash-out")
        print(f"  {ring}: {len(members)} frozen, first freeze {lead}")


if __name__ == "__main__":
    main()
