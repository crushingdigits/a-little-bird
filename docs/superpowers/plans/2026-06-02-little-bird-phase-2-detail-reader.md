# Phase 2 — The Detail Reader: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a full-screen in-app Detail reader, reached from the Feed via Jetpack Navigation 3, backed by the RSS summary with an "Open original" Custom Tab and a native Share action.

**Architecture:** UI → Domain → Data, unchanged. A new Nav3 back stack (`FeedKey`, `DetailKey`) hosts the existing home scaffold and a new `DetailScreen`. A `DetailViewModel` resolves the item by id through a new `ObserveNewsItem` use case → `NewsRepository.observeItem` → `NewsDao.observeById`. The gate's "Reveal anyway" now un-blurs **and** navigates into the reader. Tasks 1–2 are a gated toolchain/dependency prerequisite (Kotlin 2.3.21 + Nav3 + serialization + browser).

**Tech Stack:** Kotlin 2.3.21, AGP 9.2.1 (verify Kotlin coupling), Jetpack Navigation 3, Hilt (assisted VM injection), Room, Compose Material 3, Coil, AndroidX Browser (Custom Tabs), Roborazzi, UIAutomator 2.4.

**Spec:** `docs/superpowers/specs/2026-06-02-little-bird-phase-2-detail-reader-design.md`

---

## Conventions for this repo (read before any commit)

- **This project lives on GitHub and uses RAW GIT, not `/submit-mr` or `/create-branch`.** This is the user's standing instruction for this repository. Commit with plain `git`. Do **not** push unless the user asks.
- Work happens on the existing branch **`phase-2-detail-reader`** (already created; the spec commit lives there).
- Every commit message ends with the trailer:
  `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>`
- **Test post-impl skills still apply** (per `use-skills.md` + `android-test-selection.md`): record Roborazzi baselines via the `/write-screenshot-tests` Workflow (Task 12), run emulator tests via the `/write-ui-tests` Workflow (Task 13), and close coverage via the `/write-unit-tests` Workflow (Task 14). Only the commit/MR flow uses raw git here.
- **Library versions:** resolve every new/changed dependency version through **Context7** (`mcp__context7__resolve-library-id` → `mcp__context7__get-library-docs`) before editing the catalog, per the user's library-docs rule. Where a version says "resolve via Context7," that is an instruction, not a placeholder.
- Test style: JUnit 4, Truth, Turbine, `MainDispatcherRule`, fakes over mocks, inline test factories (the project does not use a separate `:test` module or a `NewsItemSample`).

---

## Task 1: Toolchain — upgrade to Kotlin 2.3.21 (GATE)

**This must build green before any feature code.** AGP 9 applies the Kotlin Gradle plugin itself; the catalog `kotlin` version feeds the `kotlin-compose` plugin and anchors KSP.

**Files:**
- Modify: `gradle/libs.versions.toml:6-7` (and `:2` if AGP must move)

- [ ] **Step 1: Resolve compatibility via Context7**

Run these Context7 queries and read the results before editing:
- `mcp__context7__resolve-library-id` for "Android Gradle Plugin" → then `get-library-docs` with topic "Kotlin compatibility AGP 9.2 bundled Kotlin version". Determine whether AGP 9.2.1 accepts Kotlin **2.3.21**. If it does not, note the minimum AGP that does.
- `mcp__context7__resolve-library-id` for "KSP" (google/ksp) → `get-library-docs` topic "release versions for Kotlin 2.3.21". Record the exact matching KSP version string `2.3.21-x.y.z`.

Write the two resolved values into the step below before editing.

- [ ] **Step 2: Verify the current build is green (baseline)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — establishes the pre-upgrade baseline.

- [ ] **Step 3: Edit the version catalog**

In `gradle/libs.versions.toml`, set:

```toml
kotlin = "2.3.21"
ksp = "2.3.21-<resolved-in-step-1>"   # exact value from Context7
```

If Step 1 found AGP 9.2.1 does **not** carry Kotlin 2.3.21, also bump `agp` to the resolved compatible version. Otherwise leave `agp = "9.2.1"`. Update the comment on lines 3-5 to state the new anchored Kotlin version.

- [ ] **Step 4: Confirm Compose BOM / Hilt / Room compile under the new toolchain**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If the Compose compiler complains about an incompatible BOM, resolve the compatible `composeBom` via Context7 and bump it; re-run.

- [ ] **Step 5: Run the full existing suite (the gate)**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all Phase-1 tests green — **unchanged behaviour under Kotlin 2.3.21**.

