# Feed Filtering — News-Type Tabs + Reliability Ladder — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Repo git-workflow (LittleBird):** commit with **raw `git`** (descriptive message + `Co-Authored-By: Claude Opus 4.8 (1M context)` trailer). **Never** `/submit-mr`; do not push or open a PR unless explicitly asked. Execution is **subagent-driven** (no execution-mode question).

**Goal:** Add a Feed-screen filter chrome — single-select news-type Tabs + a cumulative reliability ladder (T1·T2·T3) + a pinned default feed — all persisted, independent of spoiler protection.

**Architecture:** Approach B from the spec. A pure `FeedType` domain enum + a dependency-free `FilterFeed` use case produce both the visible list and per-tab counts in one pass. The reliability level and pinned feed persist in DataStore (extended `PreferencesDataSource`/`Repository`, read via thin use cases); the selected type is session-only and re-seeds from the pin on cold launch. `FeedViewModel` wires the flows; new dumb Composables render the chrome.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Hilt, DataStore, Coroutines/Flow. Tests: JUnit4 + Truth + Turbine (`MainDispatcherRule`), Roborazzi screenshots (`src/test/screenshots`), UIAutomator 2.4 (`uiAutomator{}` DSL).

**Spec:** [docs/superpowers/specs/2026-06-04-feed-filtering-design.md](../specs/2026-06-04-feed-filtering-design.md)

---

## File map

**New (main):**
- `domain/model/FeedType.kt` — news-type enum (key + category set)
- `domain/model/FeedFilter.kt` — `FeedFilter` selection + `FilteredFeed` result
- `domain/usecase/FilterFeed.kt` — pure filter + counts use case
- `domain/usecase/ObserveFilterPrefs.kt` — `FilterPrefs` + observe (Int↔Tier, String↔FeedType mapping)
- `domain/usecase/SetReliabilityLevel.kt`
- `domain/usecase/SetPinnedFeed.kt`
- `ui/feed/filter/FeedTypeLabels.kt` — `FeedType.shortLabel()`
- `ui/feed/filter/ReliabilityLadder.kt`
- `ui/feed/filter/FeedTypeTabs.kt`
- `ui/feed/filter/PinButton.kt`
- `ui/feed/filter/FilterEmpty.kt`

**Modified (main):**
- `data/preferences/datasource/PreferencesDataSource.kt` — two keys
- `data/preferences/PreferencesRepository.kt` — interface + impl
- `di/DomainModule.kt` — bind 4 new use cases
- `ui/feed/FeedUiState.kt` — `FeedTab` + new fields
- `ui/feed/FeedViewModel.kt` — inject + combine + handlers
- `ui/feed/FeedScreen.kt` — render chrome + filtered-empty branch

**New/modified (test):**
- `test/.../domain/model/FeedTypeTest.kt` (new)
- `test/.../domain/usecase/FilterFeedTest.kt` (new)
- `test/.../domain/usecase/FilterPrefsUseCasesTest.kt` (new)
- `test/.../data/preferences/PreferencesRepositoryTest.kt` (extend)
- `test/.../ui/feed/FeedViewModelTest.kt` (extend — fake + factory + tests)
- `test/.../ui/FilterComponentsScreenshotTest.kt` (new)
- `androidTest/.../FakeDataModule.kt` (extend fake prefs)
- `androidTest/.../FilterFlowUiTest.kt` (new)

---

