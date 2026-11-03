---
version: 1
slug: "dashboard-src-app-jsx"
primary_target: "dashboard/src/App.jsx"
related_targets: ["dashboard/src"]
---

## Scope

The investigator dashboard (dashboard/), Operate mode. Login screen, review queue, account detail, freeze dialog.
Audience: interviewers watching a demo who must understand in seconds; in-story user is a bank fraud investigator.

## Direction contract

THESIS: The fintech category standard played straight, at the craft level of Stripe Dashboard and professional fraud
consoles (Sardine, Unit21). Refuses the incumbent's loose header-plus-cards page and any themed costume.

OWN-WORLD: Restrained. Cool canvas, white working surfaces, a second cooler neutral layer for the sidebar, navy-ink
text, one indigo accent for primary action, selection and focus only. Semantic state colours: red for freeze tier,
amber for review, green for active, slate with a lock icon for frozen. One sans family (Inter for Latin, Noto Sans
Thai for Thai), tabular numerals, 1px borders, 6px radius, soft offset shadows only on floating layers.

STORY: The viewer sees how many suspects exist and how many are dangerous, picks one, reads why it was flagged in
plain words, follows the money, and sees who froze it and why.

FIRST VIEWPORT: Left sidebar (240px): product mark and name, queue views (All, Freeze tier, Review, Frozen) with
counts, settings menu with the signed-in user at the bottom. Main: top bar with the current view's title, account
search and refresh; a single divided strip of five overview numbers; below it the queue table on the left and the
docked account detail on the right, where Freeze/Unfreeze is the primary action.

FORM: Canon, chosen by the user after three re-rolls (category standard). seed:7155a106bac1754e

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
