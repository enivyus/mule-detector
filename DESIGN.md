---
name: Mule Investigator
description: Investigator dashboard for a mule-account detector, drawn as a survey map sheet in map inks, light and night.
colors:
  canvas: "#fafaf7"
  surface: "#ffffff"
  sidebar: "#f1efe6"
  ink: "#1b1b1b"
  ink-deep: "#000000"
  ink-2: "#3d3c36"
  muted: "#5c5b52"
  line: "#dedacd"
  line-strong: "#b5b0a0"
  hover: "#f3f1e8"
  selected: "#efe9d8"
  accent-soft: "#e8e4d6"
  contour: "#9a5a24"
  contour-soft: "#f3e8da"
  danger: "#c0322a"
  danger-hover: "#a3271f"
  danger-soft: "#fbe8e5"
  ok: "#2f6b22"
  water: "#1f6fa8"
  course: "#8e2c8e"
  canvas-night: "#0f1215"
  surface-night: "#161a1f"
  sidebar-night: "#191e24"
  ink-night: "#ecebe3"
  ink-deep-night: "#ffffff"
  ink-2-night: "#c9c7bc"
  muted-night: "#9e9c90"
  line-night: "#262b31"
  line-strong-night: "#444b54"
  hover-night: "#1b2026"
  selected-night: "#1e242b"
  accent-soft-night: "#2b3138"
  contour-night: "#e0a46a"
  contour-soft-night: "#2e2418"
  danger-night: "#ff7a6b"
  danger-hover-night: "#ff978b"
  danger-soft-night: "#3a1b17"
  danger-ink-night: "#170b09"
  ok-night: "#8ccb72"
  water-night: "#6fb0e6"
  course-night: "#d37ad3"
typography:
  spot-height-lead:
    fontFamily: "Chakra Petch, Bai Jamjuree, system-ui, sans-serif"
    fontSize: "76px"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  spot-height:
    fontFamily: "Chakra Petch, Bai Jamjuree, system-ui, sans-serif"
    fontSize: "36px"
    fontWeight: 600
    lineHeight: 1
    fontFeature: "tnum"
  account-number:
    fontFamily: "Chakra Petch, Bai Jamjuree, system-ui, sans-serif"
    fontSize: "30px"
    fontWeight: 700
    lineHeight: 1.1
    letterSpacing: "0.03em"
    fontFeature: "tnum"
  headline:
    fontFamily: "Bai Jamjuree, Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.01em"
  title:
    fontFamily: "Bai Jamjuree, Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "20px"
    fontWeight: 700
  figure:
    fontFamily: "Chakra Petch, Bai Jamjuree, system-ui, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    letterSpacing: "0.03em"
    fontFeature: "tnum"
  subtitle:
    fontFamily: "Bai Jamjuree, Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "16px"
    fontWeight: 700
  body:
    fontFamily: "Bai Jamjuree, Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "Bai Jamjuree, Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: "20px"
  table-head:
    fontFamily: "Chakra Petch, Bai Jamjuree, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 600
rounded:
  none: "0px"
spacing:
  row: "8px"
  rule-gap: "12px"
  phone-gutter: "16px"
  stack: "18px"
  sheet: "22px"
  gutter: "24px"
  page: "32px"
  tick: "48px"
components:
  button-primary:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.canvas}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "5px 12px"
  button-primary-hover:
    backgroundColor: "{colors.ink-deep}"
  button-danger:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.surface}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "5px 12px"
  button-danger-hover:
    backgroundColor: "{colors.danger-hover}"
  button-secondary:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "5px 12px"
  button-secondary-hover:
    backgroundColor: "{colors.hover}"
  button-ghost:
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    rounded: "{rounded.none}"
    padding: "5px 12px"
  input-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.none}"
    padding: "9px 10px"
  nav-view:
    textColor: "{colors.ink-2}"
    typography: "{typography.body}"
    padding: "8px 6px"
  nav-view-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.canvas}"
  sheet-panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.none}"
    padding: "{spacing.sheet}"
  table-head:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.muted}"
    typography: "{typography.table-head}"
    padding: "8px 10px"
