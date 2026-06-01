# A Little Bird — Phase 1A: Data Layer & Classifier — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the fully unit-tested data layer for A Little Bird — RSS/Atom fetch → parse → classify (match-flag + category + tier + confirmed) → cache in Room — exposed through repositories and use cases. No UI.

**Architecture:** Single `:app` module, packages structured by data type (`data/news`, `data/preferences`). `NewsRepository` coordinates a remote data source (OkHttp + hand-rolled `XmlPullParser`) and a local data source (Room, the single source of truth), applying a data-driven classifier and tier resolver at ingestion. `PreferencesRepository` wraps a DataStore data source for `protectionOn` + `revealedIds`. Hilt for DI. Coroutines + Flow throughout.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, Room (+KSP), OkHttp, DataStore-Preferences, `java.time`. Tests: JUnit4, Truth, Turbine, Robolectric, OkHttp MockWebServer, fakes-over-mocks.

**Spec:** `docs/superpowers/specs/2026-06-01-little-bird-phase-1-design.md` (§3–§8, §10).

---

## Conventions & guardrails (read before starting)

- **Tests follow the project's `/write-unit-tests` Conventions** (Truth assertions, Turbine for Flows, JUnit4, `MainDispatcherRule`, fakes over mocks, lazy `subject` init). Consult that skill's Conventions section before writing any test.
- **TDD is mandatory** for all logic tasks: write the failing test, watch it fail, implement minimally, watch it pass.
- **Version control:** This project is **not yet a git repository**. Per the project rules, git/MR operations go through the `/create-branch` and `/submit-mr` skills — **do not run raw `git commit`/`git push`**. The "✅ Checkpoint" markers below are milestone boundaries; commit via `/submit-mr` at those points once a repo/branch exists.
- **Library versions:** Task 1 pins versions in the catalog. Per the project's Context7 rule, **verify each version via Context7** against AGP `9.2.1` before trusting the numbers below; adjust if Context7 reports incompatibility.
- **Package root:** `dev.crushingdigits.littlebird`. Production source root: `app/src/main/java/dev/crushingdigits/littlebird/`. Test root: `app/src/test/java/dev/crushingdigits/littlebird/`.
- Run all unit tests with: `./gradlew :app:testDebugUnitTest`. A single class: `./gradlew :app:testDebugUnitTest --tests "<FQN>"`.

---

## File map (what gets created)

```
app/build.gradle.kts                         (modified: plugins + deps)
gradle/libs.versions.toml                     (modified: versions/libs/plugins)
app/src/main/AndroidManifest.xml              (modified: Application name + INTERNET)
app/src/main/java/dev/crushingdigits/littlebird/
  LittleBirdApp.kt                            @HiltAndroidApp
  domain/model/Tier.kt
  domain/model/Category.kt
  domain/model/Classification.kt
  domain/model/NewsItem.kt
  domain/usecase/ObserveFeed.kt               (interface + impl)
  domain/usecase/RefreshFeed.kt
  domain/usecase/ObserveProtection.kt
  domain/usecase/ToggleProtection.kt
  domain/usecase/RevealItem.kt
  domain/usecase/MarkWatched.kt
  data/news/
    NewsRepository.kt                         (interface + impl)
    classifier/TextNormalizer.kt
    classifier/ClassifierConfig.kt
    classifier/MatchClassifier.kt
    classifier/Categoriser.kt
    classifier/NewsClassifier.kt
    tier/TierConfig.kt
    tier/TierResolver.kt
    source/FeedSource.kt
    source/FeedSources.kt
    mapper/NewsMappers.kt
    datasource/remote/RawFeedItem.kt
    datasource/remote/FeedParser.kt
    datasource/remote/FeedFetcher.kt
    datasource/remote/NewsRemoteDataSource.kt
    datasource/local/NewsItemEntity.kt
    datasource/local/NewsDao.kt
    datasource/local/LittleBirdDatabase.kt
    datasource/local/NewsLocalDataSource.kt
  data/preferences/
    PreferencesRepository.kt                  (interface + impl)
    datasource/PreferencesDataSource.kt
  di/NetworkModule.kt
  di/DatabaseModule.kt
  di/DataStoreModule.kt
  di/DataBindingsModule.kt
app/src/test/java/dev/crushingdigits/littlebird/  (mirrors above for tests)
app/src/test/resources/feeds/*.xml            (captured feed fixtures)
```

---

## Task 1: Gradle setup — plugins, version catalog, Application, manifest

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/LittleBirdApp.kt`
- Delete: the scaffold sample tests `app/src/test/java/.../ExampleUnitTest.kt`, `app/src/androidTest/java/.../ExampleInstrumentedTest.kt`

- [ ] **Step 1: Verify versions via Context7**, then replace `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "9.2.1"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coreKtx = "1.18.0"
coroutines = "1.9.0"
hilt = "2.52"
room = "2.6.1"
okhttp = "4.12.0"
datastore = "1.1.1"
lifecycle = "2.8.7"
# test
junit = "4.13.2"
truth = "1.4.4"
turbine = "1.2.0"
robolectric = "4.14"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
# test
junit = { group = "junit", name = "junit", version.ref = "junit" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 2: Replace `app/build.gradle.kts`** with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "dev.crushingdigits.littlebird"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.crushingdigits.littlebird"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
}
```

> Note: `compileSdk = 36` uses the simple form (the scaffold's `release(36){…}` block was AGP-preview syntax; keep the simple form unless Context7 confirms the preview API is required).

- [ ] **Step 3: Create `LittleBirdApp.kt`**:

```kotlin
package dev.crushingdigits.littlebird

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LittleBirdApp : Application()
```

- [ ] **Step 4: Edit `AndroidManifest.xml`** — add the INTERNET permission and `android:name` on `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```
and on the `<application>` tag add `android:name=".LittleBirdApp"`.

- [ ] **Step 5: Delete scaffold sample tests**, then sync & build:

Run: `rm app/src/test/java/dev/crushingdigits/littlebird/ExampleUnitTest.kt app/src/androidTest/java/dev/crushingdigits/littlebird/ExampleInstrumentedTest.kt`
Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt/KSP wired, app compiles).

- [ ] **Step 6: ✅ Checkpoint** (commit via `/submit-mr` once a branch exists).

---

## Task 2: Domain model

**Files:**
- Create: `domain/model/Tier.kt`, `domain/model/Category.kt`, `domain/model/Classification.kt`, `domain/model/NewsItem.kt`

No behavior → no test; verified by compilation.

- [ ] **Step 1: Create `domain/model/Tier.kt`**:

```kotlin
package dev.crushingdigits.littlebird.domain.model

enum class Tier(val label: String) {
    TIER_1_RELIABLE("RELIABLE"),
    TIER_2_BEAT("BEAT"),
    TIER_3_SPECULATIVE("SPECULATIVE"),
}
```

- [ ] **Step 2: Create `domain/model/Category.kt`**:

```kotlin
package dev.crushingdigits.littlebird.domain.model

enum class Category { TRANSFER, COACHING, CONTRACT, BACKROOM, ACADEMY, MATCH, OTHER }
```

- [ ] **Step 3: Create `domain/model/Classification.kt`**:

```kotlin
package dev.crushingdigits.littlebird.domain.model

data class Classification(
    val isMatch: Boolean,
    val category: Category,
    val tier: Tier,
    val confirmed: Boolean,
)
```

- [ ] **Step 4: Create `domain/model/NewsItem.kt`**:

```kotlin
package dev.crushingdigits.littlebird.domain.model

