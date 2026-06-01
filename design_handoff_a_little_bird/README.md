# Handoff: A Little Bird — LFC spoiler-safe news app

## Overview
**A Little Bird** is an Android news app that aggregates Liverpool FC transfer and
coaching/backroom news from multiple RSS sources into one feed, with push
notifications. Its defining feature is **spoiler protection**: a user who watches
matches on delay can switch protection on so the app filters out all match-related
content (scores, results, live updates, player ratings, match reports, previews)
while keeping transfer and coaching news flowing.

Spoiler protection is a **mode**, not the whole app:
- **OFF** → everything shows, including match content (a normal LFC news app).
- **ON** → match content is redacted inline (tap-to-reveal behind a confirm gate),
  and held match notifications are never delivered until the user marks the match
  **watched**.

Every source is labelled and carries a **reliability tier**:
- **Tier 1 — RELIABLE** (green): official club + top journalists (Ornstein, Romano;
  "here we go" = confirmed).
- **Tier 2 — BEAT** (blue): established beat writers (This Is Anfield, Liverpool Echo).
- **Tier 3 — SPECULATIVE** (amber): tabloid/aggregated speculation (Mirror, Sun).

## About the Design Files
The files in this bundle are **design references created in HTML/React+Babel** —
an interactive prototype showing the intended look and behaviour. They are **not
production code to ship directly**. The task is to **recreate these designs in the
target environment** — for an Android app that almost certainly means **Jetpack
Compose with Material 3** (the prototype is built to the M3 spec), reusing the
project's existing theme, components and navigation patterns. If starting fresh,
Compose + Material 3 is the recommended stack.

The prototype is wrapped in a simulated Android device frame and uses web fonts +
CSS; map these to native equivalents (see Design Tokens).

## Fidelity
**High-fidelity.** Final colours, typography, spacing, component styling and
interactions are all intended as shown. Recreate the UI faithfully using the
codebase's Material 3 theme. Exact values are documented below.

---

## Screens / Views

### 1. Feed (home)
- **Purpose**: Primary surface. Reverse-chronological list of news cards.
- **Layout**: Vertical scroll. Sticky top app bar (60dp). Content column with 14dp
  horizontal padding, 12dp gap between cards, 96dp bottom padding (clears the nav
  bar). Fixed bottom navigation (76dp).
- **Components (top → bottom)**:
  - **Top app bar**: left = 34dp red rounded emblem (app-icon placeholder, replace
    with the real Liverbird mark) + wordmark "A Little Bird" (Archivo 800, 19sp) with
    mono sublabel "LFC TRANSFERS & BACKROOM" (Space Mono, 8.5sp, on-dim). Right =
    shield toggle button (filled red `primary-c` bg + `primary` icon when ON, ghost
    when OFF) and an overflow (⋮) button.
  - **Protection banner**: full-width rounded card (radius 22). When ON, tinted with
    `primary-c`/`primary` border and a filled shield icon; title "Spoiler protection",
    subtitle "Match scores, results & reports are hidden" / (OFF) "Showing everything,
    including match content". Right = M3 switch. Tapping the card or the switch toggles.
  - **Catch-up card** (only when protection ON **and** ≥1 held match item): surface
    container card, grid row [40dp icon | text], green eye icon, title
    "N match update(s) held", subtitle "Watched the match? Release them when you're
    ready.", then a full-width filled button "✓ Mark match as watched".
  - **News card** (`NewsCard`): rounded `sc` card, 1px outline. Optional 150dp image
    header (striped placeholder labelled "PRESS PHOTO" — replace with real press
    photography). Body padding 14/15dp: row 1 = tier chip + source (Space Mono 10.5sp)
    + relative time (right); headline (Archivo 700, 18sp, line-height 1.18); footer
    chips = category + (if confirmed) green "✓ Here we go" pill.
  - **Redacted card** (`RedactedCard`, shown for match items while protection ON):
    dashed red-tinted card; blurred placeholder bars behind a centered overlay: red
    lock tile, "Match content hidden", "{source} · tap to reveal". Tapping opens the gate.

### 2. Item detail (push overlay)
- **Purpose**: Read a story; see trust signals.
- **Layout**: Full-screen overlay sliding in from the right. Bar (60dp) with back +
  bookmark + share. Scrollable body, 20dp horizontal padding.