---

# Design System: Mule Investigator

## Overview

**Creative North Star: "The Survey Sheet"**

The dashboard is a printed survey map. Risk is elevation, the money trail is a route, and each queue view is one sheet of a numbered series. Every surface is map paper ruled in ink: heavy 2px frames close a sheet, 1px rules divide it, corners stay square, and a strip of coordinate ticks runs along the top edge of the working sheet. Colour is never decoration. Each hue is a map ink with one job, so a viewer who has read the map key can read every row.

Density follows a reference map rather than a marketing page: a legend panel on the left, a title block with sheet number and date, a strip of spot heights where the freeze-tier total stands far above the rest, then the queue beside a framed account sheet. Before any account is opened that place holds the map key, which explains every symbol on the page. Following the money opens a separate full-width sheet below, where the route is drawn in course purple over faint contour lines.

The world rejects the generic SaaS arrangement it replaced: tinted canvas, white rounded cards and one indigo accent. Night mode is the same sheet printed for the dark: the same inks lifted for contrast, never a different palette.

**Key Characteristics:**
- Map paper with ink frames (2px) and ink rules (1px); square corners on every box.
- Six inks with fixed meanings: ink, contour brown, road red, forest green, water blue, course purple.
- Map-legend symbols (circle, triangle, square) always paired with words; colour never carries a state alone.
- Bai Jamjuree sets the words; Chakra Petch sets figures, account numbers, sheet data and table heads.
- Risk drawn as contour rings, one per real-time rule fired.
- Light and night follow `prefers-color-scheme`; the graph canvas follows the scheme too.

## Colors

Survey inks on map paper: near-black ink does the structural work, and five coloured inks each mark one kind of fact.

### Primary
- **Survey Ink** (ink; night: ink-night): frames, rules, all text, the coordinate ticks and crop marks, the active view in the legend (reversed), and the primary button. Its hover deepens to pure black, or pure white at night (ink-deep). The frozen state is also drawn in ink: a closed point on the map.

### Secondary
- **Road Red** (danger; night: danger-night): the freeze tier. It leads the spot heights, fills the freeze-tier triangle and the Freeze button, marks FREEZE in the audit list and fills suspect nodes on the trail. It also carries errors: the error banner and a refused-request toast. Hover darkens (danger-hover); its tint (danger-soft) sits only behind the error banner. Text on red is white by day and near-black at night (danger-ink-night).
- **Contour Brown** (contour; night: contour-night): risk as height. The contour rings and their figure in the queue, the summit glyph on the account sheet, and the review tier (outlined triangle, spot-height value). The review tier has no colour of its own; it is contour brown by definition.

### Tertiary
- **Forest Green** (ok; night: ok-night): the active state and UNFREEZE in the audit list.
- **Water Blue** (water; night: water-night): money coming in (the IN direction and amount in the statement), the focus ring, and in-text account links.
- **Course Purple** (course; night: course-night): the money graph. Route lines, arrowheads and ordinary account nodes on the trail, the route entry in the map key and trail legend, and the one signal that comes from the graph (from_suspect).

### Neutral
- **Map Paper** (canvas): the page and the table-head strip; also the text colour on ink.
- **Sheet White** (surface): framed sheets, fields, secondary buttons, the graph canvas.
- **Legend Buff** (sidebar): the legend panel.
- **Second Ink** (ink-2): secondary text, ghost buttons, stat labels.
- **Faded Ink** (muted): captions, table heads, timestamps, the cash vault on the trail.
- **Grid Line** (line): row dividers inside a sheet; skeleton fill; the pressed button.
- **Strong Grid** (line-strong): dividers between spot heights and between facts; scrollbars; the dotted signal underline.
- **Hover Buff / Selected Buff** (hover, selected): row and control hover; the open account's row.
- **Contour Tint** (contour-soft): the faint contour lines behind the trail graph and the statement row whose transfer is being followed.
- **Selection Tint** (accent-soft): the text-selection colour.

