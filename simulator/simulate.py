"""
Transaction simulator for the mule account detector.

Generates a month of synthetic banking activity: ordinary customers (salary, bills,
shopping, transfers between friends) plus hidden scam rings in which victims pay
first-layer mule accounts that forward the money through more mule layers within
minutes before it is cashed out. Every account and transaction carries a ground-truth
label, so detection quality can be measured as precision / recall.

Standard library only. All money is in satang (1 THB = 100 satang).

    python simulate.py --customers 2000 --rings 6 --days 30 --seed 42 --out ../data
"""
from __future__ import annotations

import argparse
import csv
import random
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from pathlib import Path

VAULT = "0000000000"  # system cash vault: source of deposits, sink of withdrawals


@dataclass
class Account:
    account_no: str
    owner_name: str
    role: str                  # customer | merchant | employer | mule_l1 | mule_l2 | cashout
    opened_at: datetime
    is_mule: bool = False
    ring_id: str = ""


@dataclass
class Txn:
    timestamp: datetime
    from_account: str
    to_account: str
    amount_satang: int
    kind: str                  # TRANSFER | DEPOSIT | WITHDRAWAL
    scenario: str              # salary | bill | shopping | p2p | atm | warmup | scam_victim | scam_layer | scam_cashout
    ring_id: str = ""
    txn_id: str = field(default="")