import java.time.Instant

data class NewsItem(
    val id: String,
    val source: String,
    val tier: Tier,
    val category: Category,
    val isMatch: Boolean,
    val confirmed: Boolean,
    val author: String?,
    val role: String?,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String?,
    val publishedAt: Instant,
)
```

- [ ] **Step 5: Compile** — Run: `./gradlew :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.

---

## Task 3: TextNormalizer + ClassifierConfig + MatchClassifier (TDD)

The riskiest logic. **Recall-first**: catch every match item. Config is data.

**Files:**
- Create: `data/news/classifier/TextNormalizer.kt`
- Create: `data/news/classifier/ClassifierConfig.kt`
- Create: `data/news/classifier/MatchClassifier.kt`
- Test: `data/news/classifier/MatchClassifierTest.kt`

- [ ] **Step 1: Write the failing test** `MatchClassifierTest.kt` with a real-world labeled corpus:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MatchClassifierTest {

    private val subject = MatchClassifier(defaultClassifierConfig)

    // Headlines that MUST be flagged as match content (zero leaks allowed).
    private val matchHeadlines = listOf(
        "Liverpool 2-1 Arsenal: full-time report and player ratings",
        "Player ratings: who shone and who struggled",
        "Match report: Reds edge thriller at Anfield",
        "Liverpool vs Chelsea: live updates as it happened",
        "Half-time: Liverpool trail after early goal",
        "Predicted XI and team news ahead of kick-off",
        "WATCH: all the goals from a 3–3 draw",
        "Salah penalty seals win over rivals",
        "Highlights: Liverpool come from behind",
        "Liverpool v Everton preview",
        "Red card overshadows derby defeat",
    )

    // Headlines that MUST NOT be flagged (transfer/coaching news).
    private val nonMatchHeadlines = listOf(
        "Liverpool agree personal terms with target midfielder",
        "New set-piece coach expected to join the backroom staff",
        "Academy graduate signs new long-term deal",
        "Reds 'eyeing' shock swoop for La Liga forward, claim reports",
        "New head of fitness lined up in staff reshuffle",
        "Fabrizio Romano: here we go for £40m midfield deal",
        "Liverpool complete signing of left-back on five-year contract",
        "Club confirm appointment of new assistant manager",
    )

    @Test
    fun `flags all match headlines (100% recall)`() {
        val leaks = matchHeadlines.filterNot { subject.isMatch(it) }
        assertThat(leaks).isEmpty()
    }

    @Test
    fun `does not flag transfer or coaching headlines`() {
        val falsePositives = nonMatchHeadlines.filter { subject.isMatch(it) }
        assertThat(falsePositives).isEmpty()
    }

    @Test
    fun `detects scorelines with en-dash and hyphen`() {
        assertThat(subject.isMatch("Reds win 2-0")).isTrue()
        assertThat(subject.isMatch("Thriller ends 3–3")).isTrue()
    }

    @Test
    fun `bare token v and ft only match on word boundaries`() {
        assertThat(subject.isMatch("Liverpool v Everton")).isTrue()
        assertThat(subject.isMatch("Transfer of the season")).isFalse() // 'ft' inside 'transfer' must not match
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*MatchClassifierTest"` — Expected: FAIL (unresolved `MatchClassifier` / `defaultClassifierConfig`).

- [ ] **Step 3: Create `TextNormalizer.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

object TextNormalizer {
    private val htmlTag = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")

    fun normalize(raw: String): String =
        htmlTag.replace(raw, " ")
            .replace(whitespace, " ")
            .trim()
            .lowercase()
}
```

- [ ] **Step 4: Create `ClassifierConfig.kt`** (all lists are data):

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.domain.model.Category

data class ClassifierConfig(
    val matchSignals: List<String>,
    val scorelineRegex: Regex,
    val transferContextAllowlist: List<String>,
    /** Ordered by priority: first category with a hit wins. */
    val categoryKeywords: List<Pair<Category, List<String>>>,
)

val defaultClassifierConfig = ClassifierConfig(
    matchSignals = listOf(
        "full-time", "full time", "half-time", "half time", "ft", "ht",
        "kick-off", "kickoff", "line-up", "line-ups", "lineup", "lineups", "xi",
        "player ratings", "ratings", "match report", "as it happened", "live",
        "final score", "scoreline", "draw", "win", "won", "lose", "loss", "lost",
        "defeat", "beat", "goal", "goals", "penalty", "red card", "sent off",
        "preview", "watch:", "highlights", "vs", "v",
    ),
    scorelineRegex = Regex("""\b\d+\s*[-–—]\s*\d+\b"""),
    transferContextAllowlist = emptyList(), // recall-first; populate only if fixtures demand
    categoryKeywords = listOf(
        Category.TRANSFER to listOf(
            "transfer", "sign", "signing", "signed", "deal", "fee", "bid", "medical",
            "agree", "agreed", "loan", "target", "linked", "swoop", "here we go", "release clause",
        ),
        Category.COACHING to listOf(
            "appoint", "appointed", "appoints", "sacked", "new manager", "new coach",
            "assistant", "sporting director", "head coach",
        ),
        Category.CONTRACT to listOf("contract", "new deal", "extension", "renew", "terms"),
        Category.BACKROOM to listOf(
            "backroom", "head of fitness", "set-piece coach", "staff reshuffle",
            "sports science", "recruitment",
        ),
        Category.ACADEMY to listOf("academy", "u18", "u21", "u23", "youth", "graduate", "prospect"),
    ),
)
```

- [ ] **Step 5: Create `MatchClassifier.kt`** (precompiled word-boundary regexes):

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

class MatchClassifier(private val config: ClassifierConfig) {

    private val signalRegexes: List<Regex> = config.matchSignals.map { signal ->
        val escaped = Regex.escape(signal)
        val prefix = if (signal.first().isLetterOrDigit()) "\\b" else ""
        val suffix = if (signal.last().isLetterOrDigit()) "\\b" else ""
        Regex(prefix + escaped + suffix)
    }

    private val allowlistRegexes: List<Regex> = config.transferContextAllowlist.map { Regex(Regex.escape(it)) }

    /** @param text raw title + " " + description; normalized internally. */
    fun isMatch(text: String): Boolean {
        val t = TextNormalizer.normalize(text)
        if (config.scorelineRegex.containsMatchIn(t)) return true
        val hit = signalRegexes.any { it.containsMatchIn(t) }
        if (!hit) return false
        // A weak hit can be rescued by the (default-empty) transfer-context allowlist.
        return allowlistRegexes.none { it.containsMatchIn(t) }
    }
}
```

- [ ] **Step 6: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*MatchClassifierTest"` — Expected: PASS. If any match headline leaks, add the missing signal to `matchSignals` (data) until recall is 100%.

- [ ] **Step 7: ✅ Checkpoint.**

---

## Task 4: Categoriser (TDD)

**Files:**
- Create: `data/news/classifier/Categoriser.kt`
- Test: `data/news/classifier/CategoriserTest.kt`

- [ ] **Step 1: Write the failing test**:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Category
import org.junit.Test

class CategoriserTest {
    private val subject = Categoriser(defaultClassifierConfig)

    @Test fun `match content is MATCH regardless of keywords`() {
        assertThat(subject.categorise("Liverpool sign new XI match report", isMatch = true))
            .isEqualTo(Category.MATCH)
    }

    @Test fun `transfer wins by priority`() {
        assertThat(subject.categorise("Liverpool agree fee to sign midfielder", isMatch = false))
            .isEqualTo(Category.TRANSFER)
    }

    @Test fun `coaching detected`() {
        assertThat(subject.categorise("Club appoints new assistant manager", isMatch = false))
            .isEqualTo(Category.COACHING)
    }

    @Test fun `academy detected`() {
        assertThat(subject.categorise("U18 graduate promoted to first team", isMatch = false))
            .isEqualTo(Category.ACADEMY)
    }

    @Test fun `no keyword hit falls back to OTHER`() {
        assertThat(subject.categorise("Anfield atmosphere feature piece", isMatch = false))
            .isEqualTo(Category.OTHER)
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*CategoriserTest"` — Expected: FAIL (unresolved `Categoriser`).

- [ ] **Step 3: Create `Categoriser.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.domain.model.Category

class Categoriser(private val config: ClassifierConfig) {

    private val regexesByCategory: List<Pair<Category, List<Regex>>> =
        config.categoryKeywords.map { (cat, words) ->
            cat to words.map { Regex("\\b" + Regex.escape(it)) } // prefix-boundary; allows sign/signing/signed
        }

    fun categorise(text: String, isMatch: Boolean): Category {
        if (isMatch) return Category.MATCH
        val t = TextNormalizer.normalize(text)
        for ((cat, regexes) in regexesByCategory) {
            if (regexes.any { it.containsMatchIn(t) }) return cat
        }
        return Category.OTHER
    }
}
```

- [ ] **Step 4: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*CategoriserTest"` — Expected: PASS.

- [ ] **Step 5: ✅ Checkpoint.**

---

## Task 5: TierConfig + TierResolver (TDD)

**Files:**
- Create: `data/news/tier/TierConfig.kt`
- Create: `data/news/tier/TierResolver.kt`
- Test: `data/news/tier/TierResolverTest.kt`

- [ ] **Step 1: Write the failing test**:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class TierResolverTest {
    private val subject = TierResolver(defaultTierConfig)

    @Test fun `tier-1 journalist bumps tier up`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "David Ornstein", text = "deal agreed")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
    }

    @Test fun `known aggregator forced to tier-3`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "Daily Mirror", text = "shock swoop")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `falls back to provided default`() {
        val (tier, _) = subject.resolve(defaultTier = Tier.TIER_2_BEAT, author = "This Is Anfield", text = "report")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
    }

    @Test fun `here we go from tier-1 marks confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Here we go! Deal done")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `here we go without tier-1 is not confirmed`() {
        val (_, confirmed) = subject.resolve(Tier.TIER_3_SPECULATIVE, "Daily Mirror", "here we go claim")
        assertThat(confirmed).isFalse()
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*TierResolverTest"` — Expected: FAIL.

- [ ] **Step 3: Create `TierConfig.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

data class TierConfig(
    val tier1Journalists: Set<String>,
    val tier3Sources: Set<String>,
    val confirmedPhrase: String,
)

val defaultTierConfig = TierConfig(
    tier1Journalists = setOf("david ornstein", "fabrizio romano"),
    tier3Sources = setOf("daily mirror", "the sun", "the mirror", "mirror", "sun"),
    confirmedPhrase = "here we go",
)
```

- [ ] **Step 4: Create `TierResolver.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

import dev.crushingdigits.littlebird.data.news.classifier.TextNormalizer
import dev.crushingdigits.littlebird.domain.model.Tier

class TierResolver(private val config: TierConfig) {

    /** @return resolved tier + whether the item is a confirmed ("here we go") story. */
    fun resolve(defaultTier: Tier, author: String?, text: String): Pair<Tier, Boolean> {
        val haystack = TextNormalizer.normalize((author.orEmpty()) + " " + text)
        val authorNorm = author?.let { TextNormalizer.normalize(it) }.orEmpty()

        val tier = when {
            config.tier1Journalists.any { haystack.contains(it) } -> Tier.TIER_1_RELIABLE
            config.tier3Sources.any { authorNorm.contains(it) } -> Tier.TIER_3_SPECULATIVE
            else -> defaultTier
        }
        val confirmed = tier == Tier.TIER_1_RELIABLE && haystack.contains(config.confirmedPhrase)
        return tier to confirmed
    }
}
```

- [ ] **Step 5: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*TierResolverTest"` — Expected: PASS.

- [ ] **Step 6: ✅ Checkpoint.**

---

## Task 6: NewsClassifier (composes 3–5) (TDD)

**Files:**
- Create: `data/news/classifier/NewsClassifier.kt`
- Test: `data/news/classifier/NewsClassifierTest.kt`

- [ ] **Step 1: Write the failing test**:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class NewsClassifierTest {
    private val subject = NewsClassifier(
        matchClassifier = MatchClassifier(defaultClassifierConfig),
        categoriser = Categoriser(defaultClassifierConfig),
        tierResolver = TierResolver(defaultTierConfig),
    )

    @Test fun `confirmed transfer from tier-1 journalist`() {
        val c = subject.classify(
            title = "Liverpool agree deal for midfielder",
            description = "Here we go! Personal terms agreed.",
            author = "Fabrizio Romano",
            defaultTier = Tier.TIER_2_BEAT,
        )
        assertThat(c.isMatch).isFalse()
        assertThat(c.category).isEqualTo(Category.TRANSFER)
        assertThat(c.tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(c.confirmed).isTrue()
    }

    @Test fun `match content is flagged and categorised MATCH`() {
        val c = subject.classify(
            title = "Player ratings: 2-1 win",
            description = "Full-time report.",
            author = "BBC Sport",
            defaultTier = Tier.TIER_2_BEAT,
        )
        assertThat(c.isMatch).isTrue()
        assertThat(c.category).isEqualTo(Category.MATCH)
        assertThat(c.confirmed).isFalse()
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsClassifierTest"` — Expected: FAIL.

- [ ] **Step 3: Create `NewsClassifier.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier

class NewsClassifier(
    private val matchClassifier: MatchClassifier,
    private val categoriser: Categoriser,
    private val tierResolver: TierResolver,
) {
    fun classify(title: String, description: String, author: String?, defaultTier: Tier): Classification {
        val text = "$title $description"
        val isMatch = matchClassifier.isMatch(text)
        val category = categoriser.categorise(text, isMatch)
        val (tier, confirmed) = tierResolver.resolve(defaultTier, author, text)
        return Classification(isMatch = isMatch, category = category, tier = tier, confirmed = confirmed)
    }
}
```

- [ ] **Step 4: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsClassifierTest"` — Expected: PASS.

- [ ] **Step 5: ✅ Checkpoint.**

---

## Task 7: FeedSource config

**Files:**
- Create: `data/news/source/FeedSource.kt`, `data/news/source/FeedSources.kt`

No behavior → verified by compilation.

- [ ] **Step 1: Create `FeedSource.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.source

import dev.crushingdigits.littlebird.domain.model.Tier

enum class FeedKind { RSS, ATOM }

data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val defaultTier: Tier,
    val kind: FeedKind = FeedKind.RSS,
)
```

- [ ] **Step 2: Create `FeedSources.kt`** (URLs verified at build time per spec §12):

```kotlin
package dev.crushingdigits.littlebird.data.news.source

import dev.crushingdigits.littlebird.domain.model.Tier

object FeedSources {
    val all: List<FeedSource> = listOf(
        FeedSource("this_is_anfield", "This Is Anfield", "https://www.thisisanfield.com/feed/", Tier.TIER_2_BEAT),
        FeedSource("tia_transfers", "This Is Anfield — Transfers", "https://www.thisisanfield.com/liverpool-fc-transfer-rumours/feed/", Tier.TIER_2_BEAT),
        FeedSource("liverpool_echo", "Liverpool Echo", "https://www.liverpool.com/all-about/liverpool-fc?service=rss", Tier.TIER_2_BEAT),
        FeedSource("bbc_liverpool", "BBC Sport — Liverpool", "https://feeds.bbci.co.uk/sport/football/teams/liverpool/rss.xml", Tier.TIER_2_BEAT),
    )
}
```

- [ ] **Step 3: Compile** — Run: `./gradlew :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.

---

## Task 8: RawFeedItem + FeedParser (RSS 2.0 + Atom) (TDD, Robolectric)

`android.util.Xml` is used in production; tests run under Robolectric so it resolves on the JVM.

**Files:**
- Create: `data/news/datasource/remote/RawFeedItem.kt`, `data/news/datasource/remote/FeedParser.kt`
- Test: `data/news/datasource/remote/FeedParserTest.kt`
- Test resources: `app/src/test/resources/feeds/rss_sample.xml`, `app/src/test/resources/feeds/atom_sample.xml`

- [ ] **Step 1: Create test fixtures.** `app/src/test/resources/feeds/rss_sample.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/">
  <channel>
    <title>This Is Anfield</title>
    <item>
      <title><![CDATA[Liverpool agree deal for midfielder]]></title>
      <link>https://example.com/a</link>
      <guid>https://example.com/a</guid>
      <pubDate>Mon, 01 Jun 2026 09:00:00 +0000</pubDate>
      <dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">David Ornstein</dc:creator>
      <description><![CDATA[<p>Here we go! Personal terms agreed.</p>]]></description>
      <media:content url="https://example.com/a.jpg" />
    </item>
    <item>
      <title>Player ratings: 2-1 win</title>
      <link>https://example.com/b</link>
      <pubDate>Mon, 01 Jun 2026 08:00:00 +0000</pubDate>
      <description>Full-time report.</description>
    </item>
  </channel>
</rss>
```

`app/src/test/resources/feeds/atom_sample.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>BBC Sport - Liverpool</title>
  <entry>
    <title>New set-piece coach lined up</title>
    <link href="https://example.com/c" />
    <id>tag:example.com,2026:/c</id>
    <updated>2026-06-01T07:00:00Z</updated>
    <author><name>BBC Sport</name></author>
    <summary>Backroom reshape continues.</summary>
  </entry>
</feed>
```

- [ ] **Step 2: Write the failing test** `FeedParserTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedParserTest {
    private val subject = FeedParser()

    private fun fixture(name: String): String =
        javaClass.classLoader!!.getResourceAsStream("feeds/$name")!!.bufferedReader().use { it.readText() }

    @Test fun `parses RSS 2_0 items with cdata, creator and media image`() {
        val items = subject.parse(fixture("rss_sample.xml"), sourceId = "tia")

        assertThat(items).hasSize(2)
        val first = items[0]
        assertThat(first.title).isEqualTo("Liverpool agree deal for midfielder")
        assertThat(first.link).isEqualTo("https://example.com/a")
        assertThat(first.guid).isEqualTo("https://example.com/a")
        assertThat(first.author).isEqualTo("David Ornstein")
        assertThat(first.description).contains("Here we go")
        assertThat(first.imageUrl).isEqualTo("https://example.com/a.jpg")
        assertThat(first.pubDate).isNotNull()
    }

    @Test fun `parses Atom entries`() {
        val items = subject.parse(fixture("atom_sample.xml"), sourceId = "bbc")
        assertThat(items).hasSize(1)
        assertThat(items[0].title).isEqualTo("New set-piece coach lined up")
        assertThat(items[0].link).isEqualTo("https://example.com/c")
        assertThat(items[0].author).isEqualTo("BBC Sport")
    }
}
```

- [ ] **Step 3: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*FeedParserTest"` — Expected: FAIL (unresolved `FeedParser`).

- [ ] **Step 4: Create `RawFeedItem.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

data class RawFeedItem(
    val sourceId: String,
    val title: String,
    val link: String,
    val description: String,
    val pubDate: String?,
    val author: String?,
    val categories: List<String>,
    val imageUrl: String?,
    val guid: String?,
)
```

- [ ] **Step 5: Create `FeedParser.kt`** — auto-detects RSS vs Atom, handles CDATA, `dc:creator`, `media:content`/`enclosure`:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

class FeedParser {

    fun parse(xml: String, sourceId: String): List<RawFeedItem> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(StringReader(xml))
        }
        val items = mutableListOf<RawFeedItem>()
        var event = parser.eventType
        var current: MutableMap<String, String>? = null
        var inItem = false

        while (event != XmlPullParser.END_DOCUMENT) {
            val name = if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) parser.name else null
            when (event) {
                XmlPullParser.START_TAG -> when (name) {
                    "item", "entry" -> { inItem = true; current = mutableMapOf() }
                    "title", "link", "guid", "id", "pubDate", "published", "updated",
                    "description", "summary", "content", "dc:creator", "author", "name" ->
                        if (inItem && current != null) {
                            // Atom <link href="..."> carries the URL as an attribute.
                            val href = parser.getAttributeValue(null, "href")
                            if (name == "link" && href != null) current["link"] = href
                            else current[name!!] = readText(parser)
                        }
                    "media:content", "enclosure" ->
                        if (inItem && current != null) {
                            parser.getAttributeValue(null, "url")?.let { current["image"] = it }
                        }
                    "category" -> if (inItem && current != null) {
                        val c = readText(parser)
                        if (c.isNotBlank()) current["category"] = (current["category"]?.plus("|") ?: "") + c
                    }
                }
                XmlPullParser.END_TAG -> if (name == "item" || name == "entry") {
                    current?.let { items += it.toRawFeedItem(sourceId) }
                    inItem = false; current = null
                }
            }
            event = parser.next()
        }
        return items
    }

    /** Reads the text of the current element (handles CDATA, which the pull parser surfaces as TEXT). */
    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType == XmlPullParser.TEXT || parser.eventType == XmlPullParser.CDSECT) {
                sb.append(parser.text)
            } else if (parser.eventType == XmlPullParser.START_TAG) {
                // skip nested tags (e.g. Atom <author><name>)
            }
        }
        return sb.toString().trim()
    }

    private fun Map<String, String>.toRawFeedItem(sourceId: String) = RawFeedItem(
        sourceId = sourceId,
        title = (this["title"] ?: "").trim(),
        link = (this["link"] ?: this["id"] ?: "").trim(),
        description = (this["description"] ?: this["summary"] ?: this["content"] ?: "").trim(),
        pubDate = this["pubDate"] ?: this["published"] ?: this["updated"],
        author = this["dc:creator"] ?: this["author"] ?: this["name"],
        categories = this["category"]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
        imageUrl = this["image"],
        guid = this["guid"] ?: this["id"],
    )
}
```

> Note on Atom `<author><name>`: with namespaces off, the inner `<name>` is captured into `name`. The `readText` skip-nested guard prevents the outer `<author>` from swallowing it. If a captured-real-feed fixture reveals a quirk, extend the fixtures + parser together (TDD).

- [ ] **Step 6: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*FeedParserTest"` — Expected: PASS.