### Named Rules
**The One Ink, One Meaning Rule.** Each coloured ink marks one kind of fact: red the freeze tier and refusals, brown risk height and review, green active, blue money in and focus, purple the money graph. A new surface uses the ink of the fact it shows, or ink.

**The Course Is the Graph Rule.** Course purple appears only where the money graph is shown or cited: the route, its nodes and legend, and the graph-derived signal. It never marks a button, a state or a heading.

**The Symbol Beside Colour Rule.** Every tier and status is a map symbol plus a word. Colour alone never carries meaning.

## Typography

**Body Font:** Bai Jamjuree (with Noto Sans Thai, system-ui, Segoe UI, Leelawadee UI, sans-serif), weights 400 to 700.
**Label/Mono Font:** Chakra Petch (with Bai Jamjuree, system-ui, sans-serif), loaded at 500, 600 and 700 only.

**Character:** Bai Jamjuree is a Thai-and-Latin face with squared terminals, so Thai labels and English share one voice. Chakra Petch is its engineered, chamfered partner, used the way a map uses a separate face for heights and grid references.

### Hierarchy
- **Spot height, lead** (Chakra Petch 600, 76px, line-height 1, tabular): the freeze-tier total only, in road red. 60px at 1180px and below.
- **Spot height** (Chakra Petch 600, 36px, line-height 1, tabular): the other four overview totals. 30px at 1180px and below.
- **Account number** (Chakra Petch 700, 30px, line-height 1.1, 0.03em): the heading of the account sheet.
- **Headline** (Bai Jamjuree 700, 28px, line-height 1.2, -0.01em): the view title in the title block; the login title.
- **Title** (Bai Jamjuree 700, 20px): sheet heads (map key, Follow the money, dialog).
- **Figure** (Chakra Petch 600, 20px, tabular): the account facts row (balance, rules fired, auto-freeze threshold).
- **Subtitle** (Bai Jamjuree 700, 16px): block heads inside the account sheet.
- **Queue figures** (Chakra Petch 15px; 600 for account numbers, 700 for the contour figure).
- **Body** (Bai Jamjuree 400, 14px, line-height 1.55; Thai text 1.65): running text, rows, signal names. Hints cap at 70ch.
- **Label** (Bai Jamjuree 600, 13px, 20px line): buttons, tier and status labels, field labels.
- **Table head** (Chakra Petch 600, 12px, muted): queue column heads and the legend's view-group head, each on a 1px ink rule. Sentence case, no tracking.
- **Sheet data** (11px Bai Jamjuree caption over a 14px Chakra Petch 600 value): the title-block cells (sheet number, date).

### Named Rules
**The Figures in Chakra Rule.** Anything a reader compares or copies (counts, amounts, account numbers, times, sheet numbers) is set in Chakra Petch with tabular numerals; account numbers and amounts add 0.03em spacing. Words stay in Bai Jamjuree.

**The 4-3-3 Rule.** Account numbers always display grouped four, three and three digits with spaces (0000 000 000): in the queue, the sheet head, the trail labels and links. Search ignores the spaces.

## Layout

A two-column shell: a 248px legend panel, sticky at full height and closed by a 2px ink rule on its right, beside a fluid working sheet with 32px side gutters. The working sheet opens with a 10px band of coordinate ticks (1px ink every 48px, aligned to the gutter) closed by a 1px ink rule, then the title block (view title and subtitle, sheet-number and date cells, search, refresh) closed by a 2px ink rule. Vertical rhythm between bands is 18px.

The spot-height strip is a five-column grid (1.6fr for the lead, then four equal columns), every value on a shared bottom line, every label reserving two lines so a wrapping label cannot lift its value. The work area below is the queue (1.2fr, sticky, on the paper under a 2px top rule) beside a framed sheet (min 440px) with a 24px gap. The "Follow the money" trail gets its own full-width framed sheet under both.

