# A Little Bird — Phase 2 Design: The Detail Reader

**In-app reader for a single news item, reached from the Feed.**

Date: 2026-06-02
Status: Approved for planning
Module: `:app` (package `dev.crushingdigits.littlebird`)
Builds on: Phase 1 (`2026-06-01-little-bird-phase-1-design.md`)

---

## 1. Context & scope

Phase 1 delivered the data layer, the match classifier, and the **Feed** surface with
full spoiler protection (redact → gate → reveal, catch-up with undo). Tapping a card
did not navigate anywhere — decision #3 deferred navigation "until the Detail phase."

**This is the Detail phase.** It adds a full-screen **Detail reader** reachable from the
feed, faithful to the handoff's `DetailScreen` (`design_handoff_a_little_bird/screens.jsx`
lines 175–233), backed **honestly by the data RSS actually gives us** (headline + short
summary), with a working native **Share** action and an **"Open original"** Chrome Custom
Tab to the full article. Navigation is introduced via **Jetpack Navigation 3**, laying the
back-stack foundation that the future Settings/Alerts phases will build on.

### Phase 2 delivers

- A **toolchain upgrade** to Kotlin **2.3.21** (with KSP + Compose compiler in lockstep) —
  a prerequisite verified green before feature work (§3).
- **Jetpack Navigation 3** wired into the app: a hoisted back stack with `FeedKey` and
  `DetailKey(itemId)` destinations (§4).
- The **Detail reader** (`ui/detail/`): top bar (back + Share), tier/time/confirmed meta,
  headline, optional press photo, byline, the RSS summary as lead text, **"Open original"**
  Custom Tab, and a native **Share** button (§5).
- The **gate → reveal → detail** flow: revealing a match item from the gate now un-blurs
  it in the feed **and** slides into the reader, faithful to the prototype (§6).
- Screenshot (Roborazzi) + UI emulator (UIAutomator) coverage extended to the reader (§8).

### Phase 2 explicitly excludes (deferred — see §10)

Corroboration card (cross-feed story clustering), Save / reading-list, Mute / source
management, Settings screen, Alerts subscreen, full-article text extraction.

---

## 2. Decisions log (resolved during brainstorming)

| # | Decision | Resolution |
|---|----------|------------|
| 1 | What backs the reader body | **RSS summary + "Open original".** Render headline, tier byline, and the RSS `summary` as lead text; a primary **Open original** button launches a Chrome Custom Tab to `item.link`. Honest about the data we have — no fabricated article prose, no fragile full-text extraction. Matches Phase-1 spec §11. |
| 2 | Corroboration card | **Deferred to its own phase.** Cross-feed clustering ("N sources reporting this") is a separate hard problem deserving its own spec → plan. The corroboration block does not render. |
| 3 | Reader action bar (Save / Share / Mute) | **Share only.** Share fires a native `ACTION_SEND` (headline + link) and stands on its own. Save (reading list) and Mute (source management) are half-features without the later phases that give them a home, so they are dropped — no dead buttons. The detail top bar keeps Back + Share, drops the bookmark icon. |
| 4 | Navigation mechanism | **Jetpack Navigation 3.** Compose-native, idiomatic, lays real back-stack infrastructure for the coming Settings/Alerts destinations. Supersedes Phase-1 decision #7's "no Navigation library yet." |
| 5 | Kotlin version | **Upgrade to 2.3.21** (from 2.1.21), with KSP and the Compose-compiler plugin moved in lockstep. Treated as a prerequisite task verified green before Detail work (§3). |
| 6 | Gate → reveal behaviour | **Reveal now navigates to Detail.** Faithful to the prototype's `reveal()` (HTML lines 72–78): "Reveal anyway" adds the id to `revealedIds`, closes the gate, **and** opens the reader. Evolves Phase-1 decision #3 ("un-blur in place"), which deferred this to the Detail phase. |

---

## 3. Toolchain prerequisite — Kotlin 2.3.21 upgrade