- [ ] **Step 7: ✅ Checkpoint.**

---

## Task 9: FeedFetcher (OkHttp) (TDD, MockWebServer)

**Files:**
- Create: `data/news/datasource/remote/FeedFetcher.kt`
- Test: `data/news/datasource/remote/FeedFetcherTest.kt`

- [ ] **Step 1: Write the failing test**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class FeedFetcherTest {
    private val server = MockWebServer()
    private lateinit var subject: FeedFetcher

    @Before fun setUp() { server.start(); subject = FeedFetcher(OkHttpClient()) }
    @After fun tearDown() { server.shutdown() }

    @Test fun `returns body on 200`() = runTest {
        server.enqueue(MockResponse().setBody("<rss></rss>").setResponseCode(200))
        val result = subject.fetch(server.url("/feed").toString())
        assertThat(result.getOrNull()).isEqualTo("<rss></rss>")
    }

    @Test fun `returns failure on error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val result = subject.fetch(server.url("/feed").toString())
        assertThat(result.isFailure).isTrue()
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*FeedFetcherTest"` — Expected: FAIL.

- [ ] **Step 3: Create `FeedFetcher.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class FeedFetcher(private val client: OkHttpClient) {

    suspend fun fetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).header("User-Agent", "ALittleBird/1.0").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code} for $url")
                response.body?.string() ?: throw IOException("Empty body for $url")
            }
        }
    }
}
```

- [ ] **Step 4: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*FeedFetcherTest"` — Expected: PASS.

- [ ] **Step 5: ✅ Checkpoint.**

---

## Task 10: NewsRemoteDataSource (TDD)

The network source: for each `FeedSource`, fetch + parse → `RawFeedItem`s. Resilient — one feed failing does not sink the others.

**Files:**
- Create: `data/news/datasource/remote/NewsRemoteDataSource.kt`
- Test: `data/news/datasource/remote/NewsRemoteDataSourceTest.kt`

- [ ] **Step 1: Write the failing test** (uses a fake fetcher):

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NewsRemoteDataSourceTest {

    private val rss = """
        <rss version="2.0"><channel>
          <item><title>Deal agreed</title><link>https://x/a</link><description>terms</description></item>
        </channel></rss>
    """.trimIndent()

    private class FakeFetcher(val responses: Map<String, Result<String>>) : FeedFetcher(okhttp3.OkHttpClient()) {
        override suspend fun fetch(url: String): Result<String> =
            responses[url] ?: Result.failure(IllegalStateException("no stub for $url"))
    }

    private val sourceA = FeedSource("a", "A", "https://x/a-feed", Tier.TIER_2_BEAT)
    private val sourceB = FeedSource("b", "B", "https://x/b-feed", Tier.TIER_2_BEAT)

    @Test fun `aggregates parsed items across sources, skipping failed feeds`() = runTest {
        val fetcher = FakeFetcher(
            mapOf(
                sourceA.url to Result.success(rss),
                sourceB.url to Result.failure(RuntimeException("down")),
            ),
        )
        val subject = NewsRemoteDataSource(fetcher, FeedParser())

        val items = subject.fetchAll(listOf(sourceA, sourceB))

        assertThat(items).hasSize(1)
        assertThat(items[0].sourceId).isEqualTo("a")
        assertThat(items[0].title).isEqualTo("Deal agreed")
    }
}
```

> Make `FeedFetcher.fetch` `open` (Task 9) so the fake can override it, or extract a `FeedFetcher` interface. Simplest: add `open` to the class and method.

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRemoteDataSourceTest"` — Expected: FAIL.

- [ ] **Step 3: Make `FeedFetcher` open**, then create `NewsRemoteDataSource.kt`:

In `FeedFetcher.kt` change `class FeedFetcher` → `open class FeedFetcher` and `suspend fun fetch` → `open suspend fun fetch`.

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.remote

import dev.crushingdigits.littlebird.data.news.source.FeedSource

class NewsRemoteDataSource(
    private val fetcher: FeedFetcher,
    private val parser: FeedParser,
) {
    /** Fetches + parses every source. A failed feed is skipped, not fatal. */
    suspend fun fetchAll(sources: List<FeedSource>): List<RawFeedItem> =
        sources.flatMap { source ->
            fetcher.fetch(source.url)
                .map { xml -> parser.parse(xml, source.id) }
                .getOrElse { emptyList() }
        }
}
```

- [ ] **Step 4: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRemoteDataSourceTest"` — Expected: PASS.

- [ ] **Step 5: ✅ Checkpoint.**

---

## Task 11: Room — Entity, DAO, Database (TDD DAO, Robolectric)

**Files:**
- Create: `data/news/datasource/local/NewsItemEntity.kt`, `NewsDao.kt`, `LittleBirdDatabase.kt`
- Test: `data/news/datasource/local/NewsDaoTest.kt`

- [ ] **Step 1: Create `NewsItemEntity.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_items")
data class NewsItemEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val sourceName: String,
    val tier: String,        // Tier.name
    val category: String,    // Category.name
    val isMatch: Boolean,
    val confirmed: Boolean,
    val author: String?,
    val role: String?,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String?,
    val publishedAtEpochMs: Long,
)
```

- [ ] **Step 2: Create `NewsDao.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_items ORDER BY publishedAtEpochMs DESC")
    fun observeAll(): Flow<List<NewsItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NewsItemEntity>)

    @Query("SELECT COUNT(*) FROM news_items")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Create `LittleBirdDatabase.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NewsItemEntity::class], version = 1, exportSchema = false)
