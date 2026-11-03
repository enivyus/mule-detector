---
version: 1
slug: "dashboard-src-app-jsx"
primary_target: "dashboard/src/App.jsx"
related_targets: ["dashboard/src"]
---

## Scope

The investigator dashboard (dashboard/), Operate mode. Login screen, review queue, account detail, freeze dialog.
Audience: interviewers watching a demo who must understand in seconds; in-story user is a bank fraud investigator.
Replaces the earlier fintech-standard world, which the owner rejected as generic.

## Direction contract

THESIS: The dashboard is a survey map sheet: risk is elevation, the money trail is a route, each queue view is a
sheet. Refuses the generic SaaS arrangement of tinted canvas, white cards and one indigo accent.

OWN-WORLD: Survey map inks on map paper: ink black for frames, type and primary action; contour brown for risk;
road red for the freeze tier; forest green for active; water blue for money in and focus; course purple reserved
for the money trail only. Heavy 2px ink sheet frames, 1px rules, square corners, coordinate ticks on the sheet edge.
Bai Jamjuree for text, Chakra Petch for numbers, account numbers and legend labels. Map-symbol icons: circle,
triangle, square, drawn square-capped.

STORY: The viewer reads the freeze-tier total first, scans a queue where each row's contour glyph shows how high its
risk climbs, opens one account, reads its signals, follows the purple route of its money, and sees who froze it.

FIRST VIEWPORT: Left legend sidebar (248px): name, the four views as map symbols with counts, synthetic-data note,
user and settings at the foot. Main sheet with coordinate ticks along the top edge, a title block (sheet number,
date, view title), search and refresh. A spot-height strip where the freeze-tier total leads at the largest size.
Below it the queue (contour glyph, tier symbol, status symbol) beside the account sheet, framed in 2px ink, whose
primary action is a solid ink or road-red button and whose trail draws in course purple over faint contours.

FORM: Grounded candidate 7 of 7, assigned by the roll (survey topographic map). seed:55f635f70862efae

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
