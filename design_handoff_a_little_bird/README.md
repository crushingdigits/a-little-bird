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

### Filtering & feeds (this iteration's focus)
The app now lets the reader **decide what they read first and how much they trust it**,
via two orthogonal controls:
- **News type** — a *single-select* feed switcher: **All · Transfers · Match · Squad &
  staff · Contracts · Academy**. Picks one feed at a time.
- **Reliability** — a *single cumulative level* (NOT multi-select): level **N shows
  every tier from 1 down to N**. T1 = reliable only; T2 = +beat writers; T3 =
  everything. It is a global setting that sits right beside the type switcher so it's
  always one tap away.

The two are independent: switching feeds does **not** change your reliability level
(there is intentionally no per-type memory). Both persist across sessions, the user
can **pin a default feed**, and filtering is fully **independent of spoiler protection**
(a Match feed still redacts match cards when protection is on).

Three alternative **surfaces** for choosing the news type are prototyped as switchable
options (Tweaks → "Filter style"): **Tabs**, **Chips**, **Feed sheet**. These are
design explorations — **ship ONE** (recommended: **Tabs**), documented in the
"Filtering & feeds" section below.

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
- **Purpose**: Primary surface. Reverse-chronological list of news cards, scoped by
  the active **news type** and **reliability level**.
- **Layout**: Vertical scroll. Sticky top app bar (60dp). **Filter chrome** sits
  directly below the app bar (type switcher + reliability level — see "Filtering &
  feeds"). Content column with 14dp horizontal padding, 12dp gap between cards, 96dp
  bottom padding (clears the nav bar). Fixed bottom navigation (76dp).
- **Empty state**: when no stories match the active feed + reliability level, the list
  is replaced by a centered empty state (info tile, "Nothing here right now",
  explanatory subtitle, and a tonal "Show all tiers" button that resets the level to 3).
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

## Filtering & feeds (detailed spec)

### Data model
- Each feed item already has a raw `cat` (`Transfer`, `Coaching`, `Match`, `Contract`,
  `Backroom`, `Academy`) and a `tier` (1/2/3).
- **News-type groups** map one or more raw categories to a user-facing feed
  (`data.jsx` → `NEWS_TYPES`):
  | key | label | short | maps to cats |
  |---|---|---|---|
  | `all` | All news | All | (everything) |
  | `transfer` | Transfers | Transfers | `Transfer` |
  | `match` | Match news | Match | `Match` |
  | `staff` | Squad & staff | Squad & staff | `Coaching`, `Backroom` |
  | `contract` | Contracts | Contracts | `Contract` |
  | `academy` | Academy | Academy | `Academy` |

### Filter logic (the contract — `filters.jsx` → `filterFeed`)
```
visible = FEED
  .filter(i => i.tier <= tierMax)              // cumulative reliability level
  .filter(i => type==='all' ? true             // single-select news type
                            : group(type).cats.includes(i.cat))
```
- `tierMax ∈ {1,2,3}` (default **3** = show all). **Single value, not a set** — picking
  a level shows every more-reliable tier automatically.
- `type` is one group key (default = the pinned feed, else `all`).
- Counts shown on tabs/chips/rows are `typeCount(typeKey, tierMax)` — the number of
  items in that feed **at the current reliability level** (they update live as the
  level changes).

### The reliability "ladder" control (shared by all surfaces)
A connected 3-segment control, left→right T1 T2 T3, that fills cumulatively from the
left up to the chosen level:
- Container: `inline-flex`, 1px outline, radius 16, `surface-container` bg, segments
  divided by 1px hairlines (no gaps — the joined shape signals an ordinal scale, not
  multi-select).
- Each segment: 32dp tall, 13dp horizontal padding, Space Mono 700 / 11sp; a 7dp pip
  (T1 green / T2 blue / T3 amber) + the label "T1/2/3".
- **Included** segments (tier ≤ level): `surface-container-highest` bg, `on-surface`
  text, pip at full opacity. **Excluded**: transparent, `on-dim` text, pip at 0.35.
- **Chosen level** segment additionally carries a 3dp inset bottom accent in `primary`
  (the "down to here" boundary marker).
- Tap a segment → set `tierMax` to that tier.

### Pin a default feed
- A pin button (`PinBtn`) toggles whether the **current** type is the user's default
  landing feed. Filled (`primary`) when the current feed is pinned.
- The pinned feed key is stored; on a fresh launch the app opens on it (falls back to
  `all`). In Tabs/Chips the pinned feed shows a small filled-pin glyph; the Feed sheet
  has an explicit "Pin '{feed}' as default" row.

### Persistence
- All filter state persists across sessions. Prototype uses `localStorage` key
  **`alb.filters`** = `{ type, tierMax, pinned }`. Map to DataStore/preferences natively.
- Reliability is **global** — deliberately NOT remembered per type.

### The three surfaces (ship ONE — recommend Tabs)
**A. Tabs** *(recommended default)* — a horizontally-scrolling top tab strip for the
news type (single-select; active tab = `primary` underline, count after each label),
with a slim control row beneath it: the reliability ladder (labelled "RELIABILITY",
left) + pin button (right). Bottom nav stays 2-item (Feed/Settings).

**B. Chips** — one sticky bar holding, left→right: the reliability ladder, a hairline
divider, single-select type **chips** (pill, `primary-container` when active, count
inside), another divider, the pin button. Everything scrolls horizontally together.

**C. Feed sheet** — a compact lead row: a "Feed" button (list icon + current feed
name + chevron) on the left and the reliability ladder on the right. Tapping the
button opens a bottom sheet ("Choose a feed") listing every feed with its count, a
check on the active one, and a "Pin as default" action.

### Interaction notes
- Type and reliability are independent; changing one never resets the other.
- Counts and the empty state both react to the current reliability level.
- Spoiler protection is unchanged and orthogonal: a redacted Match card stays redacted
  inside the Match feed until revealed/marked-watched.

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
- **Filtering** (this iteration): `type: String` (news-type group key, default = pinned
  or `"all"`), `tierMax: 1|2|3` (cumulative reliability level, default `3`),
  `pinned: String|null` (default feed key), `feedOpen: Boolean` (Feed-sheet surface).
  Handlers: `onSetTier(n)`, `onType(k)`, `onPin()` (toggles pinned↔current),
  `resetTiers()` (→ 3). Persisted to `localStorage["alb.filters"] = {type, tierMax, pinned}`.
  The active surface is chosen by the `filterStyle` tweak (`Tabs|Chips|Feed sheet`) —
  in production you ship a single surface, so this is not app state.
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
- **Corner style is set to "Sharp"** (the chosen setting): card radius **12**, sheet/
  large radius scales down accordingly. (The prototype's "Rounded" alternative uses
  card radius **22** / large **28** — driven by the `--r-card` token via the Corners
  tweak. Ship Sharp.) Chips **7–8**, buttons **full** (pill, height 42–50). Icon
  tiles 11–13.
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
  confirmed, match, image, lead, paras, corrob), `TIER_META`, and the filtering config
  **`NEWS_TYPES`** (news-type groups) + `TIER_MODES` (legacy label helper).