Inside sheets, padding is 22px; rows run 8px vertically; dividers between paired facts or stats are 1px strong grid lines, outer bounds 1px ink.

**Responsive.**
- At 1180px and below the account sheet drops under the queue (one `minmax(0, 1fr)` column, so a wide table cannot widen the page), the queue stops being sticky (max 540px tall), the lead column narrows to 1.4fr and the spot heights step down (60px / 30px).
- At 860px and below the legend panel becomes a top strip (views scroll in one line, user name hidden, settings menu opens downward), gutters drop to 16px, spot heights pair up two per row with the lead spanning, and only one thing shows at a time: the queue, or the open account with a back button. Tier and status fold into a line under each account number. On the trail the route runs top to bottom, one step per row, with node labels to the right.

### Named Rules
**The Lead Height Rule.** In the overview the freeze-tier total is the single largest figure on the page (76px against 36px). No other number competes with it.

**The Key First Rule.** When no account is open, the account sheet's place shows the map key. It is never left empty or filled with a generic empty state.

## Elevation & Depth

Sheets are flat. Depth comes from ink weight: a 2px frame means a sheet, a 1px ink rule a section, a grid line a row. The open queue row is marked by a buff fill between two 1px ink rules, not by a lift. A soft float shadow appears only on layers that sit above the sheet: the settings menu, the dialog, the toast and the login card.

### Shadow Vocabulary
- **Float** (`box-shadow: 0 10px 30px rgba(27,27,27,0.14), 0 2px 6px rgba(27,27,27,0.08)`; night `0 10px 30px rgba(0,0,0,0.55), 0 2px 6px rgba(0,0,0,0.4)`): menus, dialogs, toast, login card.

### Named Rules
**The Flat Sheet Rule.** Nothing on the sheet casts a shadow. Only a layer that floats above the sheet does, and it still carries its 2px ink frame (the toast is solid ink instead).

## Shapes

Square corners (0px radius) on every box: frames, buttons, fields, menus, dialogs, the brand mark and the avatar. Curves belong to the map's own symbols: open circles, contour rings, spot-height dots, round trail nodes and curved route lines.

Recurring geometry is drawn in ink with CSS gradients: coordinate ticks along the top edge of the working sheet, 14px crop marks at the four corners of the login card (10px outside its frame), and faint concentric contour lines behind the trail graph.

**Map-legend symbols** (16-unit grid, each in the ink of its fact): open circle (all views; active), filled triangle (freeze tier), outlined triangle with mitred joins (review), filled square (frozen). A small filled spot-height dot sits beside each overview label.

**Risk contour glyph:** one ring per real-time rule fired (up to five), the innermost ring heavier, a summit dot from three rules up, always followed by its figure in Chakra Petch.

**Icons:** one hand-drawn set on a 24px grid, 2px stroke, square caps, mitred corners, rectangles in place of rounded shapes (a square lens for search, a graticule for language, a north arrow over a contour for the brand mark).

## Components

### Buttons
Square and ink-ruled; the action reads from its ink.
- **Shape:** square corners (0px), 1px border in the button's ink.
- **Primary:** solid ink with paper text (5px 12px, 13px Bai Jamjuree 600): sign-in, unfreeze, and the confirm of an unfreeze.
- **Danger:** solid road red: the Freeze action and its confirm.
- **Secondary:** sheet white with a 1px ink border; hover to hover buff, pressed to grid line.
- **Ghost:** no border, second ink; hover fills hover buff. Back, follow, fit, language.
- **Icon:** transparent 6px square around an icon. **Link:** water blue, underlined 3px below.
- **Sizes:** small (3px 9px, 12px), wide (full width, 9px 12px).
- **Hover / Focus:** colour transitions 150ms ease-out; 2px water-blue focus outline offset 2px; disabled at 50% opacity.

### Signals (legend type)
Signals are set like legend entries, not chips: the signal name in 14px Bai Jamjuree 600 ink with a dotted strong-grid underline 4px below (a tooltip explains it); extra signals fold into a "+N" figure in Chakra Petch 700, muted. The graph-derived signal is set in course purple.

