# A Little Bird — Phase 1 Design

**Spoiler-safe Liverpool FC transfer & backroom news feed (Android).**

Date: 2026-06-01
Status: Approved for planning
Module: `:app` (package `dev.crushingdigits.littlebird`)

---

## 1. Context & scope

**A Little Bird** aggregates Liverpool FC transfer and coaching/backroom news from
multiple RSS sources into one feed. Its defining feature is **spoiler protection**:
a user who watches matches on delay can switch protection on so that match-related
content (scores, results, live updates, player ratings, match reports, previews) is
hidden, while transfer and coaching news keeps flowing.

This document specifies **Phase 1 only**. The product is larger than Phase 1 (see
§11) and is being built in phases; each later phase gets its own spec → plan →
implementation cycle. The design reference is the high-fidelity prototype in
`design_handoff_a_little_bird/` (HTML/React); Phase 1 recreates the **Feed** surface
of that prototype natively.

### Phase 1 delivers

- The **data layer**: RSS/Atom fetch → parse → classify → tier → cache (Room).
- The **match classifier** (the riskiest logic), built test-first against real-world
  headline fixtures.
- The **Feed screen** with full **spoiler mode**: protection toggle, inline redaction
  of match items, the reveal gate, and the catch-up ("mark as watched") flow with undo.
- Manual + pull-to-refresh; offline support via the Room cache.
- Screenshot (Roborazzi) and UI emulator (UIAutomator 2.4+) test infrastructure.

### Phase 1 explicitly excludes (deferred — see §11)

In-app Detail reader, corroboration card, Settings tab (granular spoiler-type toggles,
blur-vs-quarantine handling, schedule windows), Alerts subscreen, push notifications,
periodic WorkManager background refresh, article navigation of any kind.

---

## 2. Decisions log (resolved during brainstorming)

| # | Decision | Resolution |
|---|---|---|
| 1 | Build all-at-once vs phased | **Phased.** Spec & build Phase 1 now. |
| 2 | Classifier action on match content | **Flag & redact, never drop.** Every item is tagged `isMatch` + `Category`. Protection ON redacts match items inline (blur → gate → reveal); OFF shows everything. High recall on *detecting* match content remains the top priority. |
| 3 | Tap target in Phase 1 | **No navigation yet.** Match cards open the gate; revealing un-blurs in place; catch-up releases held items. Normal cards are inert until the Detail phase. |
| 4 | Feed freshness | **Manual + pull-to-refresh.** Room serves cached feed offline. Periodic WorkManager refresh deferred to the Notifications phase. |
| 5 | UI test depth | **Set up Roborazzi screenshot + UIAutomator 2.4+ emulator tests in Phase 1**, alongside full TDD unit coverage on the logic. |
| 6 | RSS parsing | **Hand-rolled `XmlPullParser`** behind a `FeedParser` interface (zero dependency, fully fixture-testable, handles RSS 2.0 + Atom). |
| 7 | Cross-cutting defaults | **Hilt** (DI), **single `:app` module** with packages **structured by data type**, **OkHttp** (raw XML GET), **Room** (cache SSOT) + **DataStore** (prefs), **Coil** (images), **Compose + Material 3**, single Activity, no Navigation library yet. |
| 8 | Data model layering | Collapsed to three honest hops (`RawFeedItem` → `NewsItemEntity` → `NewsItem`); the full Common-repo `Dto→Entity→DataModel→Domain` chain is unnecessary for a standalone app with no separate SDK consumer. |

---

## 3. Architecture & package layout

Single `:app` module. Three layers, dependencies flowing inward (`ui → domain → data`).
Data layer is **structured by data type** (`news`, `preferences`); each type owns its
repository and its data sources.