**This lands first and must build green before any Detail code.** The catalog comment in
`gradle/libs.versions.toml` is the key constraint: *AGP 9 applies the Kotlin Gradle plugin
itself*; our `kotlin` version feeds the Compose-compiler plugin (`kotlin-compose`, which
uses `version.ref = "kotlin"`) and anchors KSP (which must share Kotlin's `major.minor`).

### Cascade

| Catalog key | Before | After | Notes |
|-------------|--------|-------|-------|
| `kotlin` | `2.1.21` | `2.3.21` | Drives the `kotlin-compose` plugin and is the KSP anchor. |
| `ksp` | `2.1.21-2.0.2` | `2.3.21-<match>` | KSP is versioned `<kotlin>-<ksp>`; exact suffix pinned via Context7. |
| `agp` | `9.2.1` | TBD at verify time | **Risk:** if AGP 9.2.1 does not bundle/accept Kotlin 2.3.21, this bump pulls an AGP move (or an explicit Kotlin-plugin override) with it. Verify first. |
| `composeBom` | `2026.05.01` | confirm compat | Confirm the BOM is compatible with the 2.3.21 Compose compiler; bump only if required. |

### Verification gate (do this before writing Detail code)

1. Confirm, via Context7 + the AGP–Kotlin compatibility matrix, whether AGP 9.2.1 carries
   Kotlin 2.3.21. If not, resolve the coupling (AGP bump or explicit Kotlin plugin version).
2. Pin the exact matching **KSP** release (`2.3.21-x.y.z`) via Context7.
3. Confirm **Compose BOM** + Hilt + Room (KSP-based) all compile under the new toolchain.
4. **Gate:** the *existing Phase 1 build + full test suite* (unit + Roborazzi + compile of
   the UIAutomator harness) passes unchanged under 2.3.21 before Detail work begins.

---

## 4. Architecture & navigation (Jetpack Navigation 3)

Single Activity, Compose root. Introduce a hoisted **`NavBackStack`** of **`NavKey`s** and a
root **`NavDisplay`** (Nav3). Exact API surface and the Nav3 dependency versions are verified
against the `navigation-3` skill + Context7 at implementation time.

```
dev.crushingdigits.littlebird
├─ ui/
│  ├─ navigation/   AppNavHost (NavDisplay + NavBackStack) · NavKeys (FeedKey, DetailKey)
│  ├─ feed/         (existing) FeedViewModel · FeedScreen · FeedUiState · FeedAction (+NavigateToDetail)
│  └─ detail/       DetailViewModel · DetailScreen · DetailUiState
├─ domain/usecase/  ObserveNewsItem  (new)
└─ data/news/datasource/local/  NewsDao.observeById(id)  (new query)
```

### Destinations & back stack

- **`FeedKey`** → the home scaffold (existing TopBar + BottomNav + Feed; Settings tab stays a
  Phase-1 placeholder). This is the fixed start destination and bottom of the stack.
- **`DetailKey(itemId: String)`** → the full-screen reader, **covering the bottom nav**
  exactly as the prototype's overlay does.
- Keys carry **only the item id** (a small, serializable value) — survives process death and
  is idiomatic Nav3. The reader re-observes the item by id; we never serialize a whole
  `NewsItem` (with `Instant`/enums) into a key.
- `open` → `backStack.add(DetailKey(id))`; `back` → `backStack.removeLastOrNull()`.

### Detail data path

- **`ObserveNewsItem`** (`fun interface ObserveNewsItem { operator fun invoke(id): Flow<NewsItem?> }`,
  impl `ObserveNewsItemUseCase`) backed by a new `NewsDao.observeById(id): Flow<NewsItemEntity?>`
  mapped to domain — keeps the reader reactive to refresh and never stale.
- **`DetailViewModel`** (Hilt) receives the `itemId` from the `DetailKey` — via
  `SavedStateHandle` or an entry-scoped creation extra, whichever the Nav3 + Hilt
  integration prescribes (confirmed at implementation time) — and exposes `DetailUiState`
  via `stateIn(WhileSubscribed(5000))`.

```kotlin
internal data class DetailUiState(
    val item: NewsItem? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,   // resolved + null (e.g. cache evicted)
)
```

Share and Open-original are **UI-layer side effects** (they need `Context`), so the
`DetailScreen` performs them via lambdas — the ViewModel only supplies the item. No
`DetailAction`/`SharedFlow` is needed this phase.

---

## 5. The Detail screen (faithful to handoff)

`DetailScreen` maps 1:1 to the prototype's `DetailScreen`, minus the deferred pieces.

- **Top bar (`detail-bar`):** back button (pops the stack) + **Share** icon. *(Bookmark icon
  dropped per decision #3.)*
- **Body (scrollable):**
  - **Meta row:** `TierChip` (reused from feed) + "{relativeTime} ago" + conditional
    "✓ Here we go" pill when `confirmed`.
  - **Headline** (display type).
  - **Press photo:** optional Coil image, rendered **only when `item.imageUrl != null`**
    (rounded 18, 1px outline), matching the feed's image-gating rule.
  - **Byline (`detail-by`):** avatar with author **initials** (first two words of `author`,
    uppercased — works for person *or* outlet names, e.g. "BBC Sport" → "BS"), then
    `author` name and "{role} · {tier label}".
  - **Lead text:** the RSS `summary` rendered as the reader body (decision #1). If `summary`
    is blank, show a quiet "Summary unavailable — open the original to read more."
  - **Primary actions:** **"Open original"** → Chrome Custom Tab to `item.link`; **Share**
    button → native `ACTION_SEND` (headline + link). *(Mute dropped per decision #3.)*
- Reuses the existing relative-time formatter and `TierChip`; theme tokens unchanged
  (dark Material 3, Phase-1 handoff tokens).

---

## 6. Behaviour: tap → gate → reveal → detail

The signature flow gains a destination. `FeedViewModel` keeps `gateTarget`/`catchUp` state;
navigation is a one-off effect the `FeedScreen` translates into a back-stack push.

- **`FeedAction.NavigateToDetail(itemId: String)`** — new one-off action (emitted via the
  existing `SharedFlow<FeedAction>`; the `FeedScreen` collects it and calls
  `backStack.add(DetailKey(itemId))`).
- **`openItem(item)`** (updated):
  - if `protectionOn && item.isMatch && id ∉ revealedIds` → set `gateTarget` (gate opens), **else** → emit `NavigateToDetail(item.id)`.
- **`revealItem(id)`** (updated): add id to `revealedIds`, clear `gateTarget`, **then** emit
  `NavigateToDetail(id)` — un-blurs in the feed *and* opens the reader (prototype `reveal()`).
- **"Keep it hidden"** → clears `gateTarget` only; no navigation.
- Catch-up / mark-watched / undo are unchanged from Phase 1.

The reader's content is **independent of protection state** — the gate guards *entry*, not
the reader body.

---

## 7. Edge & error handling

- **`notFound`** (id resolves to null — e.g. row evicted by a refresh): the reader shows a
  small empty/error state with a back affordance, never a crash.
- **No `link`** (RSS guid-only items): "Open original" is hidden/disabled.
- **No Custom Tab provider** (no browser supporting it): fall back to a plain `ACTION_VIEW`
  intent; if even that resolves to nothing, surface a toast.
- **Blank `summary`:** the "open the original" fallback line (see §5).
- **Process death** in the reader: `DetailKey` (id) is restored by Nav3; `DetailViewModel`
  re-resolves the item from Room — content returns intact.

---

## 8. Testing strategy

Per the project's Android test-selection rule: data/domain → unit; Compose visuals →
screenshot; interaction flows → UI emulator. This phase touches all three.

- **Unit (TDD, fakes first):**
  - `ObserveNewsItem` + `NewsDao.observeById` — returns mapped item; emits `null` for a
    missing id; reacts to upserts.
  - `DetailViewModel` — state table: loading → item resolved; resolved-null → `notFound`;
    reacts to upstream changes. Turbine + fakes + `MainDispatcherRule`, Truth, fakes over mocks.
  - `FeedViewModel` (updated) — asserts `NavigateToDetail` is emitted for a non-gated tap,
    is **not** emitted for a gated tap, and **is** emitted after `revealItem` from the gate.
  - Kover gap-closing pass.
- **Screenshot (Roborazzi):** `DetailScreen` variants — each tier; image / no-image;
  confirmed / not-confirmed; blank-summary fallback; `notFound` state. Captured with the
  existing dark-theme `captureThemed` helper.
- **UI emulator (UIAutomator 2.4+):** extend the signature flow —
  tap a **normal** card → reader opens → **back** returns to feed;
  toggle protection → tap a **redacted** card → gate → **Reveal anyway** → card un-blurs
  **and** reader opens; **Open original** launches an external browser (assert via package
  watcher / intent, not external content).

Reusable fakes (`ObserveNewsItemFake`) and the existing `NewsItemSample` factory follow
project conventions.

---

## 9. Build-time verification & open items

- **Toolchain (blocking, §3):** resolve the AGP↔Kotlin-2.3.21 coupling; pin exact KSP;
  confirm Compose BOM/Hilt/Room compile; existing suite green before Detail work.
- **Nav3 deps & API:** add Navigation 3 artifacts; pin versions and confirm the
  `NavDisplay`/`NavBackStack`/`NavKey` API via the `navigation-3` skill + Context7 against
  Kotlin 2.3.21 / AGP / Compose BOM.
- **Custom Tabs:** add `androidx.browser:browser`; verify version via Context7. Confirm the
  fallback `ACTION_VIEW` path on a device with no Custom Tab provider.
- **Fonts/assets:** unchanged from Phase 1 (Archivo + Space Mono bundling TODO carries over;
  press photos remain Coil placeholders).
- Project config retained: `minSdk 29`, `targetSdk 36`, `compileSdk 36`, Gradle Kotlin DSL +
  version catalog.

---

## 10. Out of scope (deferred to later phases)

Each becomes its own spec → plan cycle:

- **Corroboration phase:** cross-feed clustering of similar stories into "N sources" with
  per-source tier chips (the reader's corroboration card).
- **Settings phase:** master toggle, per-type spoiler checklist, blur-vs-quarantine,
  matchday schedule windows; gives **Mute** a home.
- **Reading-list phase:** persisted saved items + a surface to view them; gives **Save** a home.
- **Alerts / Notifications phase:** notification categories, source-tier gating, quiet hours,
  periodic WorkManager refresh, spoiler-safe held-notification delivery.
- **Full-article extraction:** Readability-style in-app full text (vs the current
  summary + Open-original), if ever desired.