- **Components**: meta row (tier chip + "{time} ago" + optional Here-we-go pill);
  headline (Archivo 800, 28sp); optional press-photo placeholder (radius 18); byline
  (36dp circular avatar with initials + author name + "{role} · {TIER LABEL}");
  lead paragraph (17sp, on-surface, weight 500) then body paragraphs (15.5sp, on-var,
  line-height 1.6); **corroboration card** for confirmed stories ("CORROBORATION ·
  N sources" + rows of tier chip + source + green check); action row = Save (tonal),
  Share (tonal), Mute (text). Actions fire a toast.

### 3. Spoiler-risk gate (bottom sheet)
- **Purpose**: Intercept a flagged match item before it can spoil the user.
- **Trigger**: tapping a redacted match card while protection is ON and the item is
  not yet revealed.
- **Components**: scrim (50% black) + bottom sheet (radius 28 top). Drag handle, red
  shield tile, title "This one might spoil you" (Archivo 800, 22sp), subtitle naming
  the source + risk, amber tag "MATCH CONTENT · RESULT LIKELY", full-width buttons
  "Reveal anyway" (filled) and "Keep it hidden" (tonal), footer "WHY WAS THIS HIDDEN?".
  Reveal → marks item revealed and opens its detail.

### 4. Catch-up confirm (bottom sheet)
- **Purpose**: "Mark as watched" — release all held match content at once.
- **Components**: same sheet shell; green eye tile; "All caught up?"; subtitle
  "Marking the match as watched reveals N held update(s) — scores, results and player
  ratings will be shown."; buttons "Yes, reveal them" (filled) / "Not yet" (tonal);
  footer "FUTURE MATCHES STAY PROTECTED". Confirm reveals all held match items inline
  and shows a toast "N match updates unlocked" with an **Undo** action.

### 5. Settings (tab)
- **Purpose**: Configure spoiler protection; entry point to Alerts.
- **Layout**: Large screen header "Settings". Grouped M3 cards.
- **Groups**:
  - **Master card**: "Protection on/off" + switch (tinted when on).
  - **What counts as a spoiler**: checklist rows (Scores & results, Live match
    updates, Player ratings, Match reports, Previews & predicted XI, Lineups & team
    news). Checkbox tiles, red when checked. Lineups default OFF; rest ON.
  - **When something is flagged**: radio rows — "Blur inline" (default, matches feed) /
    "Send to Quarantine".
  - **Schedule**: switch "Turn on around match windows" (subtitle "Matchday −2h until
    you mark it watched").
  - **Notifications**: navigation row "Alerts" (bell icon + chevron) → pushes Alerts.

### 6. Alerts (push subscreen of Settings)
- **Purpose**: Notification preferences; spoiler-safe delivery.
- **Layout**: Overlay (slides in) with a back bar, then large header "Alerts".
- **Components**:
  - **Lock-screen preview**: a styled phone-lock-screen card (gradient wallpaper) with
    big clock, two delivered pushes (a "here we go" confirmation; a coaching update),
    and one dashed "HELD BY SPOILER PROTECTION — 1 match update hidden until you're
    caught up" push. Illustrative.
  - **Notify me about**: switches — Confirmed signings (★ priority), Transfers &
    rumours, Coaching & backroom, Contracts & academy.
  - **Who can notify · source tier**: radios — Tier 1 only / Tier 1 + Tier 2 (default) /
    All sources.
  - **Delivery**: Quiet hours switch (10pm–8am).
  - **Spoiler-safe callout** (primary-tinted): "Match notifications are never delivered
    while spoiler protection is on — they wait until you mark the match watched."

### 7. Bottom navigation
Two destinations: **Feed**, **Settings**. M3 style — pill indicator (`primary-c`)
behind the active icon, label below. (Alerts is intentionally a Settings subscreen,
not a top-level destination.)

### 8. Toast
Bottom snackbar (above nav), `sc-highest` bg, leading check icon, message, optional
trailing text-button action (e.g. UNDO in `primary`). Auto-dismiss ~2.6s.

---

## Interactions & Behavior
- **Protection toggle**: shield in app bar AND banner switch AND Settings master —
  all bound to one `protectionOn` boolean.
- **Open an item** (`openItem`): if `protectionOn && item.match && !revealed` → open
  the **gate**; otherwise open **detail**.
- **Reveal (gate)**: add the item id to `revealed`, close the sheet, open its detail.
- **Mark as watched** (`markWatched`): add every held match id to `revealed`, close
  the sheet, toast with **Undo** (Undo restores the prior `revealed` set).
- **Navigation**: bottom nav swaps Feed/Settings; detail, gate, catch-up sheet and
  Alerts are overlays/sheets above the current tab.
- **Animations**: detail/Alerts slide in from the right (~320ms), sheets slide up
  (~340ms) over a fading scrim (~280ms), toast fades/rises (~250ms). Easing
  `cubic-bezier(0.2,0,0,1)` (M3 standard). In Compose use `AnimatedVisibility` /
  `slideIn*` + `fadeIn`; you do **not** need the prototype's setTimeout/transition
  workaround (that only existed to dodge requestAnimationFrame throttling in the
  preview iframe).

## State Management
Prototype keeps everything in local React state at the App root. Native equivalents
(ViewModel + state holders):
- `protectionOn: Boolean`
- `tab: "feed" | "settings"`
- `showAlerts: Boolean`
- `revealed: Set<String>` (ids of match items the user has chosen to see)
- `detailItem`, `gateItem`, `catchUp`, `toast {msg, action}` (transient UI)
- Spoiler settings: `types {scores, live, ratings, reports, previews, lineups}`,
  `handling: "blur" | "quarantine"`, `autoWindow: Boolean`
- Alerts: `cats {confirmed, rumours, coaching, contracts}`, `tier: "t1"|"t12"|"all"`,
  `quiet: Boolean`
- Persist user prefs (protection, settings, alerts, revealed) to local storage /
  DataStore. Real implementation also needs: RSS ingestion, per-source tier mapping,
  a **match-content classifier** (keyword/category rules that decide `item.match`),
  and notification scheduling that respects protection + quiet hours.

## Design Tokens

### Colors (dark theme — the priority/base theme)
| Token | Value | Use |
|---|---|---|
| surface | `#1d1b20` | app background (matches device) |
| surface-container-low | `#19171d` | bottom nav |
| surface-container | `#232027` | cards |
| surface-container-high | `#2a2731` | elevated, sheet |
| surface-container-highest | `#322f3a` | toast, inputs |
| on-surface | `#ece5ee` | primary text |
| on-surface-variant | `#b4adbc` | secondary text |
| on-surface-dim | `#8b8593` | meta/labels |
| outline | `rgba(236,229,238,.13)` | hairlines |
| outline-strong | `rgba(236,229,238,.22)` | emphasis borders |
| **primary** | `#ff4d5e` | brand red — protection, FAB, active nav, buttons |
| on-primary | `#43000c` | text/icon on primary |
| primary-container | `#5c1421` | tonal red surfaces |
| on-primary-container | `#ffd9dd` | text on primary-container |
| tier-1 / RELIABLE | `#5fd6a0` (container `rgba(95,214,160,.15)`) | verified |
| tier-2 / BEAT | `#8fb8ff` (container `rgba(143,184,255,.14)`) | beat writers |
| tier-3 / SPECULATIVE | `#f3b24e` (container `rgba(243,178,78,.15)`) | speculation |
| confirmed/green | `#5fd6a0` | "here we go" |

Accent (primary) is tweakable in the prototype; offered options: `#ff4d5e`,
`#ff3b54`, `#ff6a4d`. Use the official LFC red in production if licensed.

### Typography
- **Display/headlines & wordmark**: Archivo (700/800). Map to a bold grotesque (or
  the brand font). Sizes: screen header 30/800/-0.02em; detail headline 28/800;
  card headline 18/700/-0.01em; section/sheet title 22/800.
- **Body/UI**: Archivo (400/500/600). Body 15.5sp/line-height 1.6; labels 12–14sp.
- **Mono (metadata, tiers, timestamps, tags)**: Space Mono (400/700), uppercased,
  letter-spacing ~0.04–0.1em, 8.5–11sp.

### Radius / spacing / elevation
- Card radius **22**, sheet/large radius **28**, chips **7–8**, buttons **full**
  (pill, height 42–50). Icon tiles 11–13.
- Spacing rhythm: 12dp card gaps, 14dp screen padding, 14–16dp card padding.
- Switch: 52×32 track, thumb grows 16→24 when on. Tap targets ≥44dp.
- Shadows are minimal (dark M3 relies on surface tonal steps, not heavy shadows);
  toast uses `0 10px 30px rgba(0,0,0,.4)`.

## Assets
- **App emblem**: placeholder rounded-square red mark — replace with the real
  Liverbird crest art (respect club brand/licensing).
- **Press photos**: striped placeholders labelled "PRESS PHOTO" — supply real
  imagery from licensed feeds.
- **Icons**: simple Material-style outline icons (shield, bell, home, tune/sliders,
  back, bookmark, share, check, lock, chevron, star, eye, more). Use the standard
  Material Symbols set in production.
- Fonts: Archivo + Space Mono (Google Fonts) — swap for bundled equivalents.

## Files (in this bundle)
- `A Little Bird.html` — entry point; App state, screen routing, device frame, tweaks.
- `app.css` — all styling / design tokens (the source of truth for values above).
- `data.jsx` — `FEED` sample items (fields: id, tier, source, author, role, cat, time,
  confirmed, match, image, lead, paras, corrob) and `TIER_META`.
- `ui.jsx` — shared components: Sw, TierChip, NewsCard, RedactedCard, TopBar,
  ProtBanner, BottomNav, CatchUpCard.
- `screens.jsx` — FeedScreen, SettingsScreen, AlertsScreen, AlertsSubscreen,
  DetailScreen, GateSheet, CatchUpSheet, Toast.
- `icons.jsx` — outline icon set.
- `android-frame.jsx`, `tweaks-panel.jsx` — prototype scaffolding only (the device
  bezel and the design-time tweak panel); **not** part of the app to ship.
- Wireframes for context live in the project root (`A Little Bird - Wireframes.html`).

## How to run the prototype
Open `A Little Bird.html` in a browser. Interact with a mouse/touch. The right-edge
"Tweaks" panel (accent, display font, corners) is a design tool, not an app feature.