```
dev.crushingdigits.littlebird
├─ data/
│  ├─ news/                          ← the "news" data type
│  │  ├─ NewsRepository              single entry point for news data
│  │  ├─ datasource/
│  │  │  ├─ remote/  NewsRemoteDataSource · FeedFetcher (OkHttp) · FeedParser (XmlPullParser) · RawFeedItem
│  │  │  └─ local/   NewsLocalDataSource · NewsDao · NewsItemEntity · LittleBirdDatabase
│  │  ├─ classifier/ MatchClassifier · Categoriser · ClassifierConfig
│  │  ├─ tier/       TierResolver · TierConfig
│  │  ├─ source/     FeedSource · FeedSources
│  │  └─ mapper/     Raw→Entity, Entity→domain mappers
│  └─ preferences/                   ← the "spoiler prefs" data type
│     ├─ PreferencesRepository       single entry point for protectionOn + revealedIds
│     └─ datasource/  PreferencesDataSource (DataStore)
├─ domain/
│  ├─ model/         NewsItem · Tier · Category · Classification
│  └─ usecase/       ObserveFeed · RefreshFeed · ObserveProtection ·
│                    ToggleProtection · RevealItem · MarkWatched
├─ ui/
│  ├─ feed/          FeedViewModel · FeedScreen · FeedUiState · FeedAction
│  ├─ components/    NewsCard · RedactedCard · TierChip · ProtectionBanner ·
│  │                 CatchUpCard · TopBar · GateSheet · CatchUpSheet · Toast · BottomNav
│  └─ theme/         Color · Type · Theme
├─ di/               Hilt modules (NetworkModule, DatabaseModule, DataStoreModule, bindings)
└─ MainActivity · LittleBirdApp(@HiltAndroidApp)
```

### Data flow (one item's journey)

```
NewsRepository.refresh()
  → for each FeedSource: NewsRemoteDataSource.fetch(source)
        FeedFetcher (OkHttp GET) → raw XML String
        FeedParser (XmlPullParser, RSS 2.0 | Atom) → List<RawFeedItem>
  → MatchClassifier + Categoriser + TierResolver → Classification per item
  → mapper: RawFeedItem + Classification → NewsItemEntity
  → NewsLocalDataSource.upsert(entities)   (dedup by stable id)

NewsRepository.observeFeed()
  → NewsLocalDataSource.observeAll(): Flow<List<NewsItemEntity>>
  → mapper → Flow<List<NewsItem>>

FeedViewModel
  → combine(observeFeed, observeProtection, observeRevealed) → FeedUiState
  → Compose renders (Normal | Redacted cards + catch-up + sheets + toast)
```

Room is the **single source of truth**: the UI always renders from Room, so the
cached feed is shown whenever the network is unavailable. A failed refresh leaves the
cache intact and surfaces an offline indicator.

---

## 4. Data model

| Stage | Type | Fields |
|---|---|---|
| Parse (network) | `RawFeedItem` | `sourceId, title, link, description, pubDate: String?, author: String?, categories: List<String>, imageUrl: String?, guid: String?` |
| Disk | `NewsItemEntity` | `id (PK), sourceId, sourceName, tier, category, isMatch, confirmed, author, role, title, summary, link, imageUrl, publishedAtEpochMs` |
| Domain / UI | `NewsItem` | `id, source, tier: Tier, category: Category, isMatch, confirmed, author, role, title, summary, link, imageUrl, publishedAt: Instant` |

**Stable id:** derived deterministically from `guid` when present, else from `link`
(fallback: hash of `sourceId + title + pubDate`). Used for Room PK and for the
`revealedIds` set, so reveal state survives refresh and process death.

**Enums:**

```kotlin
enum class Tier(val label: String) {          // colour tokens live in the theme
    TIER_1_RELIABLE("RELIABLE"),              // green  #5fd6a0
    TIER_2_BEAT("BEAT"),                      // blue   #8fb8ff
    TIER_3_SPECULATIVE("SPECULATIVE"),        // amber  #f3b24e
}

enum class Category { TRANSFER, COACHING, CONTRACT, BACKROOM, ACADEMY, MATCH, OTHER }
```

**Persistence split:**

- **Room** → cached `NewsItemEntity` rows (offline source of truth).
- **DataStore (Preferences)** → `protectionOn: Boolean` (default `true`),
  `revealedIds: Set<String>`. An item is "revealed" (via gate) or "released" (via
  catch-up) when its id is in `revealedIds`.

---

## 5. Feed sources (config — easy to extend)

A `FeedSource` data class drives ingestion; `FeedSources` is the editable list. Adding
a feed later is a one-line addition.

```kotlin
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val defaultTier: Tier,
    val kind: FeedKind = FeedKind.RSS,   // RSS | ATOM (parser also auto-detects)
)
```

Phase 1 seed list (exact paths **verified at build time** — see §10):