abstract class LittleBirdDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
}
```

- [ ] **Step 4: Write the failing DAO test** `NewsDaoTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NewsDaoTest {
    private lateinit var db: LittleBirdDatabase
    private lateinit var dao: NewsDao

    private fun entity(id: String, at: Long) = NewsItemEntity(
        id = id, sourceId = "a", sourceName = "A", tier = "TIER_2_BEAT", category = "TRANSFER",
        isMatch = false, confirmed = false, author = null, role = null, title = "t$id",
        summary = "s", link = "https://x/$id", imageUrl = null, publishedAtEpochMs = at,
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LittleBirdDatabase::class.java).build()
        dao = db.newsDao()
    }
    @After fun tearDown() = db.close()

    @Test fun `upsert replaces by id and observeAll is newest-first`() = runTest {
        dao.upsertAll(listOf(entity("1", at = 100), entity("2", at = 200)))
        dao.upsertAll(listOf(entity("1", at = 100))) // duplicate id → replaced, not duplicated

        dao.observeAll().test {
            val rows = awaitItem()
            assertThat(rows.map { it.id }).containsExactly("2", "1").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dao.count()).isEqualTo(2)
    }
}
```

- [ ] **Step 5: Run to verify it fails then passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsDaoTest"` — Expected: FAIL before Steps 1–3 exist, PASS after. (Steps 1–3 already written; this confirms schema + KSP codegen.)

