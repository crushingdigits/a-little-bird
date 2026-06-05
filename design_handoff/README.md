# Handoff: Spoiler-Protection "Match Wall" — A Little Bird

## Overview
A spoiler-protection feature for **A Little Bird**, a spoiler-protected Liverpool FC news reader.
The app hides news that could spoil match results the user hasn't watched yet.

Protection works as a **time wall**: the wall sits at the **kickoff of the earliest game the user has
NOT yet marked watched**. News about any game at/after the wall is hidden in the feed. When the user
marks a game watched, the wall advances to the next unwatched game and that game's news is released.

This replaces the app's previous **bulk "reveal everything"** confirmation (a single `CatchUpSheet`
that revealed all held match content at once). The new feature is per-game:
- See Liverpool fixtures — recent results and upcoming games.
- Mark individual games watched, and un-mark (undo) a mis-tap.
- See at a glance how many games are currently hiding news.

### ⚠️ THE ONE HARD RULE (read first)
**Never reveal the score/result of a game the user has not marked watched — anywhere.**
A score is the exact spoiler we protect against. A score, result letter (W/D/L), or any derived result
may appear **only** for games whose state is `watched`. Unwatched played games show a "Mark watched"
action and **no result of any kind**; upcoming games show only their kickoff time.

In the code this rule is centralized in `scoreStr()` / `resultWord()` and the **`ScoreTrail`** component
(`01_integrated_app/wall.jsx`) — the score is only ever rendered inside `ScoreTrail`, which is only
mounted for watched rows. In the feed, a match story is gated by `!watched.has(item.game)`. **Keep this
single-gate pattern when porting** — never put a score into the DOM for an unwatched game, even hidden.

---

## About the design files
These are **design references built in HTML/React (in-browser Babel)** — prototypes showing the intended
look and behavior. They are **not production code to ship directly.** Recreate the design in the target
codebase's environment (the live app is Android/Material 3 — likely Compose, React Native, or Flutter),
using its existing components, theme, and patterns. Treat the HTML/CSS as the spec for visuals + behavior,
not for file layout.

### What's in the bundle (two folders)
- **`01_integrated_app/`** — ⭐ **PRIMARY.** The feature wired into the full app. Open
  **`A Little Bird.html`**. This is the source of truth for how the feature behaves *in context*: the feed,
  the protection card entry point, opening the sheet, marking a game, and news releasing in the feed.
- **`02_standalone_exploration/`** — Reference only. Open **`Spoiler Wall Sheet.html`** — a pan/zoom
  canvas showing the sheet's states side by side (default, all-caught-up, loading, offline) plus two
  **alternate layout explorations that were NOT chosen** (a vertical "timeline/wall" and a "next to watch"
  focus view). Use it to see edge states (loading, offline) and the rationale; build from `01_`.

## Fidelity
**High-fidelity (hifi).** Final colors, type, spacing, radii, and interactions are specified. Map the
tokens below onto the app's existing Material 3 theme. Replace placeholder crests with licensed assets.

---

## Design system context (inherited from the app — `app.css`)
Material 3 dark. Reuse these tokens; don't invent colors.

### Colors
| Token | Value | Use |
|---|---|---|
| `--surface` | `#1d1b20` | App background |
| `--sc-low` | `#19171d` | Recessed wells (status bar, list track) |
| `--sc` | `#232027` | Card surface |
| `--sc-high` | `#2a2731` | Sheet background |
| `--sc-highest` | `#322f3a` | Raised chips / crest fallback tile |
| `--on` / `--on-var` / `--on-dim` | `#ece5ee` / `#b4adbc` / `#8b8593` | Text: primary / secondary / tertiary |
| `--outline` / `--outline-2` | `rgba(236,229,238,.13)` / `.22` | Hairlines / borders |
| `--primary` | `#ff4d5e` | Brand red — CTAs, the wall, locks |
| `--on-primary` / `--primary-c` / `--on-primary-c` | `#43000c` / `#5c1421` / `#ffd9dd` | On-primary / container / on-container |
| `--green` (= `--t1`) | `#5fd6a0` | Watched / success / caught-up |
| `--t2` / `--t3` | `#8fb8ff` / `#f3b24e` | UCL tag / loss accent |

