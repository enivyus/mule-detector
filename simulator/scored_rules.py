"""
Scored detector: the three baseline rules plus fan_out and odd_hours, combined into a score.
An account is a suspected mule when at least SCORE_THRESHOLD different rules fire: one rule alone
is a common coincidence for ordinary customers, two independent signals rarely are.

This is the batch reference for rule-engine's streaming implementation (evaluate_stream.py checks
they agree rule by rule).

    python scored_rules.py --data ../data
"""
from __future__ import annotations

import argparse
from collections import defaultdict
from datetime import timedelta
from pathlib import Path

from baseline_rules import VAULT, detect as detect_baseline, load

FAN_OUT_WINDOW = timedelta(hours=1)
FAN_OUT_RECIPIENTS = 2
NIGHT_END_HOUR = 5        # 00:00-04:59 Bangkok time (simulated timestamps are Bangkok local)
NIGHT_TRANSFERS = 2
SCORE_THRESHOLD = 2


def detect(accounts: dict, txns: list) -> dict[str, set[str]]:
    reasons = {acc: set(rs) for acc, rs in detect_baseline(accounts, txns).items()}
    business = {no for no, r in accounts.items() if r["role"] in ("merchant", "employer")}
    incoming, outgoing = defaultdict(list), defaultdict(list)
    for t in txns:
        if t["from_account"] != VAULT:
            outgoing[t["from_account"]].append(t)
        if t["to_account"] != VAULT:
            incoming[t["to_account"]].append(t)

    for acc, ins in incoming.items():
        if acc in business:
            continue
        outs = outgoing.get(acc, [])

        # fan_out: an incoming >= 1,000 THB is followed within FAN_OUT_WINDOW by transfers to
        # >= FAN_OUT_RECIPIENTS different personal accounts (shops, payroll and cash withdrawals don't count)
        for t_in in ins:
            if t_in["amount"] < 100_000:
                continue
            recipients = {o["to_account"] for o in outs
                          if t_in["ts"] <= o["ts"] <= t_in["ts"] + FAN_OUT_WINDOW
                          and o["to_account"] != VAULT and o["to_account"] not in business}
            if len(recipients) >= FAN_OUT_RECIPIENTS:
                reasons.setdefault(acc, set()).add("fan_out")
                break

        # odd_hours: >= NIGHT_TRANSFERS transfers in or out between 00:00 and 05:00
        if sum(1 for t in ins + outs if t["ts"].hour < NIGHT_END_HOUR) >= NIGHT_TRANSFERS:
            reasons.setdefault(acc, set()).add("odd_hours")
    return reasons


def suspects(reasons: dict[str, set[str]]) -> set[str]:
    return {acc for acc, rs in reasons.items() if len(rs) >= SCORE_THRESHOLD}


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--data", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    args = p.parse_args()

    accounts, txns = load(args.data)
    reasons = detect(accounts, txns)
    truth = {a for a, r in accounts.items() if r["is_mule"] == "1"}

    def score(predicted: set[str]) -> str:
        tp = len(truth & predicted)
        return (f"flagged {len(predicted):>4} | precision {tp / len(predicted) if predicted else 0:7.2%}"
                f" | recall {tp / len(truth) if truth else 0:7.2%}")

    print(f"true mules {len(truth)}")
    for rule in ("pass_through", "fan_in", "new_account_burst", "fan_out", "odd_hours"):
        print(f"  {rule:<18} alone: {score({a for a, rs in reasons.items() if rule in rs})}")
    print(f"  {'any rule':<18}       {score(set(reasons))}")
    print(f"  {'score >= ' + str(SCORE_THRESHOLD):<18}       {score(suspects(reasons))}")


if __name__ == "__main__":
    main()