- [ ] **Step 6: ✅ Checkpoint.**

---

## Task 12: Mappers + NewsLocalDataSource (TDD)

**Files:**
- Create: `data/news/mapper/NewsMappers.kt`
- Create: `data/news/datasource/local/NewsLocalDataSource.kt`
- Test: `data/news/mapper/NewsMappersTest.kt`

- [ ] **Step 1: Write the failing test** `NewsMappersTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.mapper

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class NewsMappersTest {

    @Test fun `raw + classification maps to entity with stable id from guid`() {
        val raw = RawFeedItem(
            sourceId = "tia", title = "Deal agreed", link = "https://x/a", description = "<p>terms</p>",
            pubDate = "Mon, 01 Jun 2026 09:00:00 +0000", author = "David Ornstein",
            categories = emptyList(), imageUrl = null, guid = "https://x/a",
        )
        val c = Classification(false, Category.TRANSFER, Tier.TIER_1_RELIABLE, confirmed = true)

        val entity = raw.toEntity(sourceName = "This Is Anfield", classification = c)

        assertThat(entity.id).isEqualTo("https://x/a")
        assertThat(entity.tier).isEqualTo("TIER_1_RELIABLE")
        assertThat(entity.category).isEqualTo("TRANSFER")
        assertThat(entity.confirmed).isTrue()
        assertThat(entity.summary).doesNotContain("<p>") // HTML stripped for display
        assertThat(entity.publishedAtEpochMs).isGreaterThan(0L)
    }

    @Test fun `entity maps back to domain NewsItem`() {
        val raw = RawFeedItem("tia", "T", "https://x/a", "d", null, null, emptyList(), null, null)
        val entity = raw.toEntity("A", Classification(true, Category.MATCH, Tier.TIER_2_BEAT, false))
        val item = entity.toDomain()
        assertThat(item.category).isEqualTo(Category.MATCH)
        assertThat(item.tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(item.id).isEqualTo(entity.id)
    }
}
```

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsMappersTest"` — Expected: FAIL.

