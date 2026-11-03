---
name: Mule Investigator
description: Investigator console for a mule-account detector; the fintech category standard, light and dark.
colors:
  canvas: "#f5f7fa"
  surface: "#ffffff"
  sidebar: "#eef1f5"
  ink: "#0e1b2b"
  ink-2: "#33414f"
  muted: "#586676"
  line: "#e1e6ec"
  line-strong: "#c3ccd6"
  hover: "#f1f4f8"
  selected: "#eeeefe"
  accent: "#4844d4"
  accent-hover: "#3c38bd"
  accent-ink: "#ffffff"
  accent-soft: "#e9e8fd"
  danger: "#c42b3c"
  danger-hover: "#a82232"
  danger-soft: "#fdecee"
  danger-ink: "#ffffff"
  warn: "#8a5100"
  warn-dot: "#d98a0b"
  warn-soft: "#fff3d9"
  ok: "#1c7547"
  ok-soft: "#e4f4eb"
  frozen: "#2f3d4d"
  frozen-soft: "#e6eaf0"
  person: "#4a6fd4"
  overlay: "rgba(14, 27, 43, 0.44)"
  canvas-dark: "#0b0f14"
  surface-dark: "#121820"
  sidebar-dark: "#0f141a"
  ink-dark: "#e5ebf1"
  ink-2-dark: "#c3cdd7"
  muted-dark: "#93a0ae"
  line-dark: "#222b36"
  line-strong-dark: "#3b4858"
  hover-dark: "#18202a"
  selected-dark: "#1d1f3d"
  accent-dark: "#8f8dff"
  accent-hover-dark: "#a5a3ff"
  accent-ink-dark: "#0b0f14"
  accent-soft-dark: "#23244a"
  danger-dark: "#ff6b7b"
  danger-hover-dark: "#ff8693"
  danger-soft-dark: "#3a1a20"
  danger-ink-dark: "#1a0a0d"
  warn-dark: "#f0b55a"
  warn-dot-dark: "#f0b55a"
  warn-soft-dark: "#33270f"
  ok-dark: "#5ccb90"
  ok-soft-dark: "#12301f"
  frozen-dark: "#cfd8e1"
  frozen-soft-dark: "#26303b"
  person-dark: "#7b9cf0"
  overlay-dark: "rgba(0, 0, 0, 0.6)"
typography:
  headline:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.2
    letterSpacing: "-0.01em"
    fontFeature: "tnum"
  title:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "20px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "-0.01em"
  title-sm:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.55
  body:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.55
  body-sm:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "Inter, Noto Sans Thai, system-ui, -apple-system, Segoe UI, Leelawadee UI, sans-serif"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: 1.5
rounded:
  sm: "4px"
  md: "6px"
  pill: "999px"
  full: "50%"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "20px"
  2xl: "24px"
  3xl: "28px"
components:
  button-default:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "5px 12px"
  button-default-hover:
    backgroundColor: "{colors.hover}"
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "{colors.accent-ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "5px 12px"
  button-primary-hover:
    backgroundColor: "{colors.accent-hover}"
  button-danger:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.danger-ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "5px 12px"
  button-danger-hover:
    backgroundColor: "{colors.danger-hover}"
  button-ghost:
    textColor: "{colors.ink-2}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "5px 12px"
  button-ghost-hover:
    backgroundColor: "{colors.hover}"
    textColor: "{colors.ink}"
  input-field:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body}"
    rounded: "{rounded.md}"
    padding: "8px 10px"
  search:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    height: "32px"
    width: "240px"
  nav-item:
    textColor: "{colors.ink-2}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "7px 8px"
  nav-item-active:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
  tier-freeze:
    backgroundColor: "{colors.danger-soft}"
    textColor: "{colors.danger}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  tier-review:
    backgroundColor: "{colors.warn-soft}"
    textColor: "{colors.warn}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  status-active:
    backgroundColor: "{colors.ok-soft}"
    textColor: "{colors.ok}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  status-frozen:
    backgroundColor: "{colors.frozen-soft}"
    textColor: "{colors.frozen}"
    typography: "{typography.label}"
    rounded: "{rounded.pill}"
    padding: "1px 8px"
  chip-signal:
    backgroundColor: "{colors.canvas}"
    textColor: "{colors.ink-2}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "1px 7px"
  panel:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.md}"
  dialog:
    backgroundColor: "{colors.surface}"
    rounded: "{rounded.md}"
    padding: "20px"
    width: "440px"
  toast:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.canvas}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
  toast-error:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.danger-ink}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.md}"
    padding: "10px 16px"