- [ ] **Step 6: Confirm the instrumented sources still compile**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "build: upgrade to Kotlin 2.3.21 (KSP + Compose compiler in lockstep)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Build dependencies — Nav3, serialization, browser

**Files:**
- Modify: `gradle/libs.versions.toml` ([versions], [libraries], [plugins])
- Modify: `app/build.gradle.kts` (plugins, dependencies)

- [ ] **Step 1: Resolve versions via Context7**

Resolve and record exact versions:
- `androidx.navigation3:navigation3-runtime` and `androidx.navigation3:navigation3-ui` (resolve "androidx navigation3").
- `androidx.lifecycle:lifecycle-viewmodel-navigation3` (resolve "androidx lifecycle viewmodel navigation3").
- The `hiltViewModel<VM, Factory>(creationCallback = …)` assisted overload: confirm the artifact + version that exposes `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` (resolve "androidx hilt navigation compose" / "hilt lifecycle viewmodel compose"). It may require bumping `hiltNavigationCompose` (currently 1.2.0) or adding a new artifact.
- `androidx.browser:browser` (resolve "androidx browser custom tabs").
- `org.jetbrains.kotlinx:kotlinx-serialization-json` (resolve "kotlinx serialization json"); the serialization Gradle plugin is `org.jetbrains.kotlin.plugin.serialization` at `version.ref = "kotlin"`.

- [ ] **Step 2: Add to the version catalog**

In `gradle/libs.versions.toml` add to `[versions]` (use the values resolved in Step 1):

```toml
nav3 = "<resolved>"            # androidx.navigation3 (runtime + ui share this)
lifecycleViewmodelNav3 = "<resolved>"
serializationJson = "<resolved>"
browser = "<resolved>"
```

Add to `[libraries]`:

```toml
navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "nav3" }
navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "nav3" }
lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
androidx-browser = { group = "androidx.browser", name = "browser", version.ref = "browser" }
```

Add to `[plugins]`:

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

> If Step 1 found the assisted `hiltViewModel` overload needs a newer `hilt-navigation-compose`, bump `hiltNavigationCompose` here (or add the new artifact alias).

- [ ] **Step 3: Wire into the app module**

In `app/build.gradle.kts`, add to the `plugins { }` block:

```kotlin
alias(libs.plugins.kotlin.serialization)
```

Add to `dependencies { }` (next to the other `implementation` lines):

```kotlin
implementation(libs.navigation3.runtime)
implementation(libs.navigation3.ui)
implementation(libs.lifecycle.viewmodel.navigation3)
implementation(libs.kotlinx.serialization.json)
implementation(libs.androidx.browser)
```

- [ ] **Step 4: Verify the project syncs and compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (dependencies resolve; no code uses them yet).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Navigation 3, kotlinx-serialization, and androidx-browser

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: Navigation keys

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/navigation/NavKeys.kt`

- [ ] **Step 1: Create the keys**

```kotlin
package dev.crushingdigits.littlebird.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Home scaffold (Feed + bottom nav). Fixed start destination, bottom of the back stack. */
@Serializable
data object FeedKey : NavKey

/** Full-screen reader for a single news item, identified by its stable id. */
@Serializable
data class DetailKey(val itemId: String) : NavKey
```

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/navigation/NavKeys.kt
git commit -m "feat: add Nav3 keys (FeedKey, DetailKey)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: DAO query — observe a single item by id

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsDao.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsDaoTest.kt`

- [ ] **Step 1: Write the failing test**

Append to the existing `NewsDaoTest` class (it already provides `db`/`dao` lifecycle, the `entity(id, at)` helper, Turbine `.test`, Truth, and `runTest`):

```kotlin
@Test fun `observeById emits the matching row then null for an unknown id`() = runTest {
    dao.upsertAll(listOf(entity("x1", at = 100)))

    dao.observeById("x1").test {
        assertThat(awaitItem()?.id).isEqualTo("x1")
        cancelAndIgnoreRemainingEvents()
    }
    dao.observeById("missing").test {
        assertThat(awaitItem()).isNull()
        cancelAndIgnoreRemainingEvents()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsDaoTest"`
Expected: FAIL — `observeById` unresolved.

- [ ] **Step 3: Add the query**

In `NewsDao.kt` add (keep existing imports; `Flow` already imported):