- [ ] **Step 3: Create `NewsMappers.kt`** (id strategy + RFC-822/ISO date parsing + HTML strip):

```kotlin
package dev.crushingdigits.littlebird.data.news.mapper

import dev.crushingdigits.littlebird.data.news.classifier.TextNormalizer
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsItemEntity
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val htmlTag = Regex("<[^>]+>")
private val rfc822 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)

private fun parseDate(raw: String?): Instant = raw?.trim()?.let {
    runCatching { OffsetDateTime.parse(it, rfc822).toInstant() }
        .recoverCatching { OffsetDateTime.parse(it).toInstant() } // ISO-8601 (Atom)
        .getOrNull()
} ?: Instant.EPOCH

private fun stableId(raw: RawFeedItem): String =
    raw.guid?.takeIf { it.isNotBlank() }
        ?: raw.link.takeIf { it.isNotBlank() }
        ?: (raw.sourceId + "|" + raw.title + "|" + raw.pubDate).hashCode().toString()

fun RawFeedItem.toEntity(sourceName: String, classification: Classification): NewsItemEntity = NewsItemEntity(
    id = stableId(this),
    sourceId = sourceId,
    sourceName = sourceName,
    tier = classification.tier.name,
    category = classification.category.name,
    isMatch = classification.isMatch,
    confirmed = classification.confirmed,
    author = author,
    role = null,
    title = title,
    summary = htmlTag.replace(description, "").let { TextNormalizer.normalize(it) }.let { description.stripHtml() },
    link = link,
    imageUrl = imageUrl,
    publishedAtEpochMs = parseDate(pubDate).toEpochMilli(),
)

private fun String.stripHtml(): String = htmlTag.replace(this, "").replace(Regex("\\s+"), " ").trim()

fun NewsItemEntity.toDomain(): NewsItem = NewsItem(
    id = id,
    source = sourceName,
    tier = Tier.valueOf(tier),
    category = Category.valueOf(category),
    isMatch = isMatch,
    confirmed = confirmed,
    author = author,
    role = role,
    title = title,
    summary = summary,
    link = link,
    imageUrl = imageUrl,
    publishedAt = Instant.ofEpochMilli(publishedAtEpochMs),
)
```

> Clean the `summary` line to just `summary = description.stripHtml(),` (the inlined experiment above is redundant — keep only `description.stripHtml()`).

- [ ] **Step 4: Create `NewsLocalDataSource.kt`**:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import kotlinx.coroutines.flow.Flow

class NewsLocalDataSource(private val dao: NewsDao) {
    fun observeAll(): Flow<List<NewsItemEntity>> = dao.observeAll()
    suspend fun upsert(items: List<NewsItemEntity>) = dao.upsertAll(items)
    suspend fun count(): Int = dao.count()
}
```

- [ ] **Step 5: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsMappersTest"` — Expected: PASS.

- [ ] **Step 6: ✅ Checkpoint.**

---

## Task 13: PreferencesDataSource + PreferencesRepository (TDD, Robolectric)

**Files:**
- Create: `data/preferences/datasource/PreferencesDataSource.kt`
- Create: `data/preferences/PreferencesRepository.kt`
- Test: `data/preferences/PreferencesRepositoryTest.kt`

- [ ] **Step 1: Create `PreferencesDataSource.kt`** (DataStore-backed):

```kotlin
package dev.crushingdigits.littlebird.data.preferences.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataSource(private val dataStore: DataStore<Preferences>) {

    private val protectionOnKey = booleanPreferencesKey("protection_on")
    private val revealedIdsKey = stringSetPreferencesKey("revealed_ids")

    val protectionOn: Flow<Boolean> = dataStore.data.map { it[protectionOnKey] ?: true } // default ON
    val revealedIds: Flow<Set<String>> = dataStore.data.map { it[revealedIdsKey] ?: emptySet() }

    suspend fun setProtectionOn(on: Boolean) { dataStore.edit { it[protectionOnKey] = on } }
    suspend fun setRevealedIds(ids: Set<String>) { dataStore.edit { it[revealedIdsKey] = ids } }
}
```

- [ ] **Step 2: Write the failing test** `PreferencesRepositoryTest.kt` (uses a real DataStore in a temp dir under Robolectric):

```kotlin
package dev.crushingdigits.littlebird.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Context.testDataStore: DataStore<Preferences> by preferencesDataStore(name = "test_prefs")

@RunWith(RobolectricTestRunner::class)
class PreferencesRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val subject = PreferencesRepositoryImpl(PreferencesDataSource(context.testDataStore))

    @Test fun `protection defaults on and toggles`() = runTest {
        subject.observeProtection().test {
            assertThat(awaitItem()).isTrue()
            subject.setProtection(false)
            assertThat(awaitItem()).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `reveal adds id and markWatched adds many`() = runTest {
        subject.reveal("a")
        subject.revealAll(setOf("b", "c"))
        subject.observeRevealed().test {
            val ids = awaitItem()
            assertThat(ids).containsAtLeast("a", "b", "c")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 3: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*PreferencesRepositoryTest"` — Expected: FAIL.

- [ ] **Step 4: Create `PreferencesRepository.kt`** (interface + impl):

```kotlin
package dev.crushingdigits.littlebird.data.preferences

import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface PreferencesRepository {
    fun observeProtection(): Flow<Boolean>
    fun observeRevealed(): Flow<Set<String>>
    suspend fun setProtection(on: Boolean)
    suspend fun toggleProtection()
    suspend fun reveal(id: String)
    suspend fun revealAll(ids: Set<String>)
    suspend fun restoreRevealed(ids: Set<String>)
}

class PreferencesRepositoryImpl(private val source: PreferencesDataSource) : PreferencesRepository {
    override fun observeProtection(): Flow<Boolean> = source.protectionOn
    override fun observeRevealed(): Flow<Set<String>> = source.revealedIds
    override suspend fun setProtection(on: Boolean) = source.setProtectionOn(on)
    override suspend fun toggleProtection() = source.setProtectionOn(!source.protectionOn.first())
    override suspend fun reveal(id: String) = source.setRevealedIds(source.revealedIds.first() + id)
    override suspend fun revealAll(ids: Set<String>) = source.setRevealedIds(source.revealedIds.first() + ids)
    override suspend fun restoreRevealed(ids: Set<String>) = source.setRevealedIds(ids)
}
```