| id | name | url | default tier |
|---|---|---|---|
| `this_is_anfield` | This Is Anfield | `https://www.thisisanfield.com/feed/` | TIER_2_BEAT |
| `tia_transfers` | This Is Anfield — Transfers | `https://www.thisisanfield.com/liverpool-fc-transfer-rumours/feed/` | TIER_2_BEAT |
| `liverpool_echo` | Liverpool Echo | LFC section URL + `?service=rss` (Reach platform) | TIER_2_BEAT |
| `bbc_liverpool` | BBC Sport — Liverpool | `https://feeds.bbci.co.uk/sport/football/teams/liverpool/rss.xml` | TIER_2_BEAT |

`defaultTier` is the floor; the `TierResolver` may bump an individual item up (e.g.
an Ornstein byline) or down (known tabloid) — see §7.

---

## 6. The match classifier (riskiest logic — built test-first)

The classifier runs on a normalized text = `title + " " + description`, where
normalization = lowercase, strip HTML tags, collapse whitespace, and normalize
en-dash/em-dash (`–`/`—`) to `-` (so the scoreline regex is reliable).

All keyword/phrase/journalist lists are **data** (config objects), never hardcoded
`if/else`, so they are trivially tunable against fixtures.

### Pipeline (strict order)

1. **Match detection → `isMatch: Boolean` (EXCLUDE; overrides everything).**
   Bias is **recall-first**: a leaked scoreline is catastrophic; an over-blurred
   transfer card is a minor annoyance. When in doubt, flag as match. Signals (data):
   - phrase/keyword list: `full-time, full time, half-time, ft, ht, kick-off,
     kickoff, line-ups, lineup, lineups, xi, player ratings, ratings, match report,
     as it happened, live, final score, scoreline, draw, win, won, lose, loss, lost,
     defeat, beat, goal, goals, penalty, red card, sent off, preview, "watch:",
     highlights, full-time report, vs, v` (the bare `v`/`vs` matched only on word
     boundaries).
   - scoreline regex: `\b\d+\s*[-–—]\s*\d+\b`.
   - **Tuning hook (data, default empty):** a transfer-context allowlist of phrases
     (e.g. `"win the race", "win the signature", "win race for"`) that may suppress a
     *weak* exclude hit. Disabled by default — Phase 1 ships pure recall-first and
     prunes false positives only if fixtures demand it.
2. **Category → `Category` (INCLUDE intent).** If `isMatch` → `MATCH`. Otherwise scan
   an ordered, priority-ranked `Map<Category, List<String>>`; first category with a
   hit wins; no hit → `OTHER` (still shown, generic chip):
   - **TRANSFER**: transfer, sign, signing, signed, deal, fee, bid, medical, agree,
     agreed, loan, target, linked, swoop, "here we go", release clause.
   - **COACHING**: appoint, appointed, appoints, sacked, new manager, new coach,
     assistant, sporting director, head coach.
   - **CONTRACT**: contract, new deal, extension, renew, terms.
   - **BACKROOM**: backroom, head of fitness, set-piece coach, staff reshuffle,
     sports science, recruitment.
   - **ACADEMY**: academy, u18, u21, u23, youth, graduate, prospect.
3. **Tier + confirmed** — delegated to the `TierResolver` (§7).

**Output:** `Classification(isMatch: Boolean, category: Category, tier: Tier, confirmed: Boolean)`.

### TDD requirement

`MatchClassifierTest` is written **first**, against a labeled corpus of real-world LFC
headlines (match vs non-match), stored as test fixtures. Acceptance:
**100% recall on the match set** (zero leaks) is the gate; precision is measured and
reported, and tuned via the allowlist only as needed. Parser and resolver follow the
same fixtures-first discipline.

---

## 7. Tier resolution

`TierResolver.resolve(source: FeedSource, author: String?, text: String): Pair<Tier, Boolean>`
(the `Boolean` is `confirmed`).

1. Start from `source.defaultTier`.
2. **Journalist / source overrides (data):**
   - If `author`/`text` names a **Tier-1 journalist** (`David Ornstein`, `Fabrizio
     Romano`, …) → `TIER_1_RELIABLE`.
   - If the source/author is a **known aggregator/tabloid** (`Daily Mirror`, `The
     Sun`, …) → `TIER_3_SPECULATIVE`.
   - Truly unknown source → default `TIER_3_SPECULATIVE` (per brief).
   This reproduces the prototype's nuance (e.g. The Athletic is T1 under Ornstein but
   T2 for generic coverage).