---

# Design System: Mule Investigator

## Overview

**Creative North Star: "The Case Ledger"**

A fraud investigator's working console in the fintech category standard, built to the craft level of Stripe Dashboard and fraud consoles such as Sardine and Unit21. It plays the category straight, with no themed costume. The screen holds a case queue, a reason for every flag and the money's path. Everything else stays quiet so that those three read first.

The world is restrained and dense. A cool canvas carries white working surfaces, and a second, cooler neutral layer holds the sidebar. Text is navy ink. One indigo accent marks primary action, selection and focus. Colour appears only where it carries state: red for the freeze tier, amber for review, green for active, and slate with a lock for frozen. Light and dark are both first-class and follow the system setting (`prefers-color-scheme`). There is no toggle.

**Key Characteristics:**
- One indigo accent; every other hue is a state.
- 1px borders and tonal layers carry structure. Shadows appear only on floating layers.
- One gently rounded corner (6px) across controls and surfaces.
- One sans family (Inter for Latin, Noto Sans Thai for Thai) with tabular numerals for every figure.
- A compact type scale (12/13/14/16/20/24) with no display size.
- Light and dark token sets with identical roles.

## Colors

Cool, low-chroma neutrals with one indigo accent and four semantic states, each with a solid and a soft tint.

### Primary
- **Signal Indigo** (`accent`, dark `accent-dark`): primary buttons (sign in, unfreeze, confirm), the focus outline, the focus ring on fields and search, links, the avatar, and text selection (`accent-soft`). The selected queue row uses its near-neutral cousin `selected`.

### Semantic states
- **Freeze Red** (`danger`): the freeze tier (tier pill, sidebar dot, overview figure), the Freeze button, suspect nodes in the trail, FREEZE audit entries, sign-out, error banners, and the error toast when the bank refuses a request. Text on a danger fill is `danger-ink`.
- **Review Amber** (`warn` text, `warn-dot` marker, `warn-soft` tint): the review tier and its overview figure.
- **Active Green** (`ok`, `ok-soft`): ACTIVE status, UNFREEZE audit entries, and incoming money in the timeline.
- **Frozen Slate** (`frozen`, `frozen-soft`): FROZEN status, which always pairs with the lock icon. It is never shown as colour alone.
- **Person Blue** (`person`): ordinary account nodes in the trail graph only.

### Neutral
- **Cool Canvas** (`canvas`): the page background, signal chips, and the graph's cropped-view cue.
- **White Surface** (`surface`): panels, tables, dialogs, menus, inputs, and the active nav item.
- **Sidebar Mist** (`sidebar`): the sidebar layer, one step cooler than the canvas.
- **Navy Ink** (`ink`, `ink-2`): primary text and secondary text such as nav items and ghost buttons. `ink` is also the toast background and the frozen ring in the graph.
- **Slate Muted** (`muted`): labels, column headers, timestamps, and the cash-vault node.
- **Hairline** (`line`, `line-strong`): dividers and panel borders; control borders and graph edges.
- **Hover Wash** (`hover`): hover background for rows, buttons and menu items.

### Named Rules
**The One Accent Rule.** Indigo is used only for primary action, selection and focus. It never marks a state, a category or decoration.

**The State-Carries-Meaning Rule.** Red, amber, green and slate appear only where they encode tier, status or money direction; red also marks destructive actions (freeze, sign-out) and errors. Each state comes as a solid for text or marks and a soft tint for the pill behind it.

**The Twin Theme Rule.** Every colour token has a dark counterpart with the same role. In dark mode the accent and danger fills take dark text, through `accent-ink` and `danger-ink`. Never add a colour that exists in only one scheme.

## Typography

**Body Font:** Inter (Google Fonts, weights 400/500/600)
**Thai Font:** Noto Sans Thai (weights 400/500/600), with fallback to system-ui, Segoe UI and Leelawadee UI

**Character:** A single neutral sans that stays out of the way. Figures carry the weight, set in tabular numerals with slight tracking (0.01em).