### Cards / Containers (sheets)
- **Corner Style:** square (0px).
- **Background:** sheet white on map paper.
- **Shadow Strategy:** none (see Elevation & Depth).
- **Border:** 2px ink frame.
- **Internal Padding:** 22px (16px for the account sheet on phones).
- The queue is not a sheet: it sits on the paper under a 2px ink top rule.

### Inputs / Fields
- **Style:** 1px ink border, sheet white, square, 9px 10px padding, 15px text; label above in 13px 600 second ink.
- **Focus:** 2px water-blue outline, 1px offset (the search box shows it around the whole box).

### Navigation (legend panel)
- A Chakra Petch 12px muted head on a 1px ink rule, then the four views as rows: symbol, name (14px 500), count (Chakra Petch 14px 600).
- Hover fills hover buff; the active view reverses to solid ink with paper text and a paper symbol.
- The synthetic-data note, then the user and settings at the foot above a 1px ink rule. On phones the panel becomes a top strip with the views scrolling in one line.

### Queue table
Table heads in Chakra Petch on paper above a 1px ink rule, sticky. Rows 8px 10px with grid-line dividers and a hover-buff hover (120ms); the open account in selected buff between two 1px ink rules. Columns: account number (4-3-3), contour glyph with its figure and tier triangle, signals, status, freeze action.

### Spot-height strip
Five totals, value above label on a shared baseline, divided by 1px strong grid lines and closed by a 1px ink rule. Every label carries a spot-height dot. The lead (freeze tier) is road red throughout; the review value is contour brown.

### Map key
A framed sheet with a 20px title, a hint, and a ruled list: a 44px symbol column (contour glyph, freeze triangle, review triangle, frozen square, active circle, purple route arrow) beside each meaning.

### Follow the money
A full-width framed sheet: the title with the 4-3-3 account number, a ruled graph box with a legend bar (suspect, account, frozen, cash, route) and a fit button, the Cytoscape canvas on sheet white over faint contour rings, and a cue bar when the trail runs past the edge. Route width scales with the square root of the amount. Suspects fill road red, frozen accounts become squares, the cash vault is a muted block. Cytoscape draws on a canvas and cannot read CSS variables, so it reads the ink, surface, course, danger, muted and font tokens from the document at mount and redraws on a colour-scheme change or when crossing 860px.

### Login cover
A 400px framed sheet with the float shadow and ink crop marks at its corners: the brand mark (a north arrow over a contour in a solid ink square) and a language ghost button on top, a 28px title, fields, and a wide primary button.

## Do's and Don'ts

### Do:
- **Do** frame every sheet with a 2px ink border and divide it with 1px ink rules and grid lines.
- **Do** keep every box square (0px radius); let only map symbols be round.
- **Do** pair each tier and status with its map symbol and a word: open circle, filled triangle, outlined triangle, filled square.
- **Do** set every figure, account number, time and table head in Chakra Petch with tabular numerals.
- **Do** display account numbers as 4-3-3.
- **Do** keep the freeze-tier total the largest figure on the page, in road red.
- **Do** draw risk as contour rings in contour brown followed by the rule count.
- **Do** read colours from the CSS tokens for any canvas drawing, and redraw on a colour-scheme change.
- **Do** keep night mode as the same inks lifted, defined under `prefers-color-scheme: dark`.

### Don't:
- **Don't** round the corners of boxes, buttons, fields or menus.
- **Don't** box signals as pill or tag chips; set them as legend text.
- **Don't** use course purple outside the money graph.
- **Don't** give an ink a meaning another ink already carries, or show a state by colour alone.
- **Don't** put shadows on sheets; only floating layers get the float shadow.
- **Don't** use filled, rounded or soft-cornered icon sets; icons are 2px, square-capped and mitred on a 24px grid.
- **Don't** leave the account area empty when nothing is selected; show the map key.