## Task 1: `FeedType` domain enum

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/model/FeedType.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/model/FeedTypeTest.kt`

- [ ] **Step 1: Write the failing test**

Create `FeedTypeTest.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FeedTypeTest {
    @Test fun `ALL matches every category`() {
        Category.entries.forEach { assertThat(FeedType.ALL.matches(it)).isTrue() }
    }

    @Test fun `SQUAD_STAFF matches coaching and backroom only`() {
        assertThat(FeedType.SQUAD_STAFF.matches(Category.COACHING)).isTrue()
        assertThat(FeedType.SQUAD_STAFF.matches(Category.BACKROOM)).isTrue()
        assertThat(FeedType.SQUAD_STAFF.matches(Category.TRANSFER)).isFalse()
    }

    @Test fun `fromKey resolves known keys and falls back to ALL`() {
        assertThat(FeedType.fromKey("transfer")).isEqualTo(FeedType.TRANSFERS)
        assertThat(FeedType.fromKey("academy")).isEqualTo(FeedType.ACADEMY)
        assertThat(FeedType.fromKey(null)).isEqualTo(FeedType.ALL)
        assertThat(FeedType.fromKey("nope")).isEqualTo(FeedType.ALL)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedTypeTest"`
Expected: FAIL — compilation error, `FeedType` unresolved.

- [ ] **Step 3: Write the implementation**

Create `FeedType.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.model

/** A single-select news-type feed. [categories] = null means "every category" (the All feed). */
enum class FeedType(val key: String, val categories: Set<Category>?) {
    ALL("all", null),
    TRANSFERS("transfer", setOf(Category.TRANSFER)),
    MATCH("match", setOf(Category.MATCH)),
    SQUAD_STAFF("staff", setOf(Category.COACHING, Category.BACKROOM)),
    CONTRACTS("contract", setOf(Category.CONTRACT)),
    ACADEMY("academy", setOf(Category.ACADEMY));

    fun matches(category: Category): Boolean = categories == null || category in categories

    companion object {
        fun fromKey(key: String?): FeedType = entries.find { it.key == key } ?: ALL
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedTypeTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/model/FeedType.kt \
        app/src/test/java/dev/crushingdigits/littlebird/domain/model/FeedTypeTest.kt
git commit -m "feat: add FeedType news-type enum

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: `FeedFilter`/`FilteredFeed` + `FilterFeed` use case

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/model/FeedFilter.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/FilterFeed.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/FilterFeedTest.kt`

- [ ] **Step 1: Write the failing test**

Create `FilterFeedTest.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import java.time.Instant

class FilterFeedTest {
    private val subject = FilterFeedUseCase()

    private fun item(id: String, tier: Tier, category: Category) = NewsItem(
        id = id, source = "s", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private val feed = listOf(
        item("t1", Tier.TIER_1_RELIABLE, Category.TRANSFER),
        item("m2", Tier.TIER_2_BEAT, Category.MATCH),
        item("a3", Tier.TIER_3_SPECULATIVE, Category.ACADEMY),
        item("c1", Tier.TIER_1_RELIABLE, Category.COACHING),
    )

    @Test fun `tier ceiling is cumulative`() {
        val r = subject(feed, FeedFilter(FeedType.ALL, Tier.TIER_2_BEAT))
        assertThat(r.visible.map { it.id }).containsExactly("t1", "m2", "c1") // T3 academy excluded
    }

    @Test fun `type filter selects only matching categories`() {
        val r = subject(feed, FeedFilter(FeedType.SQUAD_STAFF, Tier.TIER_3_SPECULATIVE))
        assertThat(r.visible.map { it.id }).containsExactly("c1")
    }

    @Test fun `counts are per type at the current tier ceiling`() {
        val r = subject(feed, FeedFilter(FeedType.ALL, Tier.TIER_1_RELIABLE))
        assertThat(r.counts[FeedType.ALL]).isEqualTo(2)       // t1 + c1
        assertThat(r.counts[FeedType.TRANSFERS]).isEqualTo(1) // t1
        assertThat(r.counts[FeedType.MATCH]).isEqualTo(0)     // m2 is T2, excluded
        assertThat(r.counts[FeedType.ACADEMY]).isEqualTo(0)   // a3 is T3, excluded
    }

    @Test fun `empty when no item matches`() {
        val r = subject(feed, FeedFilter(FeedType.MATCH, Tier.TIER_1_RELIABLE))
        assertThat(r.visible).isEmpty()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*FilterFeedTest"`
Expected: FAIL — `FeedFilter`, `FilterFeedUseCase` unresolved.

- [ ] **Step 3: Write the models**

Create `FeedFilter.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.model

/** The active filter selection: which feed [type] and the cumulative reliability ceiling [maxTier]. */
data class FeedFilter(val type: FeedType, val maxTier: Tier)

/** Result of applying a [FeedFilter]: the [visible] items plus per-type [counts] at the current ceiling. */
data class FilteredFeed(val visible: List<NewsItem>, val counts: Map<FeedType, Int>)
```

- [ ] **Step 4: Write the use case**

Create `FilterFeed.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.FilteredFeed
import dev.crushingdigits.littlebird.domain.model.NewsItem
import javax.inject.Inject

fun interface FilterFeed {
    operator fun invoke(items: List<NewsItem>, filter: FeedFilter): FilteredFeed
}

class FilterFeedUseCase @Inject constructor() : FilterFeed {
    override fun invoke(items: List<NewsItem>, filter: FeedFilter): FilteredFeed {
        // Cumulative reliability ceiling: tier ordinal 0 (RELIABLE) .. 2 (SPECULATIVE).
        val withinTier = items.filter { it.tier.ordinal <= filter.maxTier.ordinal }
        val visible = withinTier.filter { filter.type.matches(it.category) }
        val counts = FeedType.entries.associateWith { type ->
            withinTier.count { type.matches(it.category) }
        }
        return FilteredFeed(visible = visible, counts = counts)
    }
}
```

- [ ] **Step 5: Bind in `DomainModule`**

In `DomainModule.kt`, add the import and the `@Binds` line:
```kotlin
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
```
```kotlin
    @Binds abstract fun filterFeed(impl: FilterFeedUseCase): FilterFeed
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*FilterFeedTest"`
Expected: PASS (4 tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/model/FeedFilter.kt \
        app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/FilterFeed.kt \
        app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt \
        app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/FilterFeedTest.kt
git commit -m "feat: add FilterFeed use case (visible list + per-type counts)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Persist reliability level + pinned feed

Widens the `PreferencesRepository` interface, so **all three implementors** are updated in this task to keep the build green: the real impl, the unit-test `FakePrefs` (in `FeedViewModelTest`), and the androidTest `FakePreferencesRepository`.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/preferences/datasource/PreferencesDataSource.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/preferences/PreferencesRepository.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt` (FakePrefs only)
- Modify: `app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt` (FakePreferencesRepository only)
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/preferences/PreferencesRepositoryTest.kt` (extend)

- [ ] **Step 1: Write the failing tests**

Append to `PreferencesRepositoryTest.kt` (inside the class):
```kotlin
    @Test fun `reliability level defaults to 3 and persists`() = runTest {
        assertThat(subject.observeReliabilityLevel().first()).isEqualTo(3)
        subject.setReliabilityLevel(1)
        assertThat(subject.observeReliabilityLevel().first()).isEqualTo(1)
    }

    @Test fun `pinned feed is absent by default, sets and clears`() = runTest {
        assertThat(subject.observePinnedFeed().first()).isNull()
        subject.setPinnedFeed("transfer")
        assertThat(subject.observePinnedFeed().first()).isEqualTo("transfer")
        subject.setPinnedFeed(null)
        assertThat(subject.observePinnedFeed().first()).isNull()
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*PreferencesRepositoryTest"`
Expected: FAIL — `observeReliabilityLevel`/`setPinnedFeed` unresolved (and `FakePrefs` will not yet compile — fixed in Step 5).

- [ ] **Step 3: Extend `PreferencesDataSource`**

In `PreferencesDataSource.kt`, add imports and members:
```kotlin
import androidx.datastore.preferences.core.intPreferencesKey
```
Inside the class, alongside the existing keys/flows:
```kotlin
    private val reliabilityLevelKey = intPreferencesKey("reliability_level")
    private val pinnedFeedKey = stringPreferencesKey("pinned_feed")

    val reliabilityLevel: Flow<Int> = dataStore.data.map { it[reliabilityLevelKey] ?: 3 } // default 3 = all tiers
    val pinnedFeed: Flow<String?> = dataStore.data.map { it[pinnedFeedKey] }

    suspend fun setReliabilityLevel(level: Int) { dataStore.edit { it[reliabilityLevelKey] = level } }
    suspend fun setPinnedFeed(key: String?) {
        dataStore.edit { if (key == null) it.remove(pinnedFeedKey) else it[pinnedFeedKey] = key }
    }
```
(`stringPreferencesKey`, `edit`, `map` are already imported.)

- [ ] **Step 4: Extend `PreferencesRepository` interface + impl**

In `PreferencesRepository.kt`, add to the interface:
```kotlin
    fun observeReliabilityLevel(): Flow<Int>
    fun observePinnedFeed(): Flow<String?>
    suspend fun setReliabilityLevel(level: Int)
    suspend fun setPinnedFeed(key: String?)
```
And to `PreferencesRepositoryImpl`:
```kotlin
    override fun observeReliabilityLevel(): Flow<Int> = source.reliabilityLevel
    override fun observePinnedFeed(): Flow<String?> = source.pinnedFeed
    override suspend fun setReliabilityLevel(level: Int) = source.setReliabilityLevel(level)
    override suspend fun setPinnedFeed(key: String?) = source.setPinnedFeed(key)
```

- [ ] **Step 5: Keep the unit-test fake compiling**

In `FeedViewModelTest.kt`, change `FakePrefs` to add the constructor params, backing flows, and the four overrides:
```kotlin
    private class FakePrefs(on: Boolean, pinned: String? = null, level: Int = 3) : PreferencesRepository {
        val protection = MutableStateFlow(on)
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        val reliabilityLevel = MutableStateFlow(level)
        val pinnedFeed = MutableStateFlow(pinned)
        override fun observeProtection() = protection
        override fun observeRevealed() = revealed
        override suspend fun setProtection(on: Boolean) { protection.value = on }
        override suspend fun toggleProtection() { protection.value = !protection.value }
        override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
        override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
        override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
        override fun observeReliabilityLevel() = reliabilityLevel
        override fun observePinnedFeed() = pinnedFeed
        override suspend fun setReliabilityLevel(level: Int) { reliabilityLevel.value = level }
        override suspend fun setPinnedFeed(key: String?) { pinnedFeed.value = key }
    }
```

- [ ] **Step 6: Keep the androidTest fake compiling**

In `FakeDataModule.kt`, inside `FakePreferencesRepository`, add:
```kotlin
    private val reliabilityLevel = MutableStateFlow(3)
    private val pinnedFeed = MutableStateFlow<String?>(null)
    override fun observeReliabilityLevel(): Flow<Int> = reliabilityLevel
    override fun observePinnedFeed(): Flow<String?> = pinnedFeed
    override suspend fun setReliabilityLevel(level: Int) { reliabilityLevel.value = level }
    override suspend fun setPinnedFeed(key: String?) { pinnedFeed.value = key }
```
(`MutableStateFlow` and `Flow` are already imported.)

- [ ] **Step 7: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*PreferencesRepositoryTest"`
Expected: PASS (6 tests: 4 existing + 2 new).

- [ ] **Step 8: Confirm the whole unit module still compiles**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — every existing test still passes (fakes compile, behavior unchanged).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/preferences/ \
        app/src/test/java/dev/crushingdigits/littlebird/data/preferences/PreferencesRepositoryTest.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt \
        app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt
git commit -m "feat: persist reliability level + pinned feed in DataStore

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Filter-prefs use cases (`ObserveFilterPrefs`, `SetReliabilityLevel`, `SetPinnedFeed`)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveFilterPrefs.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SetReliabilityLevel.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SetPinnedFeed.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/FilterPrefsUseCasesTest.kt`

- [ ] **Step 1: Write the failing test**

Create `FilterPrefsUseCasesTest.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

private class FakePrefs : PreferencesRepository {
    val level = MutableStateFlow(3)
    val pinned = MutableStateFlow<String?>(null)
    override fun observeProtection(): Flow<Boolean> = MutableStateFlow(true)
    override fun observeRevealed(): Flow<Set<String>> = MutableStateFlow(emptySet())
    override suspend fun setProtection(on: Boolean) {}
    override suspend fun toggleProtection() {}
    override suspend fun reveal(id: String) {}
    override suspend fun revealAll(ids: Set<String>) {}
    override suspend fun restoreRevealed(ids: Set<String>) {}
    override fun observeReliabilityLevel(): Flow<Int> = level
    override fun observePinnedFeed(): Flow<String?> = pinned
    override suspend fun setReliabilityLevel(level: Int) { this.level.value = level }
    override suspend fun setPinnedFeed(key: String?) { pinned.value = key }
}

class FilterPrefsUseCasesTest {
    @Test fun `ObserveFilterPrefs maps stored ints and keys to domain types`() = runTest {
        val prefs = FakePrefs()
        val sut = ObserveFilterPrefsUseCase(prefs)
        assertThat(sut().first()).isEqualTo(FilterPrefs(Tier.TIER_3_SPECULATIVE, null))
        prefs.level.value = 1
        prefs.pinned.value = "match"
        assertThat(sut().first()).isEqualTo(FilterPrefs(Tier.TIER_1_RELIABLE, FeedType.MATCH))
    }

    @Test fun `SetReliabilityLevel stores the tier as a 1-based level`() = runTest {
        val prefs = FakePrefs()
        SetReliabilityLevelUseCase(prefs)(Tier.TIER_1_RELIABLE)
        assertThat(prefs.level.value).isEqualTo(1)
        SetReliabilityLevelUseCase(prefs)(Tier.TIER_3_SPECULATIVE)
        assertThat(prefs.level.value).isEqualTo(3)
    }

    @Test fun `SetPinnedFeed stores the key and clears on null`() = runTest {
        val prefs = FakePrefs()
        SetPinnedFeedUseCase(prefs)(FeedType.ACADEMY)
        assertThat(prefs.pinned.value).isEqualTo("academy")
        SetPinnedFeedUseCase(prefs)(null)
        assertThat(prefs.pinned.value).isNull()
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsUseCasesTest"`
Expected: FAIL — `ObserveFilterPrefsUseCase`, `FilterPrefs`, etc. unresolved.

- [ ] **Step 3: Write `ObserveFilterPrefs`**

Create `ObserveFilterPrefs.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Persisted filter prefs mapped to domain types: the reliability ceiling and the pinned feed (null = none). */
data class FilterPrefs(val maxTier: Tier, val pinned: FeedType?)

fun interface ObserveFilterPrefs { operator fun invoke(): Flow<FilterPrefs> }

class ObserveFilterPrefsUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : ObserveFilterPrefs {
    override fun invoke(): Flow<FilterPrefs> = combine(
        prefs.observeReliabilityLevel(),
        prefs.observePinnedFeed(),
    ) { level, pinnedKey ->
        FilterPrefs(
            maxTier = Tier.entries[(level - 1).coerceIn(0, Tier.entries.lastIndex)],
            pinned = pinnedKey?.let { FeedType.fromKey(it) },
        )
    }
}
```

- [ ] **Step 4: Write `SetReliabilityLevel`**

Create `SetReliabilityLevel.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.Tier
import javax.inject.Inject

fun interface SetReliabilityLevel { suspend operator fun invoke(maxTier: Tier) }

class SetReliabilityLevelUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : SetReliabilityLevel {
    override suspend fun invoke(maxTier: Tier) = prefs.setReliabilityLevel(maxTier.ordinal + 1)
}
```

- [ ] **Step 5: Write `SetPinnedFeed`**

Create `SetPinnedFeed.kt`:
```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import javax.inject.Inject

fun interface SetPinnedFeed { suspend operator fun invoke(type: FeedType?) }

class SetPinnedFeedUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : SetPinnedFeed {
    override suspend fun invoke(type: FeedType?) = prefs.setPinnedFeed(type?.key)
}
```

- [ ] **Step 6: Bind in `DomainModule`**

In `DomainModule.kt` add imports and `@Binds`:
```kotlin
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
```
```kotlin
    @Binds abstract fun observeFilterPrefs(impl: ObserveFilterPrefsUseCase): ObserveFilterPrefs
    @Binds abstract fun setReliabilityLevel(impl: SetReliabilityLevelUseCase): SetReliabilityLevel
    @Binds abstract fun setPinnedFeed(impl: SetPinnedFeedUseCase): SetPinnedFeed
```

- [ ] **Step 7: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*FilterPrefsUseCasesTest"`
Expected: PASS (3 tests).

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveFilterPrefs.kt \
        app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SetReliabilityLevel.kt \
        app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SetPinnedFeed.kt \
        app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt \
        app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/FilterPrefsUseCasesTest.kt
git commit -m "feat: add filter-prefs use cases (observe/set reliability + pinned feed)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Extend `FeedUiState` with filter fields + `FeedTab`

Pure data change — adding fields with defaults keeps `FeedViewModel` (which doesn't set them yet) compiling.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedUiState.kt`

- [ ] **Step 1: Add `FeedTab` + new fields**

Replace the body of `FeedUiState.kt` with:
```kotlin
package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier

/** One news-type tab: its [type] and the live item [count] at the current reliability level. */
data class FeedTab(val type: FeedType, val count: Int)

/** Immutable snapshot of the Feed screen. Modal state (gate/catch-up) lives here so it
 *  survives configuration changes; one-off effects (toast) flow via [FeedAction]. */
data class FeedUiState(
    val cards: List<FeedCard> = emptyList(),
    val protectionOn: Boolean = true,
    val heldMatchCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val isEmpty: Boolean = false,
    val gateTarget: NewsItem? = null,
    val catchUpVisible: Boolean = false,
    // Filter chrome
    val filterTabs: List<FeedTab> = emptyList(),
    val selectedType: FeedType = FeedType.ALL,
    val reliabilityLevel: Tier = Tier.TIER_3_SPECULATIVE,
    val pinnedType: FeedType? = null,
    val isFilteredEmpty: Boolean = false,
)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedUiState.kt
git commit -m "feat: add filter fields + FeedTab to FeedUiState

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Wire filtering into `FeedViewModel`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt` (factory + tests)

- [ ] **Step 1: Write the failing tests**

In `FeedViewModelTest.kt`, add a category/tier item helper next to the existing `item(id, match)`:
```kotlin
    private fun item(id: String, category: Category, tier: Tier = Tier.TIER_1_RELIABLE) = NewsItem(
        id = id, source = "BBC", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t$id", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )
```
Add these tests inside the class:
```kotlin
    @Test fun `selecting a type filters the visible cards`() = runTest {
        val news = FakeNews(listOf(item("t", Category.TRANSFER), item("m", Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t")
            assertThat(s.selectedType).isEqualTo(FeedType.TRANSFERS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `lowering reliability hides higher-tier items and updates counts`() = runTest {
        val news = FakeNews(
            listOf(
                item("t1", Category.TRANSFER, Tier.TIER_1_RELIABLE),
                item("t3", Category.TRANSFER, Tier.TIER_3_SPECULATIVE),
            ),
        )
        val sut = vm(news, FakePrefs(on = false))
        sut.onSetReliability(Tier.TIER_1_RELIABLE)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t1")
            assertThat(s.filterTabs.first { it.type == FeedType.ALL }.count).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggling pin pins then unpins the current feed`() = runTest {
        val prefs = FakePrefs(on = false)
        val sut = vm(FakeNews(listOf(item("t", Category.TRANSFER))), prefs)
        sut.onSelectType(FeedType.TRANSFERS)
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isEqualTo("transfer")
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isNull()
    }

    @Test fun `isFilteredEmpty is set when the feed has items but none match`() = runTest {
        val sut = vm(FakeNews(listOf(item("t", Category.TRANSFER))), FakePrefs(on = false))
        sut.onSelectType(FeedType.MATCH)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards).isEmpty()
            assertThat(s.isFilteredEmpty).isTrue()
            assertThat(s.isEmpty).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `cold launch seeds the selected type from the pinned feed`() = runTest {
        val news = FakeNews(listOf(item("t", Category.TRANSFER), item("m", Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false, pinned = "match"))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.selectedType).isEqualTo(FeedType.MATCH)
            assertThat(s.cards.map { it.item.id }).containsExactly("m")
            cancelAndIgnoreRemainingEvents()
        }
    }
```
Add the needed imports at the top of the test file:
```kotlin
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
```

- [ ] **Step 2: Update the `vm()` factory**

Replace the `vm()` factory in `FeedViewModelTest.kt` with:
```kotlin
    private fun vm(news: FakeNews, prefs: FakePrefs) = FeedViewModel(
        observeFeed = ObserveFeedUseCase(news),
        observeProtection = ObserveProtectionUseCase(prefs),
        observeRevealed = ObserveRevealedUseCase(prefs),
        refreshFeed = RefreshFeedUseCase(news),
        toggleProtection = ToggleProtectionUseCase(prefs),
        revealItem = RevealItemUseCase(prefs),
        markWatched = MarkWatchedUseCase(prefs),
        restoreRevealed = RestoreRevealedUseCase(prefs),
        filterFeed = FilterFeedUseCase(),
        observeFilterPrefs = ObserveFilterPrefsUseCase(prefs),
        setReliabilityLevel = SetReliabilityLevelUseCase(prefs),
        setPinnedFeed = SetPinnedFeedUseCase(prefs),
    )
```

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedViewModelTest"`
Expected: FAIL — `FeedViewModel` has no such constructor params / `onSelectType` unresolved.

- [ ] **Step 4: Rewrite `FeedViewModel`**

Replace `FeedViewModel.kt` with:
```kotlin
package dev.crushingdigits.littlebird.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.MarkWatched
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RestoreRevealed
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeFeed: ObserveFeed,
    private val observeProtection: ObserveProtection,
    private val observeRevealed: ObserveRevealed,
    private val refreshFeed: RefreshFeed,
    private val toggleProtection: ToggleProtection,
    private val revealItem: RevealItem,
    private val markWatched: MarkWatched,
    private val restoreRevealed: RestoreRevealed,
    private val filterFeed: FilterFeed,
    private val observeFilterPrefs: ObserveFilterPrefs,
    private val setReliabilityLevel: SetReliabilityLevel,
    private val setPinnedFeed: SetPinnedFeed,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val offline = MutableStateFlow(false)
    private val gateTarget = MutableStateFlow<NewsItem?>(null)
    private val catchUpVisible = MutableStateFlow(false)

    // Session-only selected feed; re-seeded from the pinned feed on cold launch.
    private val selectedType = MutableStateFlow(FeedType.ALL)

    private val _actions = MutableSharedFlow<FeedAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<FeedAction> = _actions.asSharedFlow()

    init {
        viewModelScope.launch {
            selectedType.value = observeFilterPrefs().first().pinned ?: FeedType.ALL
        }
    }

    private data class Transient(val refreshing: Boolean, val offline: Boolean, val gate: NewsItem?, val catchUp: Boolean)
    private val transient = combine(refreshing, offline, gateTarget, catchUpVisible, ::Transient)

    private data class FilterState(val type: FeedType, val maxTier: Tier, val pinned: FeedType?)
    private val filterState = combine(selectedType, observeFilterPrefs()) { type, prefs ->
        FilterState(type, prefs.maxTier, prefs.pinned)
    }

    val uiState: StateFlow<FeedUiState> = combine(
        observeFeed(), observeProtection(), observeRevealed(), filterState, transient,
    ) { items, protectionOn, revealed, fs, t ->
        val filtered = filterFeed(items, FeedFilter(fs.type, fs.maxTier))
        val cards = filtered.visible.map { item ->
            if (protectionOn && item.isMatch && item.id !in revealed) FeedCard.Redacted(item) else FeedCard.Normal(item)
        }
        // Held count is GLOBAL (whole feed), so catch-up is consistent across tabs.
        val held = items.count { protectionOn && it.isMatch && it.id !in revealed }
        FeedUiState(
            cards = cards,
            protectionOn = protectionOn,
            heldMatchCount = held,
            isRefreshing = t.refreshing,
            isOffline = t.offline,
            isEmpty = items.isEmpty() && !t.refreshing,
            gateTarget = t.gate,
            catchUpVisible = t.catchUp,
            filterTabs = FeedType.entries.map { FeedTab(it, filtered.counts[it] ?: 0) },
            selectedType = fs.type,
            reliabilityLevel = fs.maxTier,
            pinnedType = fs.pinned,
            isFilteredEmpty = items.isNotEmpty() && filtered.visible.isEmpty() && !t.refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    fun onToggleProtection() = viewModelScope.launch { toggleProtection() }
    fun onCardClick(card: FeedCard) {
        when (card) {
            is FeedCard.Redacted -> gateTarget.value = card.item
            is FeedCard.Normal -> navigateToDetail(card.item.id)
        }
    }

    fun onDismissGate() { gateTarget.value = null }

    fun onRevealFromGate(id: String) = viewModelScope.launch {
        revealItem(id)
        gateTarget.value = null
        _actions.emit(FeedAction.NavigateToDetail(id))
    }

    private fun navigateToDetail(id: String) = viewModelScope.launch {
        _actions.emit(FeedAction.NavigateToDetail(id))
    }
    fun onRequestCatchUp() { catchUpVisible.value = true }
    fun onDismissCatchUp() { catchUpVisible.value = false }

    fun onMarkWatched() = viewModelScope.launch {
        // Derive heldIds directly from the source flows rather than from uiState.value.
        // uiState is a WhileSubscribed(5000) StateFlow — when there are no active collectors
        // it returns the stale initial FeedUiState() (empty cards). Reading from the upstream
        // flows via .first() always returns the current truth regardless of subscriber count.
        val items = observeFeed().first()
        val protectionOn = observeProtection().first()
        val revealed = observeRevealed().first()
        val heldIds = items
            .filter { protectionOn && it.isMatch && it.id !in revealed }
            .map { it.id }
            .toSet()
        val snapshot = markWatched(heldIds)
        catchUpVisible.value = false
        _actions.emit(FeedAction.ShowToast("${heldIds.size} match updates unlocked", undoSnapshot = snapshot))
    }

    fun onUndo(snapshot: Set<String>) = viewModelScope.launch { restoreRevealed(snapshot) }

    fun onRefresh() = viewModelScope.launch {
        refreshing.value = true
        val result = refreshFeed()
        offline.value = result.isFailure
        refreshing.value = false
    }

    // ---- filter handlers ----
    fun onSelectType(type: FeedType) { selectedType.value = type }
    fun onSetReliability(maxTier: Tier) = viewModelScope.launch { setReliabilityLevel(maxTier) }
    fun onResetReliability() = viewModelScope.launch { setReliabilityLevel(Tier.TIER_3_SPECULATIVE) }
    fun onTogglePin() = viewModelScope.launch {
        val current = selectedType.value
        val pinned = observeFilterPrefs().first().pinned
        setPinnedFeed(if (pinned == current) null else current)
    }
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedViewModelTest"`
Expected: PASS — 7 existing + 5 new = 12 tests.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt
git commit -m "feat: wire news-type + reliability filtering into FeedViewModel

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: `ReliabilityLadder` composable + screenshot baselines

Screenshot tests follow the `/write-screenshot-tests` conventions (Robolectric + Roborazzi, dark `LittleBirdTheme` inside a themed `Surface`). This task also creates the shared `FilterComponentsScreenshotTest` scaffold reused by Tasks 8–10.

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/ReliabilityLadder.kt`
- Create: `app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt`

- [ ] **Step 1: Write `ReliabilityLadder`**

Create `ReliabilityLadder.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.theme.tierColor

/**
 * Connected 3-segment cumulative reliability control (T1·T2·T3). Segments at/below [selected]
 * are "included"; the chosen segment carries a 3dp bottom accent in primary. Tapping a segment
 * sets the ceiling via [onSelect].
 */
@Composable
fun ReliabilityLadder(
    selected: Tier,
    onSelect: (Tier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tier.entries.forEachIndexed { index, tier ->
            if (index > 0) {
                Box(Modifier.width(1.dp).height(32.dp).background(outline))
            }
            val included = tier.ordinal <= selected.ordinal
            val isChosen = tier == selected
            val pip = tierColor(tier).copy(alpha = if (included) 1f else 0.35f)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(if (included) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                    .drawBehind {
                        if (isChosen) {
                            val h = 3.dp.toPx()
                            drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                        }
                    }
                    .clickable(role = Role.RadioButton) { onSelect(tier) }
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(pip))
                    Text(
                        text = "T${tier.ordinal + 1}",
                        color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Create the screenshot test scaffold + ladder cases**

Create `FilterComponentsScreenshotTest.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.feed.filter.ReliabilityLadder
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val DefaultRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
)

private fun ComposeContentTestRule.captureThemed(filePath: String, content: @Composable () -> Unit) {
    setContent {
        LittleBirdTheme {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.padding(16.dp)) { content() }
            }
        }
    }
    onRoot().captureRoboImage(filePath = filePath, roborazziOptions = DefaultRoborazziOptions)
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel8a)
class FilterComponentsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `reliability ladder - level 1`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level1.png") {
            ReliabilityLadder(selected = Tier.TIER_1_RELIABLE, onSelect = {})
        }
    }

    @Test fun `reliability ladder - level 2`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level2.png") {
            ReliabilityLadder(selected = Tier.TIER_2_BEAT, onSelect = {})
        }
    }

    @Test fun `reliability ladder - level 3`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_reliability_ladder_level3.png") {
            ReliabilityLadder(selected = Tier.TIER_3_SPECULATIVE, onSelect = {})
        }
    }
}
```

- [ ] **Step 3: Record the baselines**

Run: `./gradlew :app:recordRoborazziDebug --tests "*FilterComponentsScreenshotTest"`
Expected: BUILD SUCCESSFUL; 3 PNGs written under `app/src/test/screenshots/`.

- [ ] **Step 4: Eyeball the baselines**

Open the 3 `reliability_ladder_level*.png` files. Confirm: level 1 fills only T1 (green pip, accent under T1); level 2 fills T1+T2; level 3 fills all three; excluded segments are dim with faded pips.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/ReliabilityLadder.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt \
        app/src/test/screenshots/FilterComponentsScreenshotTest_reliability_ladder_level1.png \
        app/src/test/screenshots/FilterComponentsScreenshotTest_reliability_ladder_level2.png \
        app/src/test/screenshots/FilterComponentsScreenshotTest_reliability_ladder_level3.png
git commit -m "feat: add ReliabilityLadder composable + screenshot baselines

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: `FeedTypeTabs` composable + labels + screenshot baselines

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FeedTypeLabels.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FeedTypeTabs.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt`

- [ ] **Step 1: Write the labels helper**

Create `FeedTypeLabels.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui.feed.filter

import dev.crushingdigits.littlebird.domain.model.FeedType

/** Short tab label for a [FeedType] (kept out of the domain enum). */
fun FeedType.shortLabel(): String = when (this) {
    FeedType.ALL -> "All"
    FeedType.TRANSFERS -> "Transfers"
    FeedType.MATCH -> "Match"
    FeedType.SQUAD_STAFF -> "Squad & staff"
    FeedType.CONTRACTS -> "Contracts"
    FeedType.ACADEMY -> "Academy"
}
```

- [ ] **Step 2: Write `FeedTypeTabs`**

Create `FeedTypeTabs.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.ui.feed.FeedTab

/**
 * Horizontally-scrollable single-select news-type tab strip. The active tab gets a primary
 * underline; tabs with a zero count are dimmed; the pinned feed shows a filled-pin glyph.
 */
@Composable
fun FeedTypeTabs(
    tabs: List<FeedTab>,
    selected: FeedType,
    pinned: FeedType?,
    onSelect: (FeedType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.type == selected
            val dim = tab.count == 0 && !isSelected
            val labelColor = when {
                isSelected -> MaterialTheme.colorScheme.onSurface
                dim -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(tab.type) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tab.type == pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = tab.type.shortLabel(),
                        color = labelColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    Text(text = "${tab.count}", color = labelColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (isSelected) 20.dp else 0.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
}
```

- [ ] **Step 3: Add tab screenshot cases**

In `FilterComponentsScreenshotTest.kt`, add imports:
```kotlin
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.ui.feed.FeedTab
import dev.crushingdigits.littlebird.ui.feed.filter.FeedTypeTabs
```
And inside the class:
```kotlin
    private val sampleTabs = listOf(
        FeedTab(FeedType.ALL, 12),
        FeedTab(FeedType.TRANSFERS, 5),
        FeedTab(FeedType.MATCH, 0),
        FeedTab(FeedType.SQUAD_STAFF, 3),
        FeedTab(FeedType.CONTRACTS, 2),
        FeedTab(FeedType.ACADEMY, 2),
    )

    @Test fun `type tabs - all selected`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_type_tabs_all_selected.png") {
            FeedTypeTabs(tabs = sampleTabs, selected = FeedType.ALL, pinned = null, onSelect = {})
        }
    }

    @Test fun `type tabs - transfers selected and pinned`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_type_tabs_transfers_pinned.png") {
            FeedTypeTabs(tabs = sampleTabs, selected = FeedType.TRANSFERS, pinned = FeedType.TRANSFERS, onSelect = {})
        }
    }
```

- [ ] **Step 4: Record + eyeball + commit**

Run: `./gradlew :app:recordRoborazziDebug --tests "*FilterComponentsScreenshotTest"`
Expected: BUILD SUCCESSFUL; the two new `type_tabs_*` PNGs appear (ladder PNGs unchanged).
Open them: All tab underlined when selected; Match (count 0) dimmed; pin glyph on Transfers when pinned.
```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FeedTypeLabels.kt \
        app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FeedTypeTabs.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt \
        app/src/test/screenshots/FilterComponentsScreenshotTest_type_tabs_all_selected.png \
        app/src/test/screenshots/FilterComponentsScreenshotTest_type_tabs_transfers_pinned.png
git commit -m "feat: add FeedTypeTabs composable + labels + screenshot baselines

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: `PinButton` + `FilterEmpty` composables + screenshot baselines

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/PinButton.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FilterEmpty.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt`

- [ ] **Step 1: Write `PinButton`**

Create `PinButton.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Pins/unpins the current feed as the default landing feed. Filled (primary) when [pinned]. */
@Composable
fun PinButton(pinned: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (pinned) "Unpin default feed" else "Pin as default feed",
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 2: Write `FilterEmpty`**

Create `FilterEmpty.kt`:
```kotlin
package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Shown when the feed has items but none match the active type + reliability level. */
@Composable
fun FilterEmpty(onShowAllTiers: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        Text("Nothing here right now", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "No stories in this feed at your current reliability level. Try a lower tier or another feed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onShowAllTiers) { Text("Show all tiers") }
    }
}
```

- [ ] **Step 3: Add screenshot cases**

In `FilterComponentsScreenshotTest.kt`, add imports:
```kotlin
import dev.crushingdigits.littlebird.ui.feed.filter.FilterEmpty
import dev.crushingdigits.littlebird.ui.feed.filter.PinButton
```
And inside the class:
```kotlin
    @Test fun `pin button - on`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_pin_button_on.png") {
            PinButton(pinned = true, onToggle = {})
        }
    }

    @Test fun `pin button - off`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_pin_button_off.png") {
            PinButton(pinned = false, onToggle = {})
        }
    }

    @Test fun `filter empty state`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_filter_empty.png") {
            FilterEmpty(onShowAllTiers = {})
        }
    }
```

- [ ] **Step 4: Record + eyeball + commit**

Run: `./gradlew :app:recordRoborazziDebug --tests "*FilterComponentsScreenshotTest"`
Expected: BUILD SUCCESSFUL; 3 new PNGs (`pin_button_on`, `pin_button_off`, `filter_empty`).
Eyeball: filled red pin vs ghost pin; empty state with info tile, title, subtitle, "Show all tiers" button.
```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/PinButton.kt \
        app/src/main/java/dev/crushingdigits/littlebird/ui/feed/filter/FilterEmpty.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt \
        app/src/test/screenshots/FilterComponentsScreenshotTest_pin_button_on.png \
        app/src/test/screenshots/FilterComponentsScreenshotTest_pin_button_off.png \
        app/src/test/screenshots/FilterComponentsScreenshotTest_filter_empty.png
git commit -m "feat: add PinButton + FilterEmpty composables + screenshot baselines

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Integrate the filter chrome into `FeedScreen`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt`

- [ ] **Step 1: Add imports**

In `FeedScreen.kt`, add:
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import dev.crushingdigits.littlebird.ui.feed.filter.FeedTypeTabs
import dev.crushingdigits.littlebird.ui.feed.filter.FilterEmpty
import dev.crushingdigits.littlebird.ui.feed.filter.PinButton
import dev.crushingdigits.littlebird.ui.feed.filter.ReliabilityLadder
```
(`Spacer`, `height`, `Modifier`, `dp`, `weight` come via existing imports / `Modifier`.)

- [ ] **Step 2: Render the chrome above the scroll**

In `FeedContent`, replace the `Scaffold { padding -> PullToRefreshBox(...) { ... } }` body so the chrome sits between the top bar and the scroll. Replace the block starting at `PullToRefreshBox(` and its `modifier = Modifier.padding(padding).fillMaxSize(),` with a `Column` wrapper:
```kotlin
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FeedTypeTabs(
                tabs = state.filterTabs,
                selected = state.selectedType,
                pinned = state.pinnedType,
                onSelect = vm::onSelectType,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReliabilityLadder(selected = state.reliabilityLevel, onSelect = vm::onSetReliability)
                Spacer(Modifier.weight(1f))
                PinButton(pinned = state.selectedType == state.pinnedType, onToggle = vm::onTogglePin)
            }
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::onRefresh,
                state = pullState,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.isEmpty && state.isOffline -> EmptyState(
                        title = "You're offline",
                        subtitle = "Connect to load the latest LFC news.",
                    )
                    state.isEmpty -> EmptyState(
                        title = "Nothing yet",
                        subtitle = "Pull to refresh for the latest transfer & backroom news.",
                    )
                    state.isFilteredEmpty -> FilterEmpty(onShowAllTiers = vm::onResetReliability)
                    else -> LazyColumn(
                        contentPadding = PaddingValues(14.dp, 0.dp, 14.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            ProtectionBanner(
                                on = state.protectionOn,
                                onToggle = vm::onToggleProtection,
                            )
                        }
                        if (state.protectionOn && state.heldMatchCount > 0) {
                            item {
                                CatchUpCard(
                                    count = state.heldMatchCount,
                                    onMarkWatched = vm::onRequestCatchUp,
                                )
                            }
                        }
                        items(state.cards, key = { it.item.id }) { card ->
                            when (card) {
                                is FeedCard.Normal -> NewsCard(
                                    item = card.item,
                                    relativeTime = card.item.relativeTime(),
                                    onClick = { vm.onCardClick(card) },
                                )
                                is FeedCard.Redacted -> RedactedCard(
                                    item = card.item,
                                    onClick = { vm.onCardClick(card) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
```
Leave the `state.gateTarget?.let { ... }` and `if (state.catchUpVisible) { ... }` blocks (after the `Scaffold`) unchanged.

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Add a full-screen screenshot test**

In `FilterComponentsScreenshotTest.kt`, add imports:
```kotlin
import androidx.compose.material3.SnackbarHostState
import dev.crushingdigits.littlebird.ui.feed.FeedCard
import dev.crushingdigits.littlebird.ui.feed.FeedTab
```
(`FeedTab` may already be imported from Task 8 — do not duplicate.)

Rendering the full `FeedContent` requires a `FeedViewModel`, which is awkward in a screenshot test. Instead, screenshot the **chrome rows only** (tabs + ladder + pin) as the screen composes them, which is the new visual surface:
```kotlin
    @Test fun `feed chrome - tabs ladder and pin`() {
        composeTestRule.captureThemed("FilterComponentsScreenshotTest_feed_chrome.png") {
            androidx.compose.foundation.layout.Column {
                FeedTypeTabs(tabs = sampleTabs, selected = FeedType.TRANSFERS, pinned = FeedType.TRANSFERS, onSelect = {})
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    ReliabilityLadder(selected = Tier.TIER_2_BEAT, onSelect = {})
                    Spacer(Modifier.width(12.dp))
                    PinButton(pinned = true, onToggle = {})
                }
            }
        }
    }
```
Add imports used here:
```kotlin
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
```

- [ ] **Step 5: Record + eyeball + commit**

Run: `./gradlew :app:recordRoborazziDebug --tests "*FilterComponentsScreenshotTest"`
Expected: BUILD SUCCESSFUL; `feed_chrome` PNG written.
Eyeball: tab strip over a row of [ladder ... pin].
```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt \
        app/src/test/java/dev/crushingdigits/littlebird/ui/FilterComponentsScreenshotTest.kt \
        app/src/test/screenshots/FilterComponentsScreenshotTest_feed_chrome.png
git commit -m "feat: render filter chrome (tabs + ladder + pin) in FeedScreen

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: UIAutomator filter flow test (emulator)

Uses the existing Hilt fake feed (`FakeNewsRepository`: a T2 MATCH item + a T1 TRANSFER item; protection ON by default). Switching to the Match feed and lowering reliability to T1 empties the Match feed (the only match item is T2) → the `FilterEmpty` state appears. All assertions are text-based (proven to work with `textAsString()` in `SpoilerFlowUiTest`). **Requires a connected device/emulator.**

**Files:**
- Create: `app/src/androidTest/java/dev/crushingdigits/littlebird/FilterFlowUiTest.kt`

- [ ] **Step 1: Write the UI test**

Create `FilterFlowUiTest.kt`:
```kotlin
package dev.crushingdigits.littlebird

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UIAutomator test for the news-type tab + reliability ladder flow.
 *
 * Fake feed: one T2 MATCH item (shown redacted under default protection) + one T1 TRANSFER item.
 * Selecting the Match feed shows the redacted match card; lowering reliability to T1 excludes the
 * T2 match item, leaving the Match feed empty (FilterEmpty); "Show all tiers" restores it.
 *
 * NOTE: run on a connected device via connectedDebugAndroidTest.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FilterFlowUiTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun match_feed_at_tier1_is_empty_and_show_all_tiers_restores_it() = uiAutomator {
        startActivity(MainActivity::class.java)

        // Select the Match news-type tab.
        onElement(timeoutMs = 10_000L) { textAsString() == "Match" }.click()

        // Default reliability is 3, so the T2 match item shows as a redacted card.
        onElement(timeoutMs = 5_000L) { textAsString() == "Match content hidden" }

        // Lower reliability to T1 — the only match item (T2) is excluded → empty feed.
        onElement(timeoutMs = 5_000L) { textAsString() == "T1" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Nothing here right now" }

        // "Show all tiers" resets the level to 3 and the redacted match card returns.
        onElement(timeoutMs = 5_000L) { textAsString() == "Show all tiers" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Match content hidden" }
    }
}
```

- [ ] **Step 2: Run on a connected device**

Confirm a device/emulator is attached: `adb devices` (expect one `device`).
Run: `./gradlew :app:connectedDebugAndroidTest --tests "*FilterFlowUiTest"`
Expected: PASS. (If no device is available, note this and run it before merge — see the `/write-ui-tests` workflow.)

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/dev/crushingdigits/littlebird/FilterFlowUiTest.kt
git commit -m "test: UIAutomator filter flow (tab + reliability + empty + reset)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Final verification

**Files:** none (verification only).

- [ ] **Step 1: Full unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all unit tests pass (filter domain, prefs, use cases, ViewModel).

- [ ] **Step 2: Screenshot verification**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL — recorded baselines match (no unexpected diffs).

- [ ] **Step 3: Lint/format gate (if configured)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (No detekt config is present in this repo; skip if not configured.)

- [ ] **Step 4: Summarize**

Confirm against the spec's behaviour contract: tabs filter by type; the ladder applies a cumulative ceiling; counts update live; pin persists and seeds the cold-launch feed; reliability persists globally; filtered-empty shows `FilterEmpty` with a working "Show all tiers"; spoiler redaction still applies inside a Match feed. Report any deviations.

---

## Self-Review

**1. Spec coverage** — every spec section maps to a task:
- §4 Domain model → Tasks 1, 2. §5 Persistence → Tasks 3, 4. §6 ViewModel → Tasks 5, 6.
- §7 UI components → Tasks 7 (ladder), 8 (tabs + labels), 9 (pin + empty), 10 (FeedScreen integration).
- §8 Behaviour contract → enforced in Task 6 (held count global, isFilteredEmpty, orthogonal redaction) + Task 12 summary.
- §9 Testing → unit (Tasks 1–6), screenshot (7–10), UI emulator (11), verification (12). Gates confirmed: Roborazzi configured; UIAutomator 2.4.0-beta02.
- §3 State survival → Task 6 (selectedType session-only + init re-seed) + `cold launch seeds from pinned` test.

**2. Placeholder scan** — no TBD/TODO; every code step shows full code; every run step shows the command + expected result.

**3. Type consistency** — `FeedType.fromKey`/`matches`, `FeedFilter(type, maxTier)`, `FilteredFeed(visible, counts)`, `FilterFeed.invoke(items, filter)`, `FilterPrefs(maxTier, pinned)`, `SetReliabilityLevel(maxTier: Tier)`, `SetPinnedFeed(type: FeedType?)`, repo `observeReliabilityLevel/observePinnedFeed/setReliabilityLevel/setPinnedFeed`, VM handlers `onSelectType/onSetReliability/onResetReliability/onTogglePin`, UiState `filterTabs/selectedType/reliabilityLevel/pinnedType/isFilteredEmpty`, `FeedTab(type, count)`, `FeedType.shortLabel()` — all names consistent across tasks. The `FilterFeedUseCase` no-arg constructor is bound via `@Binds` (Task 2) and constructed directly in tests.

**Known deviation noted:** the full-screen `FeedContent` screenshot is replaced by a chrome-only screenshot (Task 10 Step 4) because `FeedContent` requires a live `FeedViewModel`; the integration itself is verified by `compileDebugKotlin` + the UI emulator flow (Task 11).
