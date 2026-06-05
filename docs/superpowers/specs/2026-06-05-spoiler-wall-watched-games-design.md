# Spoiler Wall — Per-Game "Watched" State + Fixtures Schedule — Design

- **Date:** 2026-06-05
- **Status:** Agreed during brainstorming — precedes the implementation plan.
- **Proposed branch:** `feat/spoiler-wall-watched-games`
- **Scope:** Replace the current "Mark match as watched" feature (a single bulk reveal-all over fuzzy `isMatch` classification) with a real **per-game spoiler wall**: fetch a Liverpool fixtures schedule, let the user mark **each game** watched independently, and hide **all** feed news published at/after the earliest unwatched game's kickoff. Adds a fixtures bottom sheet and folds the entry point into the existing protection card.

This is a design record. It precedes the implementation plan.

The handoff references live in [`design_handoff/`](../../../design_handoff/) — chiefly [`01_integrated_app/wall.jsx`](../../../design_handoff/01_integrated_app/wall.jsx) (the feature: fixtures, wall logic, `MatchWallSheet`, `ProtectionCard`, `ScoreTrail`), [`01_integrated_app/data.jsx`](../../../design_handoff/01_integrated_app/data.jsx), and [`README.md`](../../../design_handoff/README.md). The standalone [`02_standalone_exploration/`](../../../design_handoff/02_standalone_exploration/) shows the sheet's edge states (loading, offline) and two unchosen layouts. They are **reference-only**: recreate them faithfully in Compose + Material 3, reusing the existing theme and components.

> **One deliberate divergence from the handoff** (see §2 and §5): the prototype hides news by linking each story to a specific fixture (`item.game`). We **do not** build that linkage. Our hiding rule is purely **date-based** (hide everything at/after the wall). The handoff's *visuals and sheet behavior* are adopted; its *per-item `game` link* is dropped.

---

## 1. Background

Today a "match" is not a game — it is any news item the classifier flagged `isMatch: Boolean`. Spoiler protection redacts those flagged items when protection is on (`FeedViewModel` builds `FeedUiState` via `combine(observeFeed, observeProtection, observeRevealed, …).stateIn(...)`), and "Mark match as watched" (`MarkWatched` use case + `CatchUpCard`/`CatchUpSheet`) **reveals every held match item at once**. There is no schedule, no individual game, no date. This is why the feature is useless and misleading: the user cannot mark a *specific* game watched, and any misclassified story leaks the result.

The redesign introduces a real schedule and a **time wall**:

> **Wall** = the kickoff of the **earliest *played* game the user has not yet marked watched**.
> Any feed item published **at/after the wall** is hidden. Marking that game watched advances the wall to the next unwatched game and releases the band of news up to it. Watch them all → wall vanishes → everything visible.

## 2. Decisions locked during brainstorming

| # | Decision | Choice | Why |
|---|---|---|---|
| 1 | Schedule data source | **TheSportsDB** behind a `ScheduleDataSource` interface | Best *coverage* of every Liverpool game (incl. cups/friendlies) with no API key required; for a spoiler wall a *missed game* is the worst failure. Interface keeps it swappable. |
| 2 | JSON parsing | **kotlinx.serialization** | Already applied in `:app` (`libs.plugins.kotlin.serialization` + `kotlinx-serialization-json`). Zero new setup. (Existing RSS feed parsing stays XML.) |
| 3 | What gets hidden after the wall | **ALL news** (date-based) | No reliable way to classify spoilers; hide-all is safe and needs zero classification or news→game linking. Supersedes the handoff's per-item `game` link. |
| 4 | Per-item escape hatch | **Keep** the existing `GateSheet` "Reveal anyway" | Lets an impatient user knowingly peek one item; reuses `revealed` set. Copy changes from "match content" → "published after a game you haven't watched". |
| 5 | Day-1 seeding | **Past = watched, recent unwatched** | On first sync, seed games with `kickoff < firstSyncAt − 3 days` as watched; recent (≤3 days) + upcoming start unwatched. Avoids a day-1 feed that's entirely walled, while still protecting last night's recorded game. Window is a tunable constant. |
| 6 | Schedule UI surface | **Bottom sheet from the feed** | Focused "catch up" interaction; no new screen/route. Adopts the handoff's `MatchWallSheet`. |
| 7 | Entry point | **Merged into the protection card** (`ProtectionCard`) | The handoff deliberately avoided a second banner; the existing protection card gains a tappable "Manage ›" action row. Replaces `ProtectionBanner` + `CatchUpCard`. |
| 8 | Watched-state storage | **Local only** (DataStore), no server sync | Per-device, survives offline + reload, optimistic/instant. There is **no** "sync" of watched state — only the *fixtures* are fetched. |
| 9 | Wall composition | **Earliest *played* unwatched game** | Upcoming games can't hide news yet; they become wall candidates only once played. |
| 10 | Score handling | **Preserve in data, gate in UI** | Scores are fetched and stored, but rendered **only** for watched games (centralized score gate). A score is the exact spoiler. |