### Type
- **Display** — Archivo 700/800: titles, opponent names, scores.
- **Body** — Spline Sans 400–700 (the app's current tweak; Archivo is the fallback): subtitles, buttons, meta.
- **Mono** — Space Mono 400/700, uppercase, `letter-spacing .04–.1em`: eyebrows, comp tags, timestamps, count pills.

### Shape & motion
- `--r-lg: 28px` sheet corners · `--r-card: 22px` cards/protection card.
- In-sheet radii: 18px list well, 13px crest tile + status bar, 12px chips.
- Easing `cubic-bezier(.2,0,0,1)`; 0.15s hover/color, press-scale `.96` at 0.1s; sheet slide-in 0.28–0.3s.

---

## Anatomy & states

### 1. Feed entry point — the protection card (`ProtectionCard`, `wall.jsx`)
The protection card at the top of the feed **is the entry point** (we deliberately did NOT add a second
banner). Three states:
- **Protection off** → plain `.protban` card: shield-off, "Off · showing everything…", toggle. No action row.
- **Protection on, games hiding** → `.ws-protcard`: master toggle on top; divider; tappable action row
  with a red lock chip, "**N unwatched game(s) is/are** hiding newer stories", "Manage ›". Subtitle
  "On · hiding news from games you haven't watched". The action row opens the sheet.
- **Protection on, all caught up** → same card; subtitle "On · nothing to hide right now"; action row
  shows a green check, "**You're all caught up** — every game watched", "Fixtures ›". Still opens the sheet.

### 2. The sheet — "Fixtures & spoilers" (`MatchWallSheet`, `wall.jsx`)
Material modal bottom sheet (reuses the app's `.sheet.left` slide-up + `.scrim`). Top→bottom:
1. **Grip**, then **header**: title "Fixtures & spoilers" + close (×).
2. **Subtitle** (`.ws-sub`): when hiding, an inline mono **count pill** ("🔒 N UNWATCHED") + "game(s) is/are
   hiding newer stories. Mark it/them watched to catch up." When caught up: "All caught up — nothing's
   hidden. Every result below is yours to see."
3. **Status bar** (`.ws-statusbar`) — **always shown** (fixtures refresh ~1–2×/day): clock + "UPDATED 2 JUN,
   21:30" + "Refresh" pill. Offline → `.offline` modifier (primary tint), wifi-off, "**NO CONNECTION** ·
   UPDATED…", "Try again". Offline also shows the `.ws-offline-note` ("Marking still works offline…").
4. **Segmented control** (`.ws-seg`): "Results" / "Upcoming" (no count badges).
5. **Fixture list** (`.ws-list`, radius-18 well) of fixture rows.
6. **Footer** (`.ws-foot`): "A score is only ever shown once its game is watched."

#### Fixture row (`.ws-fx`) — grid `44px | 1fr | auto`, 13px gap/padding, hairline separators
- **Crest** (44×44, r13) with a venue badge (**H**/**A**, 18×18, r6, bottom-right). Fallback: 3-letter
  abbreviation tile when no crest asset.
- **Text**: opponent (Archivo 700, 16px, ellipsis) + meta line (comp tag + Home/Away + date, dot-separated).
  UCL comp tag is recolored (`--t2`).
- **Trailing — state-dependent (the hard rule):**
  - `watched` → `ScoreTrail`: score (Archivo 800, 18px, en-dash) + W/D/L chip + mono "✓ WATCHED", **plus a
    round Undo button** to re-hide. (W green, D neutral, L `--t3`.)
  - `unwatched` (played) → **"Mark watched"** primary pill + eye icon. **No score / no result hint.**
  - `upcoming` → kickoff time + weekday. No score.

#### Edge states (see `02_standalone_exploration`)
- **All caught up** — list still shows every result (with scores); no dead-end empty screen.
- **Loading** — shimmer skeleton rows (respects `prefers-reduced-motion`).
- **Offline** — shows the real cached fixtures with the escalated status bar + the marking-offline note.
  (Marking is local/offline-only — there is **no** "sync"; don't add sync copy.)

---

## Interactions & behavior (verified in `01_integrated_app`)
- **Mark watched** (in the sheet): advances the wall to the next unwatched kickoff, reveals that game's
  score in the sheet, and **un-redacts that game's tied story in the feed**. Shows a toast
  ("N match update(s) unlocked", with **Undo**). Local/optimistic — no network needed.
- **Un-mark**: the round undo button re-hides the result and pulls the wall back; the feed re-redacts.
- **Feed hiding rule**: `hide = protectionOn && item.match && item.game && !watched.has(item.game) &&
  !revealed.includes(item.id)`. Hidden match stories render as `RedactedCard`.
- **Per-item reveal-anyway**: tapping a redacted card still opens the existing `GateSheet` ("Reveal
  anyway / Keep hidden") for a one-off override (adds the id to `revealed`); this is independent of the wall.
- **Segmented switch**, **Refresh/Try again**: standard.

## State model
- `watched: Set<gameId>` — single source of truth (App state). Persist locally (per device) so it survives
  offline + reload. `INITIAL_WATCHED = ["bha","rma"]` in the prototype.
- `revealed: string[]` — per-item manual overrides from the gate (kept from the old model).
- Derived via `deriveWall(watched)` → `{ wall, hiding, hidingCount }`. `wall` = earliest played game not in
  `watched` (null when caught up).
- Per-fixture render state via `fxState(fixture, watched)` → `"watched" | "unwatched" | "upcoming"`.
- `seg` (results/upcoming), `protectionOn`, `wallOpen` — UI state.

### Data model linking news → games (IMPORTANT for the port)
Each spoiler-risk feed item has `match: true` and a **`game`** field = the fixture id it concerns
(`data.jsx`). A story is hidden iff its game is unwatched. In the prototype: `ft`→che, `match1`→eve,
`live`→new, `match2`→che. Fixtures live in `wall.jsx` (`FIXTURES`), each with `id, opp, abbr, venue,
compTag, date, ko, status: "played"|"upcoming", score`.

## Assets
- **Club crests** in `wall.jsx` (`CRESTS`) are **original placeholder kit-colour roundels**, NOT the clubs'
  real trademarked crests. **Replace with licensed crest images** keyed by the same fixture id in
  production; keep the abbreviation tile as the no-asset fallback.
- **Icons** — Material-style outline, `currentColor`, 2px stroke (`icons.jsx` + a few defined in `wall.jsx`:
  clock, wifi-off, undo, refresh, check-circle). Map to the codebase's icon set.
- **Fonts** — Archivo, Spline Sans, Space Mono (Google Fonts).

## Files
### `01_integrated_app/` (primary — open `A Little Bird.html`)
| File | Role |
|---|---|
| `A Little Bird.html` | App shell + state. Holds `watched`, `deriveWall`, `markWatched(gameId)`, `unmarkWatched`, sheet/gate wiring. |
| `wall.jsx` | **The feature.** Fixtures + wall logic, crests, `MatchWallSheet`, merged `ProtectionCard`, `ScoreTrail` (hard-rule gate). |
| `wall-sheet.css` | Sheet/card styles (prefix `.ws-`). |
| `screens.jsx` | `FeedScreen` (game-based hiding + `ProtectionCard` entry), settings/alerts/detail/gate/toast. |
| `ui.jsx` | Shared bits (`NewsCard`, `RedactedCard`, `TopBar`, `Sw`, …). |
| `data.jsx` | Feed content — match items carry the `game` link. |
| `app.css` | App's Material 3 theme (token source). |
| `filters.jsx`, `icons.jsx`, `android-frame.jsx`, `tweaks-panel.jsx` | Existing app chrome / scaffolding. |

### `02_standalone_exploration/` (reference — open `Spoiler Wall Sheet.html`)
States + alternate layouts on a design canvas. `wall-sheets.jsx` (chosen segmented sheet + 2 unchosen
alternates), `wall-shared.jsx`, `wall-data.jsx`, `wall-crests.jsx`, `wall-icons.jsx`, `design-canvas.jsx`
(presentation wrapper — not part of the feature).

## How to view
Open `01_integrated_app/A Little Bird.html`. Tap **Manage** on the protection card to open the sheet; mark a
game watched and watch its story appear in the feed behind. For loading/offline states, open
`02_standalone_exploration/Spoiler Wall Sheet.html`.