- [ ] **Step 5: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*PreferencesRepositoryTest"` — Expected: PASS.

- [ ] **Step 6: ✅ Checkpoint.**

---

## Task 14: NewsRepository (TDD, fakes)

Coordinates remote + classifier + local. On refresh: fetch → classify → map → upsert. On observe: stream from Room (SSOT) mapped to domain. Refresh failure must not clear the cache.

**Files:**
- Create: `data/news/NewsRepository.kt`
- Test: `data/news/NewsRepositoryTest.kt`

- [ ] **Step 1: Write the failing test** with fakes for the two data sources:

```kotlin
package dev.crushingdigits.littlebird.data.news

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.classifier.Categoriser
import dev.crushingdigits.littlebird.data.news.classifier.MatchClassifier
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.classifier.defaultClassifierConfig
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsItemEntity
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.RawFeedItem
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NewsRepositoryTest {

    // Fake local source backed by an in-memory StateFlow.
    private class FakeLocal {
        val rows = MutableStateFlow<List<NewsItemEntity>>(emptyList())
        fun observeAll(): Flow<List<NewsItemEntity>> = rows
        suspend fun upsert(items: List<NewsItemEntity>) = rows.update { (it + items).associateBy { e -> e.id }.values.toList() }
    }

    private val classifier = NewsClassifier(
        MatchClassifier(defaultClassifierConfig), Categoriser(defaultClassifierConfig), TierResolver(defaultTierConfig),
    )

    private fun remoteReturning(items: List<RawFeedItem>) = object : NewsRemoteDataSource(
        fetcher = dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher(okhttp3.OkHttpClient()),
        parser = dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser(),
    ) {
        override suspend fun fetchAll(sources: List<FeedSource>): List<RawFeedItem> = items
    }

    private val source = FeedSource("tia", "This Is Anfield", "https://x/feed", Tier.TIER_2_BEAT)

    @Test fun `refresh classifies, maps and upserts; observeFeed emits domain`() = runTest {
        val raw = RawFeedItem("tia", "Liverpool agree deal", "https://x/a", "terms", null, "David Ornstein", emptyList(), null, "https://x/a")
        val local = FakeLocal()
        val subject = NewsRepositoryImpl(remoteReturning(listOf(raw)), local::observeAll, local::upsert, classifier, listOf(source))

        val result = subject.refresh()
        assertThat(result.isSuccess).isTrue()

        subject.observeFeed().test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items[0].category).isEqualTo(Category.TRANSFER)
            assertThat(items[0].tier).isEqualTo(Tier.TIER_1_RELIABLE)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `refresh failure keeps existing cache`() = runTest {
        val local = FakeLocal()
        local.upsert(listOf(NewsItemEntity("x","tia","TIA","TIER_2_BEAT","TRANSFER",false,false,null,null,"t","s","https://x/x",null,1)))
        val failingRemote = object : NewsRemoteDataSource(
            dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher(okhttp3.OkHttpClient()),
            dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser(),
        ) { override suspend fun fetchAll(sources: List<FeedSource>): List<RawFeedItem> = throw RuntimeException("offline") }

        val subject = NewsRepositoryImpl(failingRemote, local::observeAll, local::upsert, classifier, listOf(source))

        assertThat(subject.refresh().isFailure).isTrue()
        subject.observeFeed().test {
            assertThat(awaitItem()).hasSize(1) // cache intact
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

> The constructor here takes the local source's `observeAll`/`upsert` as function refs to keep the fake trivial. The production `NewsRepositoryImpl` (Step 3) instead takes `NewsLocalDataSource`; adjust the fake to subclass/wrap if you prefer — either is fine as long as the two tests pass. Keep the **interface** `NewsRepository` stable.

- [ ] **Step 2: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"` — Expected: FAIL.

- [ ] **Step 3: Create `NewsRepository.kt`** (production form takes the real data sources):

```kotlin
package dev.crushingdigits.littlebird.data.news

import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsItemEntity
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.mapper.toDomain
import dev.crushingdigits.littlebird.data.news.mapper.toEntity
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NewsRepository {
    fun observeFeed(): Flow<List<NewsItem>>
    suspend fun refresh(): Result<Unit>
    suspend fun isEmpty(): Boolean
}

class NewsRepositoryImpl(
    private val remote: NewsRemoteDataSource,
    private val local: NewsLocalDataSource,
    private val classifier: NewsClassifier,
    private val sources: List<FeedSource>,
) : NewsRepository {

    private val sourcesById = sources.associateBy { it.id }

    override fun observeFeed(): Flow<List<NewsItem>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val raw = remote.fetchAll(sources)
        val entities: List<NewsItemEntity> = raw.map { item ->
            val src = sourcesById[item.sourceId]
            val classification = classifier.classify(
                title = item.title,
                description = item.description,
                author = item.author,
                defaultTier = src?.defaultTier ?: dev.crushingdigits.littlebird.domain.model.Tier.TIER_3_SPECULATIVE,
            )
            item.toEntity(sourceName = src?.name ?: item.sourceId, classification = classification)
        }
        if (entities.isNotEmpty()) local.upsert(entities)
    }

    override suspend fun isEmpty(): Boolean = local.count() == 0
}
```

> Reconcile the Step-1 fake with this signature: change `FakeLocal` to subclass `NewsLocalDataSource` by extracting a tiny interface, **or** wrap a fake `NewsDao` (preferred — mirrors `NewsDaoTest`). Pick one and make both tests pass without changing the `NewsRepository` interface.

- [ ] **Step 4: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"` — Expected: PASS.

- [ ] **Step 5: ✅ Checkpoint.**

---

## Task 15: Use cases

Thin domain wrappers per the project's UseCase naming convention (interface = action name, impl = `…UseCase`). Stateless, single responsibility.

**Files:**
- Create: `domain/usecase/ObserveFeed.kt`, `RefreshFeed.kt`, `ObserveProtection.kt`, `ToggleProtection.kt`, `RevealItem.kt`, `MarkWatched.kt`
- Test: `domain/usecase/MarkWatchedTest.kt` (the only one with real logic — snapshot/undo)

- [ ] **Step 1: Create the thin pass-through use cases.** `ObserveFeed.kt`:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveFeed { operator fun invoke(): Flow<List<NewsItem>> }

class ObserveFeedUseCase @Inject constructor(private val repo: NewsRepository) : ObserveFeed {
    override fun invoke(): Flow<List<NewsItem>> = repo.observeFeed()
}
```

`RefreshFeed.kt`:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import javax.inject.Inject

fun interface RefreshFeed { suspend operator fun invoke(): Result<Unit> }

class RefreshFeedUseCase @Inject constructor(private val repo: NewsRepository) : RefreshFeed {
    override suspend fun invoke(): Result<Unit> = repo.refresh()
}
```

`ObserveProtection.kt`:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveProtection { operator fun invoke(): Flow<Boolean> }

class ObserveProtectionUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveProtection {
    override fun invoke(): Flow<Boolean> = prefs.observeProtection()
}
```

`ToggleProtection.kt`:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

fun interface ToggleProtection { suspend operator fun invoke() }

class ToggleProtectionUseCase @Inject constructor(private val prefs: PreferencesRepository) : ToggleProtection {
    override suspend fun invoke() = prefs.toggleProtection()
}
```