### Hierarchy
- **Headline** (600, 24px, 1.2 to 1.25): the account number in the detail panel and the overview figures (tabular).
- **Title** (600, 20px, 1.3, -0.01em): the page title in the top bar and the login heading.
- **Title small** (600, 16px): dialog headings and fact values (balance, opened, owner).
- **Body** (400, 14px, 1.55): the root size, block headings at 600, and field input text.
- **Body small** (13px): tables, buttons (500, 20px line), nav items, search, timeline, audit, banners and toasts.
- **Label** (500, 12px): column headers, stat labels, tier, status and signal chips, nav section label, hints and timestamps. Always sentence case. Never uppercase-tracked.

### Named Rules
**The Tabular Figures Rule.** Every amount, count, account number and timestamp uses `font-variant-numeric: tabular-nums` so columns align.

**The Thai Leading Rule.** Thai text (`:lang(th)`) gets line-height 1.65 instead of 1.55, so stacked vowels and tone marks never collide.

**The No Display Rule.** The scale stops at 24px. This is a working console, and no hero type belongs in it.

## Layout

The app shell is a 240px sticky sidebar plus a fluid workspace (padding 20px 28px 32px, 16px vertical gap). The workspace stacks a top bar (title, 240px search, refresh), a five-cell overview strip, and the work area. The work area puts the queue (1.2fr, sticky at 16px, max-height viewport minus 32px) beside a docked account detail (min 440px), with a 16px gap. The spacing rhythm is 4/8/12/16/20/24/28. Table cells are 9 to 10px by 12px, and the detail panel is padded 20px 24px with a 20px gap between blocks.

Responsive:
- **At 1180px and below**, the detail drops under the queue in one column. The queue stops being sticky and caps at 520px.
- **At 860px and below**, the sidebar becomes a top bar. Brand and account menu sit on one row, the queue views scroll horizontally below them, the user name hides, and the settings menu opens downward. Search goes full width. The overview becomes two columns, with the total spanning both, drawn with 1px gaps on a line-coloured background so no cell is left empty. The table hides wide columns and folds tier and status into the row. The screen shows one thing at a time: the queue, or the open account with a back button. Fact and audit grids collapse.

Grid tracks use `minmax(0, 1fr)` so a wide table cannot widen the page.

## Elevation & Depth

The system is flat, layered by tone and 1px borders: canvas, then sidebar, then surface. One shadow exists, and only floating layers use it.

### Shadow Vocabulary
- **Float** (light `0 8px 24px rgba(14,27,43,0.12), 0 2px 6px rgba(14,27,43,0.06)`; dark `0 8px 24px rgba(0,0,0,0.5), 0 2px 6px rgba(0,0,0,0.35)`): the settings menu, dialog, toast and login card.
- **Focus ring** (`0 0 0 3px` accent-soft, with an accent border): fields and search on focus.
- **Active nav hairline** (`0 0 0 1px` line): the selected sidebar view.

### Named Rules
**The Floating-Only Shadow Rule.** Panels, tables and cards sit flat on borders. Only layers above the page (menu, dialog, toast, login card) cast the soft float shadow. Hard or offset-only shadows never appear.

## Shapes

One gently rounded corner (6px) covers buttons, inputs, panels, dialogs, menus, toasts, the brand mark and the graph frame. Signal chips and skeleton bars use a tighter 4px. Tier and status pills are fully rounded (999px), and dots and avatars are circles. Borders are always 1px, in `line` for structure and `line-strong` for controls.

Icons come from one stroke set on a 24px grid: 1.5 stroke, round caps and joins, `currentColor`, no fill. They render at 12 to 16px. The product mark (a shield with a break in its line) is the only exception, at a 1.75 stroke.

## Components

### Buttons
Quiet and precise. A white bordered default; solid fills only for the one primary or destructive action.
- **Shape:** gently rounded (6px), 1px `line-strong` border, 13px/500 on a 20px line, padding 5px 12px. The small size is 12px, padding 3px 9px. The wide size is full width, padding 8px 12px.
- **Primary:** accent fill, accent-ink text. Sign in, Unfreeze, and confirm in the dialog.
- **Danger:** danger fill, `danger-ink` text. The Freeze button and the confirm button of its dialog.
- **Ghost / Icon:** transparent with ink-2 text, washing to `hover` with ink text. Icon buttons pad 6px.
- **Link:** accent text, underlined at 3px offset.
- **States:** background, border and colour transition at 150ms ease-out. Active shows `line`. Disabled is 0.55 opacity. A busy action shows a spinning refresh icon.

