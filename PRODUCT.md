# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary audience: interviewers at banks, fintech and tech companies watching the owner's demo video or screen share
of this portfolio project. They see the dashboard for minutes, often for the first time, and must understand within
seconds what it does and why it is credible.

In-story user: a bank fraud investigator who works a queue of suspected mule accounts, decides which to freeze or
release, and follows stolen money across accounts. The interface must stay believable as that person's working tool.

## Product Purpose

A mini bank plus a system that detects mule accounts (บัญชีม้า): accounts that receive money from scam victims and
pass it on within minutes. The dashboard is the human end of the pipeline: it shows what the real-time rules and the
graph analysis flagged, why, where the money went, and lets an investigator freeze or unfreeze with a recorded reason.
Success: a viewer understands the problem, trusts the numbers, and remembers the project.

## Positioning

The detection is measured, not claimed: every rule is checked against labeled synthetic scam rings, and numbers are
reported on a seed never used for tuning (precision/recall in README). Two risk tiers: automatic freeze only when 3+
real-time rules fire (no genuine account frozen in tests), everything else goes to a person.

## Operating Context

- Runs locally: core-banking (:8080), rule-engine (:8082), graph-service (:8083), dashboard via Vite (:5173).
- Investigators sign in (HTTP Basic, INVESTIGATOR role); credentials live in memory only.
- Shown in a recorded demo video and live screen shares; laptop screens, sometimes narrow windows.

## Capabilities and Constraints

- Overview counts, a review queue of suspects with tabs (all, freeze tier, review, frozen) and search.
- Account view: balance, status, signals with plain-language explanations, freeze history (audit log), timeline of
  money in and out, "follow the money" trail graph (Cytoscape.js, left to right in time order), Louvain community.
- Freeze / unfreeze with a required reason; the rule engine also freezes automatically.
- Thai and English, switchable; Thai dates show the Buddhist year.
- Never shows ground-truth labels from the simulator.
- Money is in satang from the API, shown as THB with two decimals.

## Brand Commitments

None binding beyond the product name, which may be refined. Must support both light and dark mode, following the
system setting.

The owner first chose the fintech category standard, then rejected the built result as generic "AI slop": it looked
like any SaaS dashboard, its icons and fonts had no character, nothing was memorable, and everything carried the same
weight. The dashboard must now have a distinct identity a viewer remembers, while still reading as a working tool a
bank would use, with a clear first place to look.

## Evidence on Hand

- Measured results in README.md (unseen seed 9999: score + graph 87% precision / 100% recall; live demo froze 23
  accounts, all mules, 89% of cash-outs refused).
- All data is synthetic. Never show real names, real account numbers, real bank names or logos.

## Product Principles

- Explain every flag in plain words; a suspicion without a reason is not shown.
- Freezing a genuine customer is the costly mistake: irreversible-feeling actions confirm and record why.
- Show the money's path, not just a score.
- Honest numbers over impressive ones.

## Accessibility & Inclusion

Thai and English text must both render well (Thai needs taller line height). Keyboard use for the queue and dialogs.
