# Feed Filtering — News-Type Tabs + Cumulative Reliability Ladder — Design

- **Date:** 2026-06-04
- **Status:** Agreed during brainstorming — precedes the implementation plan.
- **Branch:** `feat/feed-filtering-tabs-reliability`
- **Scope:** The **Feed screen's filter chrome only**. Lets the reader choose *what* they read (news type) and *how much they trust it* (reliability), with a pinned default feed, all persisted. Settings/Alerts screens from the handoff remain **out of scope** (not yet built in the app).

This is a design record. It precedes the implementation plan.

The handoff references live in [`design_handoff_a_little_bird/`](../../../design_handoff_a_little_bird/) — chiefly [`filters.jsx`](../../../design_handoff_a_little_bird/filters.jsx) (filter surfaces + logic) and the "Filtering & feeds" section of [`README.md`](../../../design_handoff_a_little_bird/README.md). They are reference-only; the task is to recreate them faithfully in Compose + Material 3, reusing the existing theme and components.

---

## 1. Background

Today the Feed shows every item — the only transformation is spoiler **redaction** of match content when protection is on (`FeedViewModel` builds `FeedUiState` via `combine(observeFeed, observeProtection, observeRevealed, transient).stateIn(...)`). There is no way to scope the feed.

The handoff adds **two orthogonal controls** directly below the app bar:

1. **News type** — a *single-select* feed switcher: **All · Transfers · Match · Squad & staff · Contracts · Academy**.
2. **Reliability** — a *single cumulative level* (NOT multi-select): level **N shows every tier from 1 down to N**. T1 = reliable only; T2 = +beat writers; T3 = +speculative. A global setting beside the type switcher.

The two are independent (switching feeds never changes the reliability level — there is intentionally no per-type memory). The user can **pin a default feed**. Filtering is **independent of spoiler protection**: a Match card in the Match (or All) feed still redacts when protection is on.

The domain already carries everything the filter needs: `NewsItem.tier: Tier` (3 levels) and `NewsItem.category: Category` (`TRANSFER/COACHING/CONTRACT/BACKROOM/ACADEMY/MATCH/OTHER`), classified by the data layer.

## 2. Decisions locked during brainstorming

| Decision | Choice | Why |
|---|---|---|
| Which surface to ship | **Tabs** | The handoff's recommended default; cleanest mapping to M3 tabs. (Chips / Feed-sheet were prototype explorations — not built.) |
| "Pin a default feed" | **In scope** | Cohesive with the design and small; completes the handoff. |
| Cold-launch landing feed | **Pinned feed** (or `All` if none pinned) | Makes "pin" meaningful. Reliability level still persists globally. The current type is **not** restored across process death — pinning is the deliberate way to set the home feed. |
| Architecture | Domain model + pure `FilterFeed` use case (**approach B**) | Matches the project's clean-architecture conventions; filter math is pure and unit-testable; one code path feeds both the visible list and the per-tab counts. |

## 3. State survival (the persistence contract)

| State | Storage | Config change (rotation) | Process death / cold launch |
|---|---|---|---|
| **Reliability level** (`maxTier`) | DataStore | survives | survives |
| **Pinned feed** | DataStore | survives | survives |
| **Selected type** (current tab) | in-memory `MutableStateFlow` in VM | survives | **re-seeds to the pinned feed** |

On process death the selected type does not vanish to a neutral default — the VM's `init` re-seeds it from the pinned feed (or `ALL`). That *is* the "pinned wins on cold launch" decision. We deliberately do **not** use `SavedStateHandle` for the selected type — preserving it across process death would make pinning meaningless. Transient modal state (gate / catch-up sheets) is unchanged: in-memory, survives rotation, resets on process death.

## 4. Domain model (new)

### `domain/model/FeedType.kt` — pure enum, no display copy

```kotlin
enum class FeedType(val key: String, val categories: Set<Category>?) {
    ALL("all", null),                                  // null = every category
    TRANSFERS("transfer", setOf(Category.TRANSFER)),
    MATCH("match", setOf(Category.MATCH)),
    SQUAD_STAFF("staff", setOf(Category.COACHING, Category.BACKROOM)),
    CONTRACTS("contract", setOf(Category.CONTRACT)),
    ACADEMY("academy", setOf(Category.ACADEMY));

    fun matches(category: Category) = categories == null || category in categories
    companion object { fun fromKey(key: String?) = entries.find { it.key == key } ?: ALL }
}
```

`Category.OTHER` belongs to no named type, so it appears only in the `ALL` feed — acceptable.

### `domain/model/FeedFilter.kt` — selection + result

```kotlin
data class FeedFilter(val type: FeedType, val maxTier: Tier)
data class FilteredFeed(val visible: List<NewsItem>, val counts: Map<FeedType, Int>)
```