3. **confirmed** = `tier == TIER_1_RELIABLE && text contains "here we go"`. Drives the
   green "✓ Here we go" pill.

All name/source sets are config data.

---

## 8. Spoiler engine & state (UDF)

`FeedViewModel` exposes an immutable `FeedUiState` via `stateIn(WhileSubscribed(5000))`,
built by `combine(observeFeed(), observeProtection(), observeRevealed())`.

### Per-item rendering

| `protectionOn` | `isMatch` | id ∈ `revealedIds` | renders as |
|---|---|---|---|
| off | — | — | **Normal** |
| on | no | — | **Normal** |
| on | yes | no | **Redacted** |
| on | yes | yes | **Normal** (revealed in place) |

`heldMatchCount` = number of redacted items. The **Catch-up card** shows when
`protectionOn && heldMatchCount ≥ 1`.

### State shape

```kotlin
data class FeedUiState(
    val cards: List<FeedCard> = emptyList(),     // FeedCard = Normal(NewsItem) | Redacted(NewsItem)
    val protectionOn: Boolean = true,
    val heldMatchCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val isEmpty: Boolean = false,
    val gateTarget: NewsItem? = null,            // non-null → GateSheet shown
    val catchUpVisible: Boolean = false,         // true → CatchUpSheet shown
)
// One-off effects (toast w/ optional Undo) via SharedFlow<FeedAction>, not in UiState.
```

### Events (ViewModel functions / `FeedAction`)

- `toggleProtection()` → flips `protectionOn` in DataStore. Bound to the app-bar shield
  **and** the banner switch (one source of truth).
- `openItem(item)` → if `protectionOn && item.isMatch && id ∉ revealedIds` → set
  `gateTarget`; else no-op (no navigation in Phase 1).
- `revealItem(id)` → add id to `revealedIds`; clear `gateTarget`.
- `requestCatchUp()` / `dismissCatchUp()` → toggle `catchUpVisible`.
- `markWatched()` → snapshot current `revealedIds`, add **all** held match ids, clear
  `catchUpVisible`, emit `Toast("N match updates unlocked", action = Undo(snapshot))`.
- `undo(snapshot)` → restore `revealedIds` to the snapshot.

### Persistent vs one-off

Per project rules: modal state that must survive rotation (`gateTarget`,
`catchUpVisible`) lives in `FeedUiState`; fire-and-forget effects (toast) are emitted
via `SharedFlow` so they don't re-trigger on recomposition/rotation. `protectionOn` and
`revealedIds` persist in DataStore.

### Offline & empty

UI always renders from Room. `refresh()` returns a `Result`; failure keeps the cache
and sets `isOffline = true` (offline indicator + toast). `isEmpty = true` when Room has
no rows and no successful fetch yet → empty state.

---

## 9. UI (faithful to the handoff)

Single Activity → Compose root in `LittleBirdApp`. **Dark Material 3** base theme using
the handoff tokens. A two-tab **BottomNav** (Feed / Settings) is rendered for visual
fidelity; **Settings is a Phase-1 placeholder** ("Coming soon"). Feed is the live
surface; sheets and toast are state-driven (no Navigation library).

**`FeedScreen` composition** (maps 1:1 to prototype components):
`TopBar` (emblem + wordmark + "LFC TRANSFERS & BACKROOM" sublabel + shield toggle +
overflow placeholder) → `ProtectionBanner` (tinted when on; M3 switch) → `CatchUpCard`
(conditional) → `PullToRefreshBox` { `LazyColumn` of `NewsCard` | `RedactedCard` } →
`GateSheet` / `CatchUpSheet` (`ModalBottomSheet`) → `Toast` (snackbar with Undo).

- **`NewsCard`**: rounded card (radius 22, 1px outline), optional Coil image header
  (only when the feed supplies `media:content`/`enclosure`), tier chip + source +
  relative time, headline, footer chips (category + green "✓ Here we go" when
  confirmed).
- **`RedactedCard`**: dashed red-tinted card, blurred placeholder bars behind a
  centered lock overlay ("Match content hidden · {source} · tap to reveal").
- **`TierChip`**: colour-coded pip + `T{n} {LABEL}` (green/blue/amber).
- **`GateSheet`**: "This one might spoil you", amber risk tag, **Reveal anyway** /
  **Keep it hidden**.
- **`CatchUpSheet`**: "All caught up?", **Yes, reveal them** / **Not yet**.