## 3. The core model

```
wall   = earliest game with status=PLAYED and id ∉ watchedGameIds, by kickoff   (null ⇒ caught up)
hidden = protectionOn AND wall != null AND item.publishedAt ≥ wall AND item.id ∉ revealed
```

Per-fixture render state (drives the sheet rows):

```
fxState(game) =
    UPCOMING   if game.status == UPCOMING (kickoff in the future)
    WATCHED    if game.id ∈ watchedGameIds
    UNWATCHED  otherwise (played, not yet watched)
```

`status` is derived: `PLAYED` if `kickoff < now` (or the API marks it finished), else `UPCOMING`.

Both `kickoff` and `NewsItem.publishedAt` are `Instant` (UTC) — no timezone drift in the comparison.

## 4. Data layer

### 4.1 Schedule source (new package `data/schedule/`)

```kotlin
// data/schedule/datasource/remote/ScheduleDataSource.kt
interface ScheduleDataSource {
    suspend fun fetchFixtures(): List<GameDto>   // throws on network/parse error
}
```

- **`TheSportsDbScheduleDataSource`** (impl) — fetches Liverpool's recent + upcoming fixtures from TheSportsDB and parses with kotlinx.serialization. Reuses the existing OkHttp `FeedFetcher` for the HTTP call.
  - Endpoints (free tier, v1): `eventslast.php?id={teamId}` (last events) + `eventsnext.php?id={teamId}` (next events). The free "last-N / next-N" window aligns with the feature's need — we only care about recent-unwatched + upcoming, never months-old games.
  - `teamId` (Liverpool) and the API key are `BuildConfig`/constants (key defaults to the free public test key). **Verify the exact team id, key, endpoint shape, and JSON field names against live TheSportsDB docs at implementation time.**
  - **`GameDto`** — `@Serializable`, mirrors TheSportsDB's event JSON (id, opponent/home+away team names + ids, league, date+time, home/away score, status). Unused fields are ignored (`ignoreUnknownKeys = true`).

### 4.2 Persistence (new + extend)

- **`GameEntity`** (Room, new `games` table) — `id (PK), opponent, abbreviation, isHome (Boolean), competition, kickoffEpochMs (Long), status, homeScore (Int?), awayScore (Int?)`. Scores stored; never rendered for unwatched games (UI gate). Indexed by `kickoffEpochMs`.
- **`GameDao`** — `observeAll(): Flow<List<GameEntity>>` (by kickoff), `upsertAll(...)`, `count()`.
- **Room migration** — additive (new table), bump DB version.
- **`PreferencesDataSource`** (extend) — new keys:
  - `watched_game_ids: StringSet` — explicit watched marks (mirrors `revealed_ids`).
  - `first_schedule_sync_at: Long` — written once; anchors the day-1 seed.
  - `last_schedule_sync_at: Long` — surfaced as "Updated …" in the sheet status bar.

### 4.3 Repository + domain model

- **`ScheduleRepository`** (interface + impl) — SSOT = Room.
  - `observeSchedule(): Flow<List<Game>>`
  - `suspend fun refresh(): Result<Unit>` — fetch DTOs → upsert entities → on first success, run the day-1 seed and stamp `first_schedule_sync_at`; always stamp `last_schedule_sync_at`.