Cumulative semantics reuse the existing `Tier` ordinal (`TIER_1_RELIABLE=0 … TIER_3_SPECULATIVE=2`): an item is included when `item.tier.ordinal <= maxTier.ordinal`. Default `maxTier = TIER_3_SPECULATIVE` (show everything → preserves today's behavior).

### `domain/usecase/FilterFeed.kt` — stateless, dependency-free, single pass

```kotlin
fun interface FilterFeed { operator fun invoke(items: List<NewsItem>, filter: FeedFilter): FilteredFeed }
```

- `visible` = items filtered by **both** the tier ceiling and the selected type.
- `counts` = for **every** `FeedType`, the number of items at the current tier ceiling matching that type — independent of which type is selected; drives the per-tab badges, which update live as the ladder changes.

Bound in `DomainModule` following the existing use-case pattern (interface + `…UseCase` impl with `@Inject` constructor).

## 5. Persistence (extend the existing DataStore layer)

Only two things persist; the current type is session-only.

### `PreferencesDataSource` — two new keys

```kotlin
private val reliabilityLevelKey = intPreferencesKey("reliability_level")   // 1..3
private val pinnedFeedKey = stringPreferencesKey("pinned_feed")            // FeedType.key; absent = none

val reliabilityLevel: Flow<Int> = dataStore.data.map { it[reliabilityLevelKey] ?: 3 }  // default 3 = all tiers
val pinnedFeed: Flow<String?>   = dataStore.data.map { it[pinnedFeedKey] }
suspend fun setReliabilityLevel(level: Int) { dataStore.edit { it[reliabilityLevelKey] = level } }
suspend fun setPinnedFeed(key: String?) {
    dataStore.edit { if (key == null) it.remove(pinnedFeedKey) else it[pinnedFeedKey] = key }
}
```

### `PreferencesRepository` — stays primitive-typed

The repo keeps its current primitive style (`Boolean` / `Set<String>`); the `Int↔Tier` and `String?↔FeedType` mapping lives in domain use cases (the DataModel→domain mapping rule). New interface methods: `observeReliabilityLevel(): Flow<Int>`, `observePinnedFeed(): Flow<String?>`, `setReliabilityLevel(Int)`, `setPinnedFeed(String?)`.

### Three thin filter-prefs use cases (matching `ObserveProtection`/`ToggleProtection` style)

- **`ObserveFilterPrefs`** → `Flow<FilterPrefs(maxTier: Tier, pinned: FeedType?)>` — combines + maps the two pref flows. `level 1→TIER_1`, `2→TIER_2`, `3→TIER_3`; `pinned = FeedType.fromKey` (null when absent).
- **`SetReliabilityLevel`** → persists the level (used by the ladder **and** the empty-state reset).
- **`SetPinnedFeed`** → persists / clears the pinned feed key.

## 6. ViewModel wiring (`FeedViewModel`)

- Inject the 3 new prefs use cases + `FilterFeed` alongside the existing 8.
- **Session state:** `selectedType = MutableStateFlow<FeedType>`, seeded in `init` from `ObserveFilterPrefs().first().pinned ?: FeedType.ALL` → this is "cold launch opens on pinned." Survives rotation; re-seeds on process death.
- **Combine:** group `selectedType` + `ObserveFilterPrefs()` into a `filterState` flow, then
  `combine(observeFeed, observeProtection, observeRevealed, filterState, transient)`:
  1. `FilterFeed(items, FeedFilter(selectedType, maxTier))` → `FilteredFeed`.
  2. map `FilteredFeed.visible` → `FeedCard.Normal/Redacted` (existing protection + revealed logic, **unchanged**).
  3. `heldMatchCount` stays **global** (counted from the unfiltered `items`) → the catch-up affordance is consistent across tabs and `onMarkWatched` is unaffected (it already derives held ids from `observeFeed().first()`).
  4. `isEmpty` = unfiltered feed empty (existing "Nothing yet" / offline states). New `isFilteredEmpty` = unfiltered non-empty but `visible` empty.
- **New handlers:** `onSelectType(FeedType)` (in-memory), `onSetReliability(Int)`, `onTogglePin()` (pin `selectedType`, or unpin if already pinned), `onResetReliability()` (→ level 3).

### `FeedUiState` additions

```kotlin
val filterTabs: List<FeedTab> = emptyList(),   // FeedTab(type: FeedType, count: Int)
val selectedType: FeedType = FeedType.ALL,
val reliabilityLevel: Tier = Tier.TIER_3_SPECULATIVE,
val pinnedType: FeedType? = null,
val isFilteredEmpty: Boolean = false,
```

UI derives per-tab `selected` (`type == selectedType`) and `pinned` (`type == pinnedType`).

## 7. UI components (Compose, Material 3) — new package `ui/feed/filter/`

All are dumb renderers: state down, lambdas up. The filter chrome is **sticky**, directly under `TopBar`, above the scroll:

```
TopBar
FeedTypeTabs                                       ← row 1: type switcher
Row { ReliabilityLadder · spacer · PinButton }     ← row 2: trust + pin
PullToRefreshBox { ProtectionBanner, CatchUpCard, cards / FilterEmpty }
```

- **`FeedTypeTabs.kt`** — horizontally-scrollable tab strip (M3 `ScrollableTabRow` or a custom scrolling `Row` matching the prototype's underline style). Per tab: short label (resolved here) + count badge; active tab = `primary` underline; `count == 0` → muted (`onSurfaceDim`); small filled-pin glyph when `type == pinnedType`. Tap → `onSelectType(type)`.
- **`ReliabilityLadder.kt`** — the connected 3-segment cumulative control (T1·T2·T3). One joined shape, 1px outline, radius 12 (Sharp corners per tokens), hairline dividers, no gaps. Segments at/below the chosen level are "included" (`surfaceContainerHighest` bg, full-opacity pip); above are excluded (transparent, pip at 0.35). The chosen-level segment gets a 3dp inset bottom accent in `primary` (the "down to here" marker). Pip colours reuse the tier palette already used by `TierChip` (T1 green / T2 blue / T3 amber). Tap segment N → `onSetReliability(N)`. `radiogroup` semantics for accessibility.
- **`PinButton.kt`** — pin toggle; filled (`primary`) when `selectedType == pinnedType`, ghost otherwise. Tap → `onTogglePin()`.
- **`FilterEmpty.kt`** — the new filtered-empty state: info tile, "Nothing here right now", "No stories in this feed at your current reliability level. Try a lower tier or another feed.", tonal **"Show all tiers"** button → `onResetReliability()`.
- **`FeedTypeLabels.kt`** (UI helper) — maps `FeedType` → display `label`/`short` (e.g. `SQUAD_STAFF → "Squad & staff"`), keeping the domain enum copy-free. Literal strings, matching the existing Composables' style (string resources noted as a future option).

`FeedScreen`/`FeedContent` integration: render the two chrome rows above `PullToRefreshBox`; add an `isFilteredEmpty → FilterEmpty(...)` branch to the existing `when`. `ProtectionBanner` and `CatchUpCard` stay inside the scroll as today.

## 8. Behaviour contract

- Type and reliability are **independent** — changing one never resets the other.
- **No per-type tier memory** — reliability is one global level.
- Filtering is **orthogonal to spoiler protection** — a Match card in the Match/All feed still redacts when protection is on; tapping it still opens the gate.
- **Held count is global** — counted from the unfiltered feed, so catch-up is consistent on any tab.
- **"Show all tiers"** resets the *level* to 3 only (not the type), per the handoff.

## 9. Testing strategy

Both project capability gates pass: Roborazzi is configured; UIAutomator is `2.4.0-beta02`.

- **Unit** (`/write-unit-tests`):
  - `FilterFeedTest` — cumulative tier ceiling, type membership, counts per type at a given level, `OTHER` only-in-`ALL`, empty result.
  - `PreferencesDataSource` / `PreferencesRepository` — new keys: defaults (level 3, pinned absent) + round-trip + clear.
  - The 3 prefs use cases — `Int↔Tier`, `String?↔FeedType`, pin toggle/clear.
  - `FeedViewModelTest` additions — select type filters cards; set level changes cards + counts; pin toggle; `isFilteredEmpty`; **launch seeds from pinned**; live counts; held count stays global.
  - Close gaps with kover.
- **Screenshot** (`/write-screenshot-tests`, Roborazzi): `FeedTypeTabs` (selected / muted / pinned), `ReliabilityLadder` (levels 1·2·3), `FilterEmpty`, and `FeedScreen` with chrome. Record baselines.
- **UI emulator** (`/write-ui-tests`, UIAutomator): the interaction flow — tap a tab filters the list; tap a ladder segment updates counts + list; pin a feed shows the glyph; empty-state "Show all tiers" recovers the list.

## 10. Out of scope

- Settings & Alerts screens (incl. the Alerts "source tier" radios) — not built in the app yet.
- Chips and Feed-sheet surfaces — prototype explorations only.
- Any change to RSS ingestion, classification, or tier resolution — the filter reads the already-classified `NewsItem` fields.

## 11. Touched / new files

**New:** `domain/model/FeedType.kt`, `domain/model/FeedFilter.kt`, `domain/usecase/FilterFeed.kt`, `domain/usecase/ObserveFilterPrefs.kt`, `domain/usecase/SetReliabilityLevel.kt`, `domain/usecase/SetPinnedFeed.kt`, `ui/feed/filter/FeedTypeTabs.kt`, `ui/feed/filter/ReliabilityLadder.kt`, `ui/feed/filter/PinButton.kt`, `ui/feed/filter/FilterEmpty.kt`, `ui/feed/filter/FeedTypeLabels.kt`.

**Modified:** `data/preferences/datasource/PreferencesDataSource.kt`, `data/preferences/PreferencesRepository.kt`, `di/DomainModule.kt` (bind new use cases), `ui/feed/FeedViewModel.kt`, `ui/feed/FeedUiState.kt`, `ui/feed/FeedScreen.kt`.