`RevealItem.kt`:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

fun interface RevealItem { suspend operator fun invoke(id: String) }

class RevealItemUseCase @Inject constructor(private val prefs: PreferencesRepository) : RevealItem {
    override suspend fun invoke(id: String) = prefs.reveal(id)
}
```

- [ ] **Step 2: Write the failing test** `MarkWatchedTest.kt` (snapshot for undo + reveal-all):

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MarkWatchedTest {

    private class FakePrefs : PreferencesRepository {
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        override fun observeProtection(): Flow<Boolean> = MutableStateFlow(true)
        override fun observeRevealed(): Flow<Set<String>> = revealed
        override suspend fun setProtection(on: Boolean) {}
        override suspend fun toggleProtection() {}
        override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
        override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
        override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
    }

    @Test fun `markWatched reveals all held ids and returns prior snapshot for undo`() = runTest {
        val prefs = FakePrefs().apply { revealed.value = setOf("already") }
        val subject = MarkWatchedUseCase(prefs)

        val snapshot = subject(heldIds = setOf("m1", "m2"))

        assertThat(snapshot).containsExactly("already")
        assertThat(prefs.revealed.first()).containsExactly("already", "m1", "m2")
    }
}
```

- [ ] **Step 3: Run to verify it fails** — Run: `./gradlew :app:testDebugUnitTest --tests "*MarkWatchedTest"` — Expected: FAIL.

- [ ] **Step 4: Create `MarkWatched.kt`**:

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Reveals all currently-held match ids; returns the prior revealed set so the caller can offer Undo. */
fun interface MarkWatched { suspend operator fun invoke(heldIds: Set<String>): Set<String> }

class MarkWatchedUseCase @Inject constructor(private val prefs: PreferencesRepository) : MarkWatched {
    override suspend fun invoke(heldIds: Set<String>): Set<String> {
        val snapshot = prefs.observeRevealed().first()
        prefs.revealAll(heldIds)
        return snapshot
    }
}
```

- [ ] **Step 5: Run to verify it passes** — Run: `./gradlew :app:testDebugUnitTest --tests "*MarkWatchedTest"` — Expected: PASS.

- [ ] **Step 6: ✅ Checkpoint.**

---

## Task 16: Hilt wiring + full verification

**Files:**
- Create: `di/NetworkModule.kt`, `di/DatabaseModule.kt`, `di/DataStoreModule.kt`, `di/DataBindingsModule.kt`

- [ ] **Step 1: Create `di/NetworkModule.kt`**:

```kotlin
package dev.crushingdigits.littlebird.di

import dev.crushingdigits.littlebird.data.news.classifier.Categoriser
import dev.crushingdigits.littlebird.data.news.classifier.MatchClassifier
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.classifier.defaultClassifierConfig
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.news.source.FeedSources
import dev.crushingdigits.littlebird.data.news.tier.TierResolver
import dev.crushingdigits.littlebird.data.news.tier.defaultTierConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun okHttp(): OkHttpClient = OkHttpClient.Builder().build()
    @Provides @Singleton fun fetcher(client: OkHttpClient) = FeedFetcher(client)
    @Provides @Singleton fun parser() = FeedParser()
    @Provides @Singleton fun remote(fetcher: FeedFetcher, parser: FeedParser) = NewsRemoteDataSource(fetcher, parser)
    @Provides @Singleton fun classifier() = NewsClassifier(
        MatchClassifier(defaultClassifierConfig), Categoriser(defaultClassifierConfig), TierResolver(defaultTierConfig),
    )
    @Provides @Singleton fun sources(): List<FeedSource> = FeedSources.all
}
```

- [ ] **Step 2: Create `di/DatabaseModule.kt`**:

```kotlin
package dev.crushingdigits.littlebird.di

import android.content.Context
import androidx.room.Room
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsDao
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton fun db(@ApplicationContext ctx: Context): LittleBirdDatabase =
        Room.databaseBuilder(ctx, LittleBirdDatabase::class.java, "little_bird.db").build()
    @Provides fun dao(db: LittleBirdDatabase): NewsDao = db.newsDao()
    @Provides @Singleton fun local(dao: NewsDao) = NewsLocalDataSource(dao)
}
```

- [ ] **Step 3: Create `di/DataStoreModule.kt`**:

```kotlin
package dev.crushingdigits.littlebird.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "little_bird_prefs")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton fun dataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
    @Provides @Singleton fun prefsSource(ds: DataStore<Preferences>) = PreferencesDataSource(ds)
}
```

- [ ] **Step 4: Create `di/DataBindingsModule.kt`** (bind repository interfaces; use cases are constructor-injected so they need no binding):

```kotlin
package dev.crushingdigits.littlebird.di

import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.news.NewsRepositoryImpl
import dev.crushingdigits.littlebird.data.news.classifier.NewsClassifier
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.news.datasource.remote.NewsRemoteDataSource
import dev.crushingdigits.littlebird.data.news.source.FeedSource
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepositoryImpl
import dev.crushingdigits.littlebird.data.preferences.datasource.PreferencesDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataBindingsModule {
    @Provides @Singleton fun newsRepository(
        remote: NewsRemoteDataSource, local: NewsLocalDataSource,
        classifier: NewsClassifier, sources: List<FeedSource>,
    ): NewsRepository = NewsRepositoryImpl(remote, local, classifier, sources)

    @Provides @Singleton fun preferencesRepository(source: PreferencesDataSource): PreferencesRepository =
        PreferencesRepositoryImpl(source)
}
```

- [ ] **Step 5: Run the full unit suite + build** —
Run: `./gradlew :app:testDebugUnitTest`
Expected: ALL PASS.
Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL (Hilt graph resolves end-to-end).

- [ ] **Step 6: Coverage pass** — Invoke the `/write-unit-tests` skill's **Workflow** section to run kover, identify uncovered lines in the data/domain packages, and close gaps. Then run detekt per project conventions.

- [ ] **Step 7: ✅ Checkpoint** (commit via `/submit-mr`).

---

## Self-review — spec coverage

| Spec section | Covered by |
|---|---|
| §3 packages by data type | Tasks 1–16 (file map matches spec tree) |
| §4 data model (Raw→Entity→domain, enums, stable id) | Tasks 2, 11, 12 |
| §5 feed sources config | Task 7 |
| §6 classifier (normalize, recall-first match, category, data-driven) | Tasks 3, 4, 6 |
| §7 tier + confirmed | Tasks 5, 6 |
| §8 prefs (protectionOn default ON, revealedIds, reveal/markWatched/undo) | Tasks 13, 15 |
| §8 offline (refresh failure keeps cache) | Task 14 |
| §10 unit tests (classifier 100% recall, parser fixtures, repo, Turbine/Truth/Robolectric/fakes) | every task; kover in Task 16 |

**Deferred to Plan 1B (UI):** `FeedViewModel`/`FeedUiState`/`FeedAction`, `FeedScreen` + components, gate/catch-up sheets, toast, theme, Roborazzi + UIAutomator. The `ObserveProtection`/`RevealItem`/`MarkWatched`/`ToggleProtection` use cases built here are consumed there.

**Known reconciliation points flagged inline** (engineer must resolve while keeping public interfaces stable): Task 12 Step 3 redundant `summary` line → keep `description.stripHtml()`; Task 14 fake-vs-production `NewsLocalDataSource` shape.