### Theme tokens (dark, base)

surface `#1d1b20`, surface-container `#232027`, surface-container-high `#2a2731`,
on-surface `#ece5ee`, on-surface-variant `#b4adbc`, outline `rgba(236,229,238,.13)`,
**primary `#ff4d5e`**, tier-1 `#5fd6a0`, tier-2 `#8fb8ff`, tier-3 `#f3b24e`,
confirmed/green `#5fd6a0`. Radius: cards 22, sheets 28, chips 7–8, buttons pill.
Switch 52×32 (thumb 16→24). Tap targets ≥ 44dp.

### Typography

Archivo (display/body, 400–800) + Space Mono (metadata/tiers/timestamps, uppercased,
letter-spacing ~0.04–0.1em). Fonts **bundled** (or Compose downloadable fonts). Sizes
per handoff: card headline 18sp/700, screen header 30sp/800, tier/meta mono 8.5–11sp.

---

## 10. Testing strategy

Test selection follows the project's Android test-selection rule: data/domain → unit;
Compose visuals → screenshot; interaction flows → UI emulator. Phase 1 touches all
three, and sets up the infra for the latter two.

- **Unit (TDD, fixtures first):**
  - `MatchClassifier` — labeled headline corpus; **gate: 100% recall on match set**.
  - `Categoriser`, `TierResolver` (+ confirmed).
  - `FeedParser` — RSS 2.0 + Atom + **captured real-feed XML fixtures** (This Is
    Anfield/WordPress, Reach/Echo, BBC) covering CDATA, namespaces, date formats.
  - `NewsRepository` — dedup, mapping, refresh-failure keeps cache.
  - `FeedViewModel` — state derivation table (§8), events. Turbine + fakes for data
    sources, `MainDispatcherRule`, Truth assertions, fakes over mocks.
  - Kover gap-closing pass at the end.
- **Screenshot (Roborazzi — set up in Phase 1):** `NewsCard` (each tier; image/no-image;
  with/without Here-we-go), `RedactedCard`, `CatchUpCard`, `ProtectionBanner` on/off,
  empty + offline states, `GateSheet`, `CatchUpSheet`, `Toast`.
- **UI emulator (UIAutomator 2.4+ — set up in Phase 1):** the signature flow —
  toggle protection → match cards redact → tap redacted → gate → **Reveal anyway** →
  card un-blurs; catch-up → **Mark as watched** → all release → **Undo** restores.

Test utilities (fakes/samples) follow the project conventions; reusable fakes for the
data sources and a `NewsItemSample` factory.

---

## 11. Out of scope (deferred to later phases)

Each becomes its own spec → plan cycle:

- **Detail phase:** in-app reader (headline + RSS summary + tier byline + "Open
  original" Custom Tab), corroboration card (clustering similar stories across feeds
  into "N sources").
- **Settings phase:** master toggle, per-type spoiler checklist (scores, live,
  ratings, reports, previews, lineups), blur-vs-**quarantine** handling, matchday
  schedule windows.
- **Alerts / Notifications phase:** notification categories, source-tier gating, quiet
  hours, **periodic WorkManager background refresh**, spoiler-safe held-notification
  delivery (release on "mark as watched").

---

## 12. Build-time verification & open items

- **Verify feed URLs** resolve and return valid XML at build time; confirm the exact
  Liverpool Echo LFC section URL for the `?service=rss` suffix. Capture a sample of
  each real feed's XML as parser test fixtures.
- **Fonts:** bundle Archivo + Space Mono (or wire Compose downloadable fonts); confirm
  licensing.
- **Assets:** app emblem and press photos are placeholders in the prototype — Phase 1
  uses placeholder/Coil; real Liverbird crest art and licensed imagery are out of scope.
- **Dependencies to add** to the version catalog (exact versions pinned at
  implementation time, cross-checked via Context7 against AGP 9.2.1 / Kotlin):
  Compose BOM + Material 3, Hilt, Room (+ KSP), OkHttp, kotlinx-coroutines, DataStore
  Preferences, Coil-Compose, Roborazzi, AndroidX Test UIAutomator 2.4+, Truth, Turbine,
  Robolectric. Existing AppCompat/Material-Views scaffold dependencies are removed.
- Project config retained: `minSdk 29`, `targetSdk 36`, `compileSdk 36`, Gradle Kotlin
  DSL + version catalog.