class Simulator:
    def __init__(self, n_customers: int, n_rings: int, days: int, seed: int):
        self.rng = random.Random(seed)
        self.n_customers = n_customers
        self.n_rings = n_rings
        self.days = days
        self.start = datetime(2026, 10, 1)
        self.end = self.start + timedelta(days=days)
        self.accounts: dict[str, Account] = {}
        self.txns: list[Txn] = []
        self._used_nos: set[str] = {VAULT}

    # ---------- helpers ----------
    def _new_no(self) -> str:
        while True:
            no = f"{self.rng.randint(1_000_000_000, 9_999_999_999)}"
            if no not in self._used_nos:
                self._used_nos.add(no)
                return no

    def _open(self, role: str, name: str, opened_at: datetime, **kw) -> Account:
        acc = Account(self._new_no(), name, role, opened_at, **kw)
        self.accounts[acc.account_no] = acc
        return acc

    def _at(self, day: int, hour_lo: int = 7, hour_hi: int = 22) -> datetime:
        return self.start + timedelta(days=day, hours=self.rng.randint(hour_lo, hour_hi),
                                      minutes=self.rng.randint(0, 59), seconds=self.rng.randint(0, 59))

    def _baht(self, lo: float, hi: float) -> int:
        return int(round(self.rng.uniform(lo, hi), 2) * 100)

    def _add(self, *args, **kw) -> None:
        t = Txn(*args, **kw)
        if t.timestamp < self.end:
            self.txns.append(t)

    # ---------- population ----------
    def build_population(self) -> None:
        old = self.start - timedelta(days=365 * 3)
        self.employers = [self._open("employer", f"Company {i}", old) for i in range(max(5, self.n_customers // 100))]
        self.merchants = [self._open("merchant", f"Shop {i}", old) for i in range(max(10, self.n_customers // 40))]
        # ~10% of real customers are new too (students, first jobs), so "new account" alone is not proof
        self.customers = [
            self._open("customer", f"Customer {i}",
                       self.start - timedelta(days=self.rng.randint(1, 45) if self.rng.random() < 0.10
                                              else self.rng.randint(60, 3000)))
            for i in range(self.n_customers)
        ]
        # Friend groups for p2p transfers
        self.friends = {c.account_no: self.rng.sample(self.customers, k=min(6, len(self.customers))) for c in self.customers}
        # Savings deposited before the window opens, so a month of spending before payday does not overdraw
        self.savings = {}
        for c in self.customers:
            self.savings[c.account_no] = self._baht(40_000, 400_000)
            t = max(c.opened_at, self.start - timedelta(days=1)) + timedelta(minutes=self.rng.randint(1, 600))
            self._add(t, VAULT, c.account_no, self.savings[c.account_no], "DEPOSIT", "funding")

    # ---------- normal behaviour ----------
    def simulate_normal(self) -> None:
        for emp in self.employers:  # employers are funded by deposits
            self._add(self.start, VAULT, emp.account_no, 10_000_000_000, "DEPOSIT", "funding")

        for c in self.customers:
            employer = self.rng.choice(self.employers)
            salary = self._baht(15_000, 80_000)
            payday = self.rng.choice([25, 28, 30])
            for day in range(self.days):
                date = self.start + timedelta(days=day)
                if date.day == payday:
                    self._add(self._at(day, 8, 10), employer.account_no, c.account_no, salary, "TRANSFER", "salary")
                if self.rng.random() < 0.55:
                    self._add(self._at(day), c.account_no, self.rng.choice(self.merchants).account_no,
                              self._baht(40, 1_500), "TRANSFER", "shopping")
                if self.rng.random() < 0.08:
                    self._add(self._at(day), c.account_no, self.rng.choice(self.merchants).account_no,
                              self._baht(300, 4_000), "TRANSFER", "bill")
                if self.rng.random() < 0.12:
                    friend = self.rng.choice(self.friends[c.account_no])
                    if friend.account_no != c.account_no:
                        self._add(self._at(day, 9, 23), c.account_no, friend.account_no,
                                  self._baht(50, 3_000), "TRANSFER", "p2p")
                if self.rng.random() < 0.05:
                    self._add(self._at(day), c.account_no, VAULT, self._baht(500, 5_000), "WITHDRAWAL", "atm")

    # ---------- scam rings ----------
    def simulate_rings(self) -> None:
        for r in range(self.n_rings):
            ring = f"R{r + 1:02d}"
            attack_day = self.rng.randint(3, self.days - 3)
            attack_start = self.start + timedelta(days=attack_day)

            # Mule accounts: opened recently, often rented from real people
            n_l1, n_l2 = self.rng.randint(2, 4), self.rng.randint(3, 6)
            opened = lambda: attack_start - timedelta(days=self.rng.randint(2, 25))
            l1 = [self._open("mule_l1", f"Mule {ring}-A{i}", opened(), is_mule=True, ring_id=ring) for i in range(n_l1)]
            l2 = [self._open("mule_l2", f"Mule {ring}-B{i}", opened(), is_mule=True, ring_id=ring) for i in range(n_l2)]
            cashout = self._open("cashout", f"Mule {ring}-C", opened(), is_mule=True, ring_id=ring)

            # Warm-up: tiny transfers so the accounts don't look completely empty
            for m in l1 + l2:
                for _ in range(self.rng.randint(0, 2)):
                    t = m.opened_at + timedelta(hours=self.rng.randint(1, 40))
                    self._add(t, VAULT, m.account_no, self._baht(100, 500), "DEPOSIT", "warmup", ring)

            # Victims pay first-layer mules over a few hours
            victims = self.rng.sample(self.customers, k=self.rng.randint(5, 15))
            for i, v in enumerate(victims):
                mule = l1[i] if i < len(l1) else self.rng.choice(l1)  # every L1 mule takes at least one victim
                t_in = attack_start + timedelta(minutes=self.rng.randint(0, 6 * 60))
                if self.rng.random() < 0.35:  # some scams happen late at night
                    t_in = attack_start.replace(hour=self.rng.randint(0, 4)) + timedelta(minutes=self.rng.randint(0, 59))
                # A victim cannot lose more than they have; cap at half their savings (spending continues meanwhile)
                amount = min(self._baht(3_000, 60_000), self.savings[v.account_no] // 2)
                self._add(t_in, v.account_no, mule.account_no, amount, "TRANSFER", "scam_victim", ring)

                # L1 forwards 90-98% within minutes, split across several L2 mules
                remaining = int(amount * self.rng.uniform(0.90, 0.98))
                targets = self.rng.sample(l2, k=min(len(l2), self.rng.randint(2, 3)))
                t = t_in
                for i, tgt in enumerate(targets):
                    part = remaining if i == len(targets) - 1 else remaining // len(targets)
                    remaining -= part
                    t += timedelta(minutes=self.rng.randint(2, 20))
                    self._add(t, mule.account_no, tgt.account_no, part, "TRANSFER", "scam_layer", ring)

                    # L2 sends to the cash-out account, which withdraws
                    fwd = int(part * self.rng.uniform(0.92, 0.99))
                    t2 = t + timedelta(minutes=self.rng.randint(3, 40))
                    self._add(t2, tgt.account_no, cashout.account_no, fwd, "TRANSFER", "scam_layer", ring)
                    self._add(t2 + timedelta(minutes=self.rng.randint(5, 90)), cashout.account_no, VAULT,
                              int(fwd * 0.98), "WITHDRAWAL", "scam_cashout", ring)

    # ---------- output ----------
    def run(self) -> None:
        self.build_population()
        self.simulate_normal()
        self.simulate_rings()
        self.txns.sort(key=lambda t: t.timestamp)
        self._drop_overdrafts()
        for i, t in enumerate(self.txns, 1):
            t.txn_id = f"T{i:08d}"

    def _drop_overdrafts(self) -> None:
        """Replay balances in time order and drop what a real ledger would reject, so CSV and API agree."""
        balance: dict[str, int] = {}
        kept = []
        for t in self.txns:
            if t.from_account != VAULT and balance.get(t.from_account, 0) < t.amount_satang:
                continue
            balance[t.from_account] = balance.get(t.from_account, 0) - t.amount_satang
            balance[t.to_account] = balance.get(t.to_account, 0) + t.amount_satang
            kept.append(t)
        self.dropped = len(self.txns) - len(kept)
        self.txns = kept

    def write(self, out: Path) -> None:
        out.mkdir(parents=True, exist_ok=True)
        with open(out / "accounts.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["account_no", "owner_name", "role", "opened_at", "is_mule", "ring_id"])
            for a in self.accounts.values():
                w.writerow([a.account_no, a.owner_name, a.role, a.opened_at.isoformat(), int(a.is_mule), a.ring_id])
        with open(out / "transactions.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            w.writerow(["txn_id", "timestamp", "from_account", "to_account", "amount_satang", "kind", "scenario", "ring_id"])
            for t in self.txns:
                w.writerow([t.txn_id, t.timestamp.isoformat(), t.from_account, t.to_account,
                            t.amount_satang, t.kind, t.scenario, t.ring_id])

    def summary(self) -> str:
        mules = sum(a.is_mule for a in self.accounts.values())
        scam = sum(t.scenario.startswith("scam") for t in self.txns)
        return (f"accounts: {len(self.accounts):,} (mules: {mules}, rings: {self.n_rings})\n"
                f"transactions: {len(self.txns):,} (scam-related: {scam:,}, dropped as overdraft: {self.dropped:,})")


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--customers", type=int, default=2000)
    p.add_argument("--rings", type=int, default=6)
    p.add_argument("--days", type=int, default=30)
    p.add_argument("--seed", type=int, default=42)
    p.add_argument("--out", type=Path, default=Path(__file__).resolve().parent.parent / "data")
    args = p.parse_args()

    sim = Simulator(args.customers, args.rings, args.days, args.seed)
    sim.run()
    sim.write(args.out)
    print(sim.summary())
    print(f"written to {args.out}")


if __name__ == "__main__":
    main()