- **`Game`** (domain model) — `id, opponent, abbreviation, isHome, competition, kickoff: Instant, status: GameStatus, homeScore: Int?, awayScore: Int?`.
- **Mappers** — `GameDto.toEntity()` (parse date/time → epoch ms, map team names → opponent + home/away from Liverpool's perspective), `GameEntity.toDomain()`.

### 4.4 Crests / badges

TheSportsDB provides team badge URLs; use them (loaded via the project's image loader) with the **abbreviation tile as the no-asset fallback** (`Crest` in the handoff). Note the README's trademark caveat — fine for personal use; swap to licensed assets if ever distributed.

## 5. Domain layer (use cases)

Following the project's UseCase convention (`fun interface` = action name, `…UseCase` impl in the same file).

**Added**
- `SyncSchedule` — `suspend operator fun invoke(): Result<Unit>` → `ScheduleRepository.refresh()`. Called on app open (debounced ~once / few hours) and from the sheet's Refresh.
- `ObserveSchedule` — `operator fun invoke(): Flow<List<Game>>`.
- `ObserveWatchedGames` — `operator fun invoke(): Flow<Set<String>>`.
- `MarkGameWatched` — `suspend operator fun invoke(gameId: String, watched: Boolean): Set<String>` — bidirectional toggle (watch / un-watch); returns the prior set for Undo.
- `ComputeSpoilerWall` — pure: `(games: List<Game>, watched: Set<String>) → Instant?` (earliest played unwatched kickoff, or null).
- `SeedWatchedOnFirstSync` — pure helper used by the repository: `(games, firstSyncAt, window=3d) → Set<String>` of game ids to seed as watched.

**Removed**
- `MarkWatched` (bulk reveal-all) and its tests.

**Kept (re-purposed)**
- `RevealItem`, `ObserveRevealed`, `RestoreRevealed` — per-item peek + undo, now triggered by the wall instead of `isMatch`.

DI: bind the new interfaces in `DomainModule`; provide `ScheduleDataSource`/`ScheduleRepository` in the data DI modules; add the `games` table to the database module.

## 6. UI layer

### 6.1 Entry point — `ProtectionCard` (replaces `ProtectionBanner` + `CatchUpCard`)

Three states (from `wall.jsx`):
- **Off** → plain `.protban`: shield-off, "Off · showing everything", toggle. No action row.
- **On, games hiding** → master toggle on top; divider; tappable action row with a red lock chip, "**N unwatched game(s) is/are** hiding newer stories", "Manage ›". Subtitle "On · hiding news from games you haven't watched".
- **On, all caught up** → same card; subtitle "On · nothing to hide right now"; action row shows a green check, "**You're all caught up** — every game watched", "Fixtures ›".

The action row opens `MatchWallSheet`.

### 6.2 `MatchWallSheet` ("Fixtures & spoilers")

Material modal bottom sheet (reuses `.sheet.left` + scrim). Top → bottom:
1. Grip + header (title + close ✕).
2. **Subtitle** — when hiding: a mono count pill ("🔒 N UNWATCHED") + "game(s) is/are hiding newer stories…"; when caught up: "All caught up — nothing's hidden."
3. **Status bar** (always shown) — clock + "Updated {lastSyncedAt}" + **Refresh** pill. Offline → wifi-off, "No connection · Updated …", "Try again", plus the marking-still-works-offline note.
4. **Segmented control** — Results / Upcoming.
5. **Fixture list** — `FixtureRow`s.
6. **Footer** — "A score is only ever shown once its game is watched."

**`FixtureRow`** — crest (with H/A venue badge, abbreviation fallback) · opponent + meta (competition · Home/Away · date) · trailing state-dependent:
- **WATCHED** → `ScoreTrail`: score (en-dash) + W/D/L chip + "✓ Watched" + a round **Undo** button.
- **UNWATCHED (played)** → "Mark watched" pill + eye icon. **No score / no result hint of any kind.**
- **UPCOMING** → kickoff time + weekday. No score.

**Hide-all refinement:** because only the *earliest* unwatched game holds the wall, marking a later game first releases nothing. In the Results list, **mark the wall game** (oldest unwatched) with a subtle "next to catch up" accent so the user catches up chronologically.

### 6.3 Feed redaction + `FeedViewModel`

- `FeedViewModel` combines `observeFeed`, `observeProtection`, `observeRevealed`, `observeSchedule`, `observeWatchedGames`, filter prefs, transient state. It computes the wall via `ComputeSpoilerWall` and maps each item to `FeedCard.Normal` / `FeedCard.Redacted` using the §3 predicate. It exposes `hidingCount` (played-unwatched count), `protectionCard` state, and sheet open/close + handlers (`onMarkGameWatched`, `onUnmarkGame`, `onUndo`, `onRefreshSchedule`).
- Any hidden item → existing **`RedactedCard`** (no longer tied to `isMatch`).
- **`GateSheet`** copy updated to the date-based reason.
- **Mark watched** → optimistic; toast "**N stories unlocked**" + **Undo** (restores prior `watchedGameIds`).

### 6.4 State survival (persistence contract)

| State | Storage | Rotation | Process death |
|---|---|---|---|
| `watchedGameIds` | DataStore | survives | survives |
| `first/last_schedule_sync_at` | DataStore | survives | survives |
| fixtures | Room | survives | survives (offline) |
| `revealed` (per-item peek) | DataStore | survives | survives |
| sheet open / segment | in-memory VM | survives | resets |

## 7. Edge cases & error handling

- **Day-1 seed** — `SeedWatchedOnFirstSync` marks games with `kickoff < firstSyncAt − 3 days` watched; runs once (guarded by `first_schedule_sync_at`). Later syncs add new games (default unwatched); a newly-ingested game already older than the window is also auto-seeded watched.
- **Fail-open** — no fixtures yet (fresh install / no network) ⇒ `wall = null` ⇒ nothing hidden; sheet shows the offline/error state with "Try again". Once synced, Room serves it offline.
- **Offline** — cached fixtures shown; status bar escalates; marking still works (local).
- **Out-of-order marking** — marking a non-earliest game doesn't move the wall; the "next to catch up" accent (§6.2) steers chronological catch-up. Un-marking a game that becomes the new earliest unwatched snaps the wall back and re-redacts the feed.
- **All caught up** — list still shows every result with scores (no dead-end empty screen).
- **Refresh cadence** — `SyncSchedule` debounced on app open + manual Refresh; never blocks the feed.

## 8. The hard rule (centralized score gate)

A score / result word (W/D/L) / any derived result is rendered **only** for `WATCHED` games. In code this lives in one place — the score formatting + `ScoreTrail` composable, mounted only for watched rows — mirroring the handoff. Never put a score into the composition for an unwatched game, even hidden. Unit + screenshot tests assert unwatched rows contain no score.

## 9. Testing strategy

All three test types are supported (kotlinx.serialization applied; Roborazzi configured in `app/build.gradle.kts`; UIAutomator 2.4). Touches data/domain **and** Compose **and** an interaction flow → unit + screenshot + UI emulator.

- **Unit** (`/write-unit-tests` conventions — Truth, Turbine, fakes, `MainDispatcherRule`):
  - `ComputeSpoilerWall` — earliest played unwatched; null when caught up; ignores upcoming.
  - `SeedWatchedOnFirstSync` — 3-day window boundary; idempotent.
  - hide predicate / `FeedViewModel` — redaction follows the wall, `revealed` overrides, banner/`hidingCount`, sheet state.
  - `MarkGameWatched` toggle + Undo snapshot; un-mark snaps wall back.
  - W/D/L derivation (home/away); DTO→entity→domain mapping (date/time parse, opponent + H/A, score preserved).
  - `ScheduleRepository.refresh` — first-sync seed + timestamps; fail-open on error.
- **Screenshot (Roborazzi)** — `MatchWallSheet` (default / all-caught-up / loading / offline), `ProtectionCard` (off / hiding / caught-up), `FixtureRow` (watched / unwatched / upcoming), `RedactedCard`. Assert unwatched rows render no score.
- **UI emulator (UIAutomator)** — open the sheet from the protection card → mark the wall game watched → previously hidden stories appear in the feed; per-item Reveal-anyway gate still works.

Coverage closed with the `/write-unit-tests` Workflow (kover) after implementation.

## 10. Out of scope

- News→game linking / per-game story association (explicitly dropped — see §2.3).
- Notifications/alerts for fixtures; multi-team support (Liverpool only).
- A standalone Schedule screen/route (bottom sheet only).
- Server-side sync of watched state.
- Restoring the old `MarkWatched` bulk-reveal behavior.

## 11. Risks & open questions

- **TheSportsDB free-tier shape** — exact endpoints, team id, key, and JSON field names must be verified against live docs during implementation; crowdsourced data timeliness can wobble (acceptable: fail-open + cache).
- **Last-N/next-N window** — if the free window proves too narrow around congested fixture periods, revisit (premium full-season endpoint or bundled fallback) — designed behind `ScheduleDataSource` so it's a localized change.
- **3-day seed window** — a starting value; tune after dogfooding.