```kotlin
@Query("SELECT * FROM news_items WHERE id = :id LIMIT 1")
fun observeById(id: String): Flow<NewsItemEntity?>
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsDaoTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsDao.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsDaoTest.kt
git commit -m "feat: add NewsDao.observeById query (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: Local data source + repository — observe a single item

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsLocalDataSource.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/NewsRepository.kt`
- Modify (fakes, so the suite compiles): `app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt`, `app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/NewsRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository test**

Append to the existing `NewsRepositoryTest` class (reuse its `repo(fetcher)`, `rss(...)`, `FakeFetcher`, `source`, and the `kotlinx.coroutines.flow.first` import — all already present). The item id is derived from the link, so resolve it from the feed rather than hardcoding:

```kotlin
@Test fun `observeItem maps the stored row to domain and emits null for a missing id`() = runTest {
    val subject = repo(FakeFetcher(mapOf(source.url to Result.success(rss("Deal agreed for midfielder", "Here we go")))))
    subject.refresh()
    val id = subject.observeFeed().first().single().id

    assertThat(subject.observeItem(id).first()?.id).isEqualTo(id)
    assertThat(subject.observeItem("does-not-exist").first()).isNull()
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"`
Expected: FAIL to compile — `observeItem` is not yet on the `NewsRepository` interface. (Steps 3–4 add the method and update the fakes; the module compiles again only after Step 4.)

- [ ] **Step 3: Implement local source + repository**

In `NewsLocalDataSource.kt` add:

```kotlin
fun observeById(id: String): Flow<NewsItemEntity?> = dao.observeById(id)
```

In `NewsRepository.kt`, add to the interface:

```kotlin
fun observeItem(id: String): Flow<NewsItem?>
```

and to `NewsRepositoryImpl`:

```kotlin
override fun observeItem(id: String): Flow<NewsItem?> =
    local.observeById(id).map { entity -> entity?.toDomain() }
```

- [ ] **Step 4: Update the fakes so the suite compiles**

In `FeedViewModelTest.kt`, add to `FakeNews`:

```kotlin
override fun observeItem(id: String): Flow<NewsItem?> = flow.map { items -> items.firstOrNull { it.id == id } }
```

(import `kotlinx.coroutines.flow.map`.)

In `FakeDataModule.kt`, add to `FakeNewsRepository`:

```kotlin
override fun observeItem(id: String): Flow<NewsItem?> = items.map { list -> list.firstOrNull { it.id == id } }
```

(import `kotlinx.coroutines.flow.map`.)

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsLocalDataSource.kt app/src/main/java/dev/crushingdigits/littlebird/data/news/NewsRepository.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/NewsRepositoryTest.kt app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt
git commit -m "feat: NewsRepository.observeItem(id) + fakes (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: Domain use case — ObserveNewsItem

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveNewsItem.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/ObserveNewsItemTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class ObserveNewsItemTest {

    private fun item(id: String) = NewsItem(
        id = id, source = "BBC", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private class FakeRepo(items: List<NewsItem>) : NewsRepository {
        val flow = MutableStateFlow(items)
        override fun observeFeed(): Flow<List<NewsItem>> = flow
        override fun observeItem(id: String): Flow<NewsItem?> = flow.map { it.firstOrNull { i -> i.id == id } }
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun isEmpty(): Boolean = flow.value.isEmpty()
    }

    @Test fun `delegates to repository observeItem`() = runTest {
        val sut: ObserveNewsItem = ObserveNewsItemUseCase(FakeRepo(listOf(item("a"))))
        assertThat(sut("a").first()?.id).isEqualTo("a")
        assertThat(sut("b").first()).isNull()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*ObserveNewsItemTest"`
Expected: FAIL — `ObserveNewsItem`/`ObserveNewsItemUseCase` unresolved.

- [ ] **Step 3: Create the use case**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveNewsItem { operator fun invoke(id: String): Flow<NewsItem?> }

class ObserveNewsItemUseCase @Inject constructor(private val repo: NewsRepository) : ObserveNewsItem {
    override fun invoke(id: String): Flow<NewsItem?> = repo.observeItem(id)
}
```

- [ ] **Step 4: Bind in DI**

In `DomainModule.kt`, add the import for `ObserveNewsItem`/`ObserveNewsItemUseCase` and add:

```kotlin
@Binds abstract fun observeNewsItem(impl: ObserveNewsItemUseCase): ObserveNewsItem
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*ObserveNewsItemTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveNewsItem.kt app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/ObserveNewsItemTest.kt
git commit -m "feat: add ObserveNewsItem use case + DI binding (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: Extract the relative-time formatter

The `relativeTime` formatter is currently a `private` extension in `FeedScreen.kt`. The Detail reader needs it too — extract to a shared util.

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/util/RelativeTime.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt` (remove the private copy + now-unused imports; import the shared one)
- Test: `app/src/test/java/dev/crushingdigits/littlebird/ui/util/RelativeTimeTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.ui.util

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import java.time.Instant

class RelativeTimeTest {
    private fun at(epochMs: Long) = NewsItem(
        id = "x", source = "s", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "l", imageUrl = null, publishedAt = Instant.ofEpochMilli(epochMs),
    )
    private val now = Instant.ofEpochSecond(10_000_000)

    @Test fun `formats minutes hours days and now`() {
        assertThat(at(now.toEpochMilli()).relativeTime(now)).isEqualTo("now")
        assertThat(at(now.minusSeconds(120).toEpochMilli()).relativeTime(now)).isEqualTo("2m")
        assertThat(at(now.minusSeconds(7200).toEpochMilli()).relativeTime(now)).isEqualTo("2h")
        assertThat(at(now.minusSeconds(172800).toEpochMilli()).relativeTime(now)).isEqualTo("2d")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*RelativeTimeTest"`
Expected: FAIL — `relativeTime` unresolved in `ui.util`.

- [ ] **Step 3: Create the shared util**

```kotlin
package dev.crushingdigits.littlebird.ui.util

import dev.crushingdigits.littlebird.domain.model.NewsItem
import java.time.Duration
import java.time.Instant

/** Compact relative age ("now", "5m", "3h", "2d"). The Detail reader renders it as "{value} ago". */
internal fun NewsItem.relativeTime(now: Instant = Instant.now()): String {
    val mins = Duration.between(publishedAt, now).toMinutes().coerceAtLeast(0)
    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        mins < 1440 -> "${mins / 60}h"
        else -> "${mins / 1440}d"
    }
}
```

- [ ] **Step 4: Update FeedScreen to use it**

In `FeedScreen.kt`:
- Delete the entire `private fun NewsItem.relativeTime(...)` block (lines ~170-178).
- Delete the now-unused imports `java.time.Duration` and `java.time.Instant`.
- Add import `dev.crushingdigits.littlebird.ui.util.relativeTime`.

The call site `card.item.relativeTime()` stays unchanged (now resolves to the shared util).

- [ ] **Step 5: Run tests to verify pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*RelativeTimeTest" --tests "*FeedViewModelTest"`
Expected: PASS (and `:app:compileDebugKotlin` clean — no unused-import errors).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/util/RelativeTime.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt app/src/test/java/dev/crushingdigits/littlebird/ui/util/RelativeTimeTest.kt
git commit -m "refactor: extract relativeTime to ui/util for reuse in Detail (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: DetailUiState + DetailViewModel

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailUiState.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailViewModel.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/ui/detail/DetailViewModelTest.kt`

- [ ] **Step 1: Create the state model**

```kotlin
package dev.crushingdigits.littlebird.ui.detail

import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Immutable snapshot of the Detail reader. */
data class DetailUiState(
    val item: NewsItem? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
)
```

- [ ] **Step 2: Write the failing ViewModel test**

```kotlin
package dev.crushingdigits.littlebird.ui.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.MainDispatcherRule
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItem
import dev.crushingdigits.littlebird.ui.navigation.DetailKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun item(id: String) = NewsItem(
        id = id, source = "BBC", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = "David Ornstein", role = "The Athletic",
        title = "t", summary = "s", link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private fun vm(flow: MutableStateFlow<NewsItem?>, id: String = "a") =
        DetailViewModel(DetailKey(id), ObserveNewsItem { flow })

    @Test fun `emits the resolved item`() = runTest {
        val sut = vm(MutableStateFlow(item("a")))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.item?.id).isEqualTo("a")
            assertThat(s.isLoading).isFalse()
            assertThat(s.notFound).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `emits notFound when the item resolves to null`() = runTest {
        val sut = vm(MutableStateFlow(null))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.item).isNull()
            assertThat(s.notFound).isTrue()
            assertThat(s.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*DetailViewModelTest"`
Expected: FAIL — `DetailViewModel` unresolved.

- [ ] **Step 4: Create the ViewModel**

```kotlin
package dev.crushingdigits.littlebird.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItem
import dev.crushingdigits.littlebird.ui.navigation.DetailKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = DetailViewModel.Factory::class)
class DetailViewModel @AssistedInject constructor(
    @Assisted private val key: DetailKey,
    observeNewsItem: ObserveNewsItem,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = observeNewsItem(key.itemId)
        .map { item ->
            if (item == null) DetailUiState(item = null, isLoading = false, notFound = true)
            else DetailUiState(item = item, isLoading = false, notFound = false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DetailUiState())

    @AssistedFactory
    interface Factory { fun create(key: DetailKey): DetailViewModel }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*DetailViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailUiState.kt app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailViewModel.kt app/src/test/java/dev/crushingdigits/littlebird/ui/detail/DetailViewModelTest.kt
git commit -m "feat: add DetailViewModel + DetailUiState (TDD, assisted injection)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: Feed navigation action + ViewModel wiring

The gate→reveal→detail behaviour and normal-card→detail behaviour. Add a one-off `NavigateToDetail` action and emit it from `onCardClick` (Normal) and `onRevealFromGate`.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedAction.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt`

- [ ] **Step 1: Add the action**

In `FeedAction.kt`, add a member to the sealed interface:

```kotlin
/** Navigate to the Detail reader for [itemId]. */
data class NavigateToDetail(val itemId: String) : FeedAction
```

- [ ] **Step 2: Write the failing tests**

Append to `FeedViewModelTest.kt`:

```kotlin
@Test fun `clicking a normal card emits NavigateToDetail`() = runTest {
    val sut = vm(FakeNews(listOf(item("t", false))), FakePrefs(on = false))
    sut.actions.test {
        sut.onCardClick(FeedCard.Normal(item("t", false)))
        assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("t"))
        cancelAndIgnoreRemainingEvents()
    }
}

@Test fun `clicking a redacted card opens the gate and does not navigate`() = runTest {
    val sut = vm(FakeNews(listOf(item("m", true))), FakePrefs(on = true))
    sut.onCardClick(FeedCard.Redacted(item("m", true)))
    sut.uiState.test {
        assertThat(awaitItem().gateTarget?.id).isEqualTo("m")
        cancelAndIgnoreRemainingEvents()
    }
}

@Test fun `revealing from the gate navigates to detail`() = runTest {
    val prefs = FakePrefs(on = true)
    val sut = vm(FakeNews(listOf(item("m", true))), prefs)
    sut.actions.test {
        sut.onRevealFromGate("m")
        assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("m"))
        cancelAndIgnoreRemainingEvents()
    }
    assertThat(prefs.revealed.value).contains("m")
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedViewModelTest"`
Expected: FAIL — the Normal branch does not emit; `onRevealFromGate` does not emit `NavigateToDetail`.

- [ ] **Step 4: Wire the ViewModel**

In `FeedViewModel.kt`, replace `onCardClick` and `onRevealFromGate`:

```kotlin
fun onCardClick(card: FeedCard) {
    when (card) {
        is FeedCard.Redacted -> gateTarget.value = card.item
        is FeedCard.Normal -> navigateToDetail(card.item.id)
    }
}

fun onRevealFromGate(id: String) = viewModelScope.launch {
    revealItem(id)
    gateTarget.value = null
    _actions.emit(FeedAction.NavigateToDetail(id))
}

private fun navigateToDetail(id: String) = viewModelScope.launch {
    _actions.emit(FeedAction.NavigateToDetail(id))
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedViewModelTest"`
Expected: PASS (including the pre-existing reveal test, which still sees the card turn Normal).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedAction.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt
git commit -m "feat: emit NavigateToDetail on normal-card tap and gate reveal (TDD)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Detail screen composables (stateless content + stateful screen + actions)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailActions.kt` (Custom Tab + Share helpers)
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailScreen.kt` (DetailContent + DetailScreen + states)

- [ ] **Step 1: Create the side-effect helpers**

```kotlin
package dev.crushingdigits.littlebird.ui.detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import dev.crushingdigits.littlebird.domain.model.NewsItem

/** Open the article in a Chrome Custom Tab; fall back to a plain VIEW intent if none is available. */
internal fun openOriginal(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}

/** Native share sheet with the headline + link. */
internal fun shareItem(context: Context, item: NewsItem) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, item.title)
        putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.link}")
    }
    context.startActivity(Intent.createChooser(send, null))
}
```

- [ ] **Step 2: Create the screen composables**

```kotlin
package dev.crushingdigits.littlebird.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.ui.components.TierChip
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.util.relativeTime
import androidx.compose.ui.platform.LocalContext

@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val item = state.item
    when {
        state.notFound -> DetailMessage("Story unavailable", "It may have dropped from the latest feed.", onBack, modifier)
        item != null -> DetailContent(
            item = item,
            relativeTime = item.relativeTime(),
            onBack = onBack,
            onOpenOriginal = { openOriginal(context, item.link) },
            onShare = { shareItem(context, item) },
            modifier = modifier,
        )
        else -> DetailMessage("Loading…", "", onBack, modifier)
    }
}

@Composable
fun DetailContent(
    item: NewsItem,
    relativeTime: String,
    onBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        // Top bar: back + Share
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TierChip(item.tier)
                Text("$relativeTime ago", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.confirmed) {
                    Spacer(Modifier.weight(1f))
                    Text("✓ Here we go", color = Confirmed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Text(item.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                )
            }

            Byline(item)

            Text(
                text = item.summary.ifBlank { "Summary unavailable — open the original to read more." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.link.isNotBlank()) {
                // Hidden for guid-only RSS items with no resolvable link (spec §7).
                Button(onClick = onOpenOriginal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Open original")
                }
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Byline(item: NewsItem) {
    val name = item.author ?: item.source
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Text(initials, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Column {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(item.role, "T${item.tier.ordinal + 1} ${item.tier.label}").joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun DetailMessage(title: String, subtitle: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
```

- [ ] **Step 3: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If `OpenInNew`/`Share` aren't in the icons artifact, both are in `material-icons-extended`, already a dependency.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailActions.kt app/src/main/java/dev/crushingdigits/littlebird/ui/detail/DetailScreen.kt
git commit -m "feat: add Detail reader composables (content, byline, states, share/open)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Nav host + MainActivity rewire + FeedScreen nav param

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/MainActivity.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt` (add `onNavigateToDetail` param + handle the action)

- [ ] **Step 1: Add the nav param to FeedScreen**

In `FeedScreen.kt`, change the `FeedScreen` signature and action handling:

```kotlin
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onRefresh()
        viewModel.actions.collect { action ->
            when (action) {
                is FeedAction.ShowToast -> {
                    val result = snackbarHostState.showSnackbar(
                        message = action.message,
                        actionLabel = action.undoSnapshot?.let { "UNDO" },
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed && action.undoSnapshot != null) {
                        viewModel.onUndo(action.undoSnapshot)
                    }
                }
                is FeedAction.NavigateToDetail -> onNavigateToDetail(action.itemId)
            }
        }
    }

    FeedContent(state, snackbarHostState, viewModel, modifier)
}
```

- [ ] **Step 2: Create the nav host**

```kotlin
package dev.crushingdigits.littlebird.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.crushingdigits.littlebird.ui.components.BottomNav
import dev.crushingdigits.littlebird.ui.detail.DetailScreen
import dev.crushingdigits.littlebird.ui.detail.DetailViewModel
import dev.crushingdigits.littlebird.ui.feed.FeedScreen

@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(FeedKey)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<FeedKey> {
                HomeScaffold(onOpenDetail = { id -> backStack.add(DetailKey(id)) })
            }
            entry<DetailKey> { key ->
                val vm = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
                    creationCallback = { factory -> factory.create(key) },
                )
                DetailScreen(viewModel = vm, onBack = { backStack.removeLastOrNull() })
            }
        },
    )
}

@Composable
private fun HomeScaffold(onOpenDetail: (String) -> Unit) {
    var tab by remember { mutableStateOf("feed") }
    Scaffold(bottomBar = { BottomNav(selected = tab, onSelect = { tab = it }) }) { padding ->
        when (tab) {
            "feed" -> FeedScreen(modifier = Modifier.padding(padding), onNavigateToDetail = onOpenDetail)
            else -> SettingsPlaceholder(Modifier.padding(padding))
        }
    }
}

@Composable
private fun SettingsPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Settings — coming soon", style = MaterialTheme.typography.titleLarge)
    }
}
```

> If Task 2 Step 1 found the assisted `hiltViewModel` overload lives under a different import than `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`, use the import Context7 prescribes.

- [ ] **Step 3: Slim down MainActivity**

Replace the body of `MainActivity.kt` with:

```kotlin
package dev.crushingdigits.littlebird

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import dev.crushingdigits.littlebird.ui.navigation.AppNavHost
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleBirdTheme { AppNavHost() }
        }
    }
}
```

(The old `SettingsPlaceholder` now lives in `AppNavHost.kt`; remove the now-unused imports from `MainActivity.kt`.)

- [ ] **Step 4: Verify compile + full unit suite**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL, all unit tests green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/navigation/AppNavHost.kt app/src/main/java/dev/crushingdigits/littlebird/MainActivity.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt
git commit -m "feat: host Feed + Detail in a Nav3 back stack (NavDisplay)

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Roborazzi screenshot tests for the Detail reader

**Pre-req:** consult the `/write-screenshot-tests` **Conventions** section before writing. Use Robolectric + the dark-theme capture wrapper, mirroring `FeedComponentsScreenshotTest`.

**Files:**
- Create: `app/src/test/java/dev/crushingdigits/littlebird/ui/DetailScreenshotTest.kt`

- [ ] **Step 1: Write the screenshot test (targets the stateless `DetailContent`)**

```kotlin
package dev.crushingdigits.littlebird.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.detail.DetailContent
import dev.crushingdigits.littlebird.ui.detail.DetailMessage
import dev.crushingdigits.littlebird.ui.theme.LittleBirdTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

private val DefaultRoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0f),
    recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.5),
)

private fun ComposeContentTestRule.captureThemed(filePath: String, content: @Composable () -> Unit) {
    setContent {
        LittleBirdTheme {
            Surface(color = MaterialTheme.colorScheme.surface) { Box(Modifier) { content() } }
        }
    }
    onRoot().captureRoboImage(filePath = filePath, roborazziOptions = DefaultRoborazziOptions)
}

private fun item(
    tier: Tier,
    confirmed: Boolean = false,
    author: String? = "David Ornstein",
    role: String? = "The Athletic",
    summary: String = "Liverpool have reached full agreement on personal terms with their priority midfield target.",
    imageUrl: String? = null,
) = NewsItem(
    id = "id", source = "The Athletic", tier = tier, category = Category.TRANSFER, isMatch = false,
    confirmed = confirmed, author = author, role = role,
    title = "Liverpool agree personal terms with target midfielder",
    summary = summary, link = "https://example.com/1", imageUrl = imageUrl,
    publishedAt = Instant.parse("2026-06-01T10:00:00Z"),
)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel8a)
class DetailScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `detail - tier1 confirmed`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier1_confirmed.png") {
            DetailContent(item(Tier.TIER_1_RELIABLE, confirmed = true), "2h", {}, {}, {})
        }
    }

    @Test fun `detail - tier2 no image`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier2.png") {
            DetailContent(item(Tier.TIER_2_BEAT, author = "This Is Anfield", role = "Beat reporting"), "45m", {}, {}, {})
        }
    }

    @Test fun `detail - tier3 speculative`() {
        composeTestRule.captureThemed("DetailScreenshotTest_tier3.png") {
            DetailContent(item(Tier.TIER_3_SPECULATIVE, author = "Daily Mirror", role = "Aggregated speculation"), "3h", {}, {}, {})
        }
    }

    @Test fun `detail - blank summary fallback`() {
        composeTestRule.captureThemed("DetailScreenshotTest_blank_summary.png") {
            DetailContent(item(Tier.TIER_2_BEAT, summary = ""), "1h", {}, {}, {})
        }
    }

    @Test fun `detail - with image slot`() {
        composeTestRule.captureThemed("DetailScreenshotTest_with_image.png") {
            DetailContent(item(Tier.TIER_2_BEAT, imageUrl = "https://example.com/p.jpg"), "20m", {}, {}, {})
        }
    }

    @Test fun `detail - not found state`() {
        composeTestRule.captureThemed("DetailScreenshotTest_not_found.png") {
            DetailMessage("Story unavailable", "It may have dropped from the latest feed.", onBack = {})
        }
    }
}
```

> Note: under Robolectric the Coil `AsyncImage` renders empty, so `with_image` validates the framed image slot/layout, not pixels.

- [ ] **Step 2: Record baselines via the skill**

Invoke the **`/write-screenshot-tests` Workflow** section to record (it runs `recordRoborazziDebug`). Confirm new PNGs appear under `app/src/test/screenshots/`.

- [ ] **Step 3: Verify the comparison run is green**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL (baselines match).

- [ ] **Step 4: Commit (code + baseline images)**

```bash
git add app/src/test/java/dev/crushingdigits/littlebird/ui/DetailScreenshotTest.kt app/src/test/screenshots/
git commit -m "test: Roborazzi screenshots for the Detail reader

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 13: UIAutomator — extend the signature flow to the reader

**Pre-req:** consult the `/write-ui-tests` **Conventions** section (uiAutomator{}, onElement{}, watchFor). The Hilt fake feed (`FakeDataModule`) provides `transfer-1` (Normal) and `match-1` (Redacted under protection). `FakeNewsRepository.observeItem` (added in Task 5) lets the Detail VM resolve items.

**Files:**
- Modify: `app/src/androidTest/java/dev/crushingdigits/littlebird/SpoilerFlowUiTest.kt`

- [ ] **Step 1: Add the two reader-flow tests**

Append to `SpoilerFlowUiTest`:

```kotlin
/** Tapping a non-match (Normal) card opens the Detail reader; device-back returns to the feed. */
@Test
fun tapping_a_normal_card_opens_the_detail_reader() = uiAutomator {
    startActivity(MainActivity::class.java)

    val transferCard = onElement(timeoutMs = 10_000L) {
        textAsString() == "Liverpool agree £65m deal for midfielder"
    }
    transferCard.click()

    // Reader shows the primary "Open original" button.
    onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }

    pressBack()

    // Back on the feed.
    onElement(timeoutMs = 5_000L) { textAsString()?.contains("Spoiler protection") == true }
}

/** Revealing a match card through the gate opens the reader for that item. */
@Test
fun revealing_a_match_card_opens_the_detail_reader() = uiAutomator {
    startActivity(MainActivity::class.java)

    onElement(timeoutMs = 10_000L) { textAsString() == "Match content hidden" }.click()
    onElement(timeoutMs = 5_000L) { textAsString() == "This one might spoil you" }
    onElement(timeoutMs = 5_000L) { textAsString() == "Reveal anyway" }.click()

    // After reveal the reader opens — assert its "Open original" button (do NOT click it,
    // to avoid launching an external browser out of the app).
    onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }
}
```

Add the import `androidx.test.uiautomator.pressBack` if `pressBack()` is not already resolvable in the `uiAutomator {}` scope (confirm against the `/write-ui-tests` conventions).

- [ ] **Step 2: Compile the instrumented sources**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run on a device/emulator via the skill**

Invoke the **`/write-ui-tests` Workflow** section to run `connectedDebugAndroidTest` on a connected device. Expected: all `SpoilerFlowUiTest` tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/java/dev/crushingdigits/littlebird/SpoilerFlowUiTest.kt
git commit -m "test: UIAutomator coverage for normal-tap and reveal→detail flows

Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: Full verification + coverage gap pass

**Files:** none (verification only; close any coverage gaps the kover pass reveals).

- [ ] **Step 1: Full unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all unit tests pass.

- [ ] **Step 2: Coverage gap pass via the skill**

Invoke the **`/write-unit-tests` Workflow** section (runs unit tests + `koverXmlReport` in one invocation). Inspect the report for the new Detail/Nav code (`DetailViewModel`, `ObserveNewsItem`, `NewsRepository.observeItem`, `FeedViewModel` nav branches). Close meaningful gaps with additional tests, then re-run. Commit any added tests with the standard trailer.

- [ ] **Step 3: Screenshot comparison**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Assemble the debug APK**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Manual smoke (device/emulator)** — see Test Plan below.

- [ ] **Step 6: Final commit if Step 2 added tests** (otherwise nothing to commit).

---

## Test Plan (manual verification — CI cannot catch these)

- **Normal card → reader:** tap a transfer/coaching card → reader slides in showing headline, tier byline, summary, "Open original" + "Share". Device-back returns to the feed with scroll position retained.
- **Gate reveal → reader:** with protection ON, tap a redacted match card → gate → "Reveal anyway" → card un-blurs in the feed **and** the reader opens for that item. "Keep it hidden" closes the gate with no navigation.
- **Open original:** tap "Open original" → a Chrome Custom Tab opens the article. On a device with no Custom Tab provider, it falls back to the system browser.
- **Share:** tap "Share" (top bar or button) → the native share sheet shows the headline + link.
- **Blank summary:** an item whose RSS `summary` is empty shows the "open the original to read more" fallback line.
- **Not found / process death:** kill & relaunch while the reader is open (or open an item then clear the cache) → reader re-resolves from Room, or shows the "Story unavailable" state with a working back button — never a crash.
- **Rotation:** rotate while reading → reader state (and the back stack) survives.
```