- `filters.jsx` — **filtering feature**: `filterFeed`/`typeCount` logic, the
  `TierLevel` ladder, `PinBtn`, and the three type-switcher surfaces (`TypeTabs`,
  `ChipBar`, `FeedButton`+`FeedSheet`) + `FilterEmpty` empty state.
- `ui.jsx` — shared components: Sw, TierChip, NewsCard, RedactedCard, TopBar,
  ProtBanner, BottomNav, CatchUpCard.
- `screens.jsx` — FeedScreen (renders the active filter surface + filtered list +
  empty state), SettingsScreen, AlertsScreen, AlertsSubscreen, DetailScreen, GateSheet,
  CatchUpSheet, Toast.
- `icons.jsx` — outline icon set (now incl. `IcPin`, `IcPinFill`, `IcList`).
- `android-frame.jsx`, `tweaks-panel.jsx` — prototype scaffolding only (the device
  bezel and the design-time tweak panel); **not** part of the app to ship.
- Wireframes for context live in the project root (`A Little Bird - Wireframes.html`).

## How to run the prototype
Open `A Little Bird.html` in a browser. Interact with a mouse/touch. The right-edge
"Tweaks" panel is a design tool, not an app feature — it switches the **Filter style**
(Tabs/Chips/Feed sheet) and tunes accent, display font, corners.