### Tier, status and signal chips
- **Tier and status pills:** 12px/500 on a fully rounded pill, padding 1px 8px, soft tint behind solid text. ACTIVE leads with a 6px currentColor dot. FROZEN leads with the lock icon.
- **Signal chips:** 12px on canvas, 1px `line` border, 4px radius, ink-2 text, `cursor: help` with a plain-language title. The graph rule `from_suspect` gets a danger-tinted border.

### Cards / Containers
- **Corner Style:** 6px.
- **Background:** surface on canvas.
- **Shadow Strategy:** none at rest (see Elevation).
- **Border:** 1px `line`.
- **Divided strips:** the overview, the facts and the audit list are one bordered surface split by 1px internal rules, not separate cards.

### Inputs / Fields
- **Style:** 1px `line-strong` border, surface background, 6px radius, 14px text, padding 8px 10px. Labels are 13px/500 ink-2, 6px above the field.
- **Focus:** the border turns accent, plus a 3px `accent-soft` ring. Elsewhere focus is a 2px accent outline at 2px offset (inset -2px on table rows).
- **Search:** 32px tall with a leading search icon. It takes the same ring through `:focus-within`.

### Navigation
- **Sidebar views:** 13px ink-2 rows, padding 7px 8px, 10px gap, each with an 8px state dot (grey, red, amber, slate) and a tabular count. Hover washes the row. The active view is surface with a 1px line hairline at 600 weight. A 12px muted "Queue" label groups the views.
- **Account menu:** at the sidebar foot, above a top border. The avatar is an accent-soft circle with accent initials. The menu opens upward as a floating layer, and sign-out shows in danger.

### Queue table
13px rows with a sticky 12px muted header. Hover washes the row, the selected row takes `selected`, and rows are keyboard-focusable. Tier first, then rule count. Amounts are tabular at 500.

### Trail graph (signature)
Cytoscape draws on canvas and cannot read CSS variables. At mount, and again whenever the OS colour scheme changes, it reads `--ink`, `--line-strong`, `--surface`, `--person`, `--danger`, `--muted` and `--font` from the document with `getComputedStyle`, so the graph follows the active theme without a reload. Person nodes are blue and suspects red. The cash vault is a muted rounded rectangle, and frozen accounts get a 5px ink ring on the 28px node; the legend's 14px dot carries a 3px ring in the same proportion. Edge width scales with the square root of the amount. Node and edge labels use the `--font` stack (14px nodes, 12px edges) on an 85% surface plate. A legend bar with a Fit button sits above the 1px framed canvas.

### Dialog and toast
The dialog is a 440px surface, padding 20px, with float shadow over a navy overlay. It rises 6px over 180ms on `cubic-bezier(0.16, 1, 0.3, 1)`. A reason is required before confirming. The toast is centred 20px from the bottom. A completed freeze or unfreeze shows on ink; only a refused request shows the error toast on danger.

## Do's and Don'ts

### Do:
- **Do** use the indigo accent only for primary action, selection and focus.
- **Do** pair every state colour with its soft tint, and pair FROZEN with the lock icon.
- **Do** set every figure in tabular numerals, and give Thai text its 1.65 line height.
- **Do** define every new colour in both the light and the dark token set.
- **Do** read theme colours from CSS variables at mount for anything drawn on canvas, rather than hard-coding hex.
- **Do** keep the 6px radius and 1px borders on new controls and surfaces.
- **Do** draw new icons on the 24px grid with a 1.5 round-capped stroke in currentColor.
- **Do** honour `prefers-reduced-motion`: animations and transitions switch off.

### Don't:
- **Don't** put shadows on panels, tables or cards. The float shadow is for menus, dialogs, toasts and the login card only.
- **Don't** add a second accent hue, or use a state colour for decoration.
- **Don't** add uppercase tracked labels or kicker lines above headings. Labels are 12px/500 sentence case.
- **Don't** set type above 24px.
- **Don't** use emoji or text glyphs as icons. Use the stroke set.
- **Don't** add a manual theme toggle. The scheme follows the system setting.
