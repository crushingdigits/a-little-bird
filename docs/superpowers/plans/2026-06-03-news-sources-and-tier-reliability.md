# News Sources & Reliability Tier — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three RSS sources (David Lynch, Fabrizio Romano via CaughtOffside, The Guardian — Liverpool), promote BBC & Guardian to a Tier-1 floor, make journalist reliability conditional on language (three-state), and filter non-club-scoped feeds down to Liverpool stories.

**Architecture:** Pure data-layer change. The single reliability tier is resolved from two factors — an institutional floor (`FeedSource.defaultTier`) and a per-item journalist override gated by language (`TierResolver`). A new `LiverpoolRelevanceFilter` drops off-topic items from feeds flagged `clubScoped = false`, applied in `NewsRepositoryImpl.refresh()` before classification. New sources are guarded by real captured fixtures (`FeedSourcesGuardTest`, already present).

**Tech Stack:** Kotlin, JUnit4, Truth, Robolectric (for the XML parser + DB tests), Hilt (DI wiring), Gradle. Tests run with `./gradlew :app:testDebugUnitTest`.

**Spec:** [docs/superpowers/specs/2026-06-03-news-sources-and-tier-reliability-design.md](../specs/2026-06-03-news-sources-and-tier-reliability-design.md)

---

## Conventions (read once)

- **Test runner:** `./gradlew :app:testDebugUnitTest` runs all unit tests. Single class: append `--tests "fully.qualified.ClassName"`. Wildcards allowed: `--tests "*FeedSourcesGuardTest"`.
- **Test style:** Truth (`assertThat(x).isEqualTo(y)`), JUnit4 (`@Test`), fakes over mocks. Plain JVM tests for pure logic; `@RunWith(RobolectricTestRunner::class)` only when `android.util.Xml` (the parser) or Room is involved.
- **Normalization:** `TextNormalizer.normalize(s)` lowercases, strips HTML tags, collapses whitespace. All config phrases/terms MUST be lowercase — matching happens on normalized text.
- **Commits:** raw `git` (this project does NOT use `/submit-mr`); branch is already `feat/news-sources-and-tier-reliability`. End each commit body with the Co-Authored-By trailer the harness appends.
- **No screenshot/UI tests:** nothing visual changes. `TierChip` already renders all three tiers; screenshot tests use static sample `FeedCard`s, not live feeds, so the BBC tier change cannot move a baseline. Per the Android test-selection matrix (data layer), unit tests only.

## File map

**Modify:**
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/tier/TierConfig.kt` — replace `confirmedPhrase: String` with `confirmationPhrases: Set<String>`; add `tentativePhrases: Set<String>`; add `"david lynch"`.
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/tier/TierResolver.kt` — new three-state precedence + institutional floor.
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSource.kt` — add `clubScoped: Boolean = true`.
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt` — BBC → T1; add Guardian, David Lynch, Fabrizio Romano.
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/NewsRepository.kt` — add `relevanceFilter` param + filter step in `refresh()`.
- `app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt` — provide `LiverpoolRelevanceFilter`.
- `app/src/main/java/dev/crushingdigits/littlebird/di/DataBindingsModule.kt` — inject filter into the repository.

**Create:**
- `app/src/main/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilter.kt`
- `app/src/test/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilterTest.kt`
- `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt`
- `app/src/test/resources/feeds/source_guardian_liverpool.xml`
- `app/src/test/resources/feeds/source_david_lynch.xml`
- `app/src/test/resources/feeds/source_fabrizio_romano.xml`

**Modify (tests):**
- `app/src/test/java/dev/crushingdigits/littlebird/data/news/tier/TierResolverTest.kt`
- `app/src/test/java/dev/crushingdigits/littlebird/data/news/NewsRepositoryTest.kt`

---

## Task 1: Three-state tier resolution by language

Rewrite the resolver so a trusted journalist is Tier 1 only with confirmation language, Tier 2 with tentative language; institutions keep their floor; tabloids drop to Tier 3.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/tier/TierConfig.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/tier/TierResolver.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/tier/TierResolverTest.kt`

- [ ] **Step 1: Replace the resolver test with the full matrix**

Overwrite `TierResolverTest.kt` with:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class TierResolverTest {
    private val subject = TierResolver(defaultTierConfig)

    @Test fun `trusted journalist with confirmation language is tier-1 and confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Ornstein", "deal agreed for midfielder")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `here we go from a trusted journalist marks confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Here we go! Deal done")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `david lynch is recognised as a trusted journalist`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Lynch", "Here we go, medical completed")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `trusted journalist with tentative language caps at tier-2`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "Fabrizio Romano", "Liverpool interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(confirmed).isFalse()
    }

    @Test fun `neutral journalist item falls back to the source floor`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "David Lynch", "why Liverpool sacked the head coach")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(confirmed).isFalse()
    }

    @Test fun `tabloid author is forced to tier-3`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "Daily Mirror", "shock swoop")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `tabloid name-dropping a journalist tentatively stays tier-3`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "The Sun", "Fabrizio Romano says Liverpool interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_3_SPECULATIVE)
    }

    @Test fun `tabloid is rescued to tier-1 only by a journalist confirmation`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_2_BEAT, "The Sun", "Fabrizio Romano: here we go, done deal")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `inherently reliable source is not downgraded by a tentative journalist quote`() {
        // Full name so the journalist IS detected; tentative language would cap a T2 source at T2,
        // but the T1 institutional floor must hold (exercises moreReliable).
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "David Ornstein: Liverpool are interested in a winger")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isFalse()
    }

    @Test fun `institutional confirmation without a journalist still lights confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "Liverpool complete the signing — done deal")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isTrue()
    }

    @Test fun `non-transfer use of the word confirmed does not light confirmed`() {
        val (tier, confirmed) = subject.resolve(Tier.TIER_1_RELIABLE, "BBC Sport", "Liverpool's win confirmed at Anfield")
        assertThat(tier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(confirmed).isFalse()
    }

    @Test fun `unknown source falls back to the provided default`() {
        val (tier, _) = subject.resolve(Tier.TIER_2_BEAT, "This Is Anfield", "match report")
        assertThat(tier).isEqualTo(Tier.TIER_2_BEAT)
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*TierResolverTest"`
Expected: FAIL — compile error (`confirmationPhrases`/`tentativePhrases` don't exist yet) and/or assertion failures (tentative cases currently resolve to T1).

- [ ] **Step 3: Update the config**

Overwrite `TierConfig.kt` with:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

data class TierConfig(
    val tier1Journalists: Set<String>,
    val tier3Sources: Set<String>,
    val confirmationPhrases: Set<String>,
    val tentativePhrases: Set<String>,
)

val defaultTierConfig = TierConfig(
    tier1Journalists = setOf("david ornstein", "fabrizio romano", "david lynch"),
    tier3Sources = setOf("daily mirror", "the sun", "the mirror", "mirror", "sun"),
    // Multi-word phrases only — keeps "win confirmed" / "official website" from false-firing.
    confirmationPhrases = setOf(
        "here we go",
        "done deal",
        "deal done",
        "deal agreed",
        "agreement reached",
        "medical completed",
        "officially signed",
        "transfer confirmed",
        "deal confirmed",
        "completed the signing",
        "deal sealed",
    ),
    tentativePhrases = setOf(
        "interested in",
        "keen on",
        "linked with",
        "in talks",
        "monitoring",
        "tracking",
        "eyeing",
        "considering",
        "reportedly",
        "could sign",
        "target",
        "rumour",
        "rumor",
    ),
)
```

- [ ] **Step 4: Rewrite the resolver**

Overwrite `TierResolver.kt` with:

```kotlin
package dev.crushingdigits.littlebird.data.news.tier

import dev.crushingdigits.littlebird.data.news.classifier.TextNormalizer
import dev.crushingdigits.littlebird.domain.model.Tier

class TierResolver(private val config: TierConfig) {

    /**
     * Resolves the single reliability tier from two factors:
     *  - the source's institutional floor ([defaultTier]), never downgraded by wording;
     *  - a trusted-journalist override gated by language (confirmation -> T1, tentative -> T2).
     *
     * @return resolved tier + whether the item is a confirmed ("done deal") story.
     */
    fun resolve(defaultTier: Tier, author: String?, text: String): Pair<Tier, Boolean> {
        val haystack = TextNormalizer.normalize(author.orEmpty() + " " + text)
        val authorNorm = author?.let { TextNormalizer.normalize(it) }.orEmpty()

        val journalist = config.tier1Journalists.any { haystack.contains(it) }
        val tabloid = config.tier3Sources.any { authorNorm.contains(it) }
        val confirming = config.confirmationPhrases.any { haystack.contains(it) }
        val tentative = config.tentativePhrases.any { haystack.contains(it) }

        val tier = when {
            journalist && confirming -> Tier.TIER_1_RELIABLE
            tabloid -> Tier.TIER_3_SPECULATIVE
            journalist && tentative -> moreReliable(defaultTier, Tier.TIER_2_BEAT)
            else -> defaultTier
        }
        val confirmed = tier == Tier.TIER_1_RELIABLE && confirming
        return tier to confirmed
    }

    // Tier ordinals: TIER_1=0 (most reliable) .. TIER_3=2 (least). The smaller ordinal wins,
    // so a tentative journalist quote never drops an inherently-reliable (T1) source below its floor.
    private fun moreReliable(a: Tier, b: Tier): Tier = if (a.ordinal <= b.ordinal) a else b
}
```

- [ ] **Step 5: Run the test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*TierResolverTest"`
Expected: PASS (12 tests).

- [ ] **Step 6: Run the dependent classifier/repository tests — expect PASS**

These reference `defaultTierConfig` and "here we go"; verify the rename didn't break them.
Run: `./gradlew :app:testDebugUnitTest --tests "*NewsClassifierTest" --tests "*NewsRepositoryTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/tier/ app/src/test/java/dev/crushingdigits/littlebird/data/news/tier/
git commit -m "feat: three-state tier resolution by language

Trusted journalists resolve to T1 only with confirmation language, T2 with
tentative language; institutional floors are never downgraded by wording;
tabloids drop to T3. Broadens the confirmed signal to a phrase set."
```

---

## Task 2: Add `clubScoped` to FeedSource + promote BBC to Tier 1

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSource.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt`
- Test (create): `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt`

- [ ] **Step 1: Write the failing test**

Create `FeedSourcesTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.source

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test

class FeedSourcesTest {
    private fun source(id: String) = FeedSources.all.single { it.id == id }

    @Test fun `bbc is an inherently reliable tier-1 source`() {
        assertThat(source("bbc_liverpool").defaultTier).isEqualTo(Tier.TIER_1_RELIABLE)
    }

    @Test fun `feed sources are club-scoped by default`() {
        assertThat(source("bbc_liverpool").clubScoped).isTrue()
        assertThat(source("this_is_anfield").clubScoped).isTrue()
        assertThat(source("liverpool_echo").clubScoped).isTrue()
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesTest"`
Expected: FAIL — `clubScoped` is unresolved and BBC tier is `TIER_2_BEAT`.

- [ ] **Step 3: Add the `clubScoped` field**

Overwrite `FeedSource.kt` with:

```kotlin
package dev.crushingdigits.littlebird.data.news.source

import dev.crushingdigits.littlebird.domain.model.Tier

enum class FeedKind { RSS, ATOM }

/**
 * One RSS/Atom source. [defaultTier] is the floor reliability for items from this feed;
 * the TierResolver may bump an individual item up (e.g. an Ornstein byline with confirmation
 * language) or down (known tabloid). [clubScoped] = false marks a transfer-wide feed (e.g.
 * Fabrizio Romano's column) whose items are filtered to Liverpool stories at ingestion.
 * Adding a feed later is a one-line addition to [FeedSources.all].
 */
data class FeedSource(
    val id: String,
    val name: String,
    val url: String,
    val defaultTier: Tier,
    val kind: FeedKind = FeedKind.RSS,
    val clubScoped: Boolean = true,
)
```

- [ ] **Step 4: Promote BBC to Tier 1**

In `FeedSources.kt`, change the `bbc_liverpool` entry's `defaultTier` from `Tier.TIER_2_BEAT` to `Tier.TIER_1_RELIABLE`:

```kotlin
        FeedSource(
            id = "bbc_liverpool",
            name = "BBC Sport — Liverpool",
            url = "https://feeds.bbci.co.uk/sport/football/teams/liverpool/rss.xml",
            defaultTier = Tier.TIER_1_RELIABLE,
        ),
```

- [ ] **Step 5: Run the test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesTest" --tests "*FeedSourcesGuardTest"`
Expected: PASS — the guard still passes (no new sources yet).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/source/ app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt
git commit -m "feat: add clubScoped flag to FeedSource; promote BBC to Tier 1"
```

---

## Task 3: Liverpool relevance filter

A pure filter that keeps only Liverpool-relevant items. Used at ingestion for non-club-scoped feeds.

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilter.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilterTest.kt`

- [ ] **Step 1: Write the failing test**

Create `LiverpoolRelevanceFilterTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LiverpoolRelevanceFilterTest {
    private val subject = LiverpoolRelevanceFilter()

    @Test fun `keeps an item mentioning Liverpool in the title`() {
        assertThat(subject.isRelevant("Liverpool agree deal for midfielder", "Personal terms done")).isTrue()
    }

    @Test fun `keeps an item mentioning Anfield in the description`() {
        assertThat(subject.isRelevant("Big summer ahead", "Plans are taking shape at Anfield")).isTrue()
    }

    @Test fun `drops an item about a different club`() {
        assertThat(subject.isRelevant("Arsenal eye Declan Rice", "Arteta wants a new deal at the Emirates")).isFalse()
    }

    @Test fun `matching is case-insensitive`() {
        assertThat(subject.isRelevant("LIVERPOOL CLOSING IN", "")).isTrue()
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*LiverpoolRelevanceFilterTest"`
Expected: FAIL — `LiverpoolRelevanceFilter` does not exist.

- [ ] **Step 3: Implement the filter**

Create `LiverpoolRelevanceFilter.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.classifier

/**
 * Keeps only Liverpool-relevant items. Applied at ingestion to feeds flagged
 * `clubScoped = false` (e.g. a transfer-wide journalist column), so non-Liverpool
 * stories never reach the cache. Terms are lowercase; matching runs on normalized text.
 */
class LiverpoolRelevanceFilter(private val terms: Set<String> = defaultLiverpoolTerms) {
    fun isRelevant(title: String, description: String): Boolean {
        val haystack = TextNormalizer.normalize("$title $description")
        return terms.any { haystack.contains(it) }
    }
}

val defaultLiverpoolTerms: Set<String> = setOf(
    "liverpool",
    "anfield",
    "lfc",
    "the reds",
    "the kop",
)
```

- [ ] **Step 4: Run the test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*LiverpoolRelevanceFilterTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilter.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/classifier/LiverpoolRelevanceFilterTest.kt
git commit -m "feat: add LiverpoolRelevanceFilter for non-club-scoped feeds"
```

---

## Task 4: Wire the filter into the repository + DI

`NewsRepositoryImpl.refresh()` filters non-club-scoped items before classification.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/NewsRepository.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DataBindingsModule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/NewsRepositoryTest.kt`

- [ ] **Step 1: Add the failing repository test**

In `NewsRepositoryTest.kt`, first update the imports block — add:

```kotlin
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
```

Then update the `repo(...)` helper (currently line ~59-60) to pass a filter:

```kotlin
    private fun repo(fetcher: FeedFetcher, sources: List<FeedSource> = listOf(source)) =
        NewsRepositoryImpl(
            NewsRemoteDataSource(fetcher, FeedParser()),
            local,
            classifier,
            sources,
            LiverpoolRelevanceFilter(),
        )
```

Then add this test method to the class:

```kotlin
    @Test fun `non-club-scoped feed keeps only Liverpool items`() = runTest {
        val romano = FeedSource(
            id = "fabrizio_romano",
            name = "Fabrizio Romano",
            url = "https://x/romano-feed",
            defaultTier = Tier.TIER_2_BEAT,
            clubScoped = false,
        )
        val feed = """
            <rss version="2.0"><channel>
              <item><title>Arsenal eye Declan Rice</title><link>https://x/arsenal</link>
                <dc:creator>Fabrizio Romano</dc:creator><description>Arteta wants a new deal</description>
                <pubDate>Tue, 02 Jun 2026 20:56:47 GMT</pubDate></item>
              <item><title>Liverpool push for a winger</title><link>https://x/lfc</link>
                <dc:creator>Fabrizio Romano</dc:creator><description>Reds in talks</description>
                <pubDate>Tue, 02 Jun 2026 21:56:47 GMT</pubDate></item>
            </channel></rss>
        """.trimIndent()
        val subject = repo(FakeFetcher(mapOf(romano.url to Result.success(feed))), listOf(romano))

        subject.refresh()

        val items = subject.observeFeed().first()
        assertThat(items).hasSize(1)
        assertThat(items.single().title).isEqualTo("Liverpool push for a winger")
    }
```

- [ ] **Step 2: Run the test — expect FAIL**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"`
Expected: FAIL — `NewsRepositoryImpl` constructor has no 5th parameter (compile error).

- [ ] **Step 3: Add the filter to the repository**

In `NewsRepository.kt`, add the import:

```kotlin
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
```

Change the `NewsRepositoryImpl` constructor to add the filter as the last parameter:

```kotlin
class NewsRepositoryImpl(
    private val remote: NewsRemoteDataSource,
    private val local: NewsLocalDataSource,
    private val classifier: NewsClassifier,
    private val sources: List<FeedSource>,
    private val relevanceFilter: LiverpoolRelevanceFilter,
) : NewsRepository {
```

Replace the body of `refresh()` with (adds the filter step before mapping):

```kotlin
    override suspend fun refresh(): Result<Unit> = runCatching {
        val raw = remote.fetchAll(sources)
        val relevant = raw.filter { item ->
            val src = sourcesById[item.sourceId]
            src == null || src.clubScoped || relevanceFilter.isRelevant(item.title, item.description)
        }
        val entities = relevant.map { item ->
            val src = sourcesById[item.sourceId]
            val classification = classifier.classify(
                title = item.title,
                description = item.description,
                author = item.author,
                defaultTier = src?.defaultTier ?: Tier.TIER_3_SPECULATIVE,
            )
            item.toEntity(sourceName = src?.name ?: item.sourceId, classification = classification)
        }
        if (entities.isNotEmpty()) local.upsert(entities)
    }
```

- [ ] **Step 4: Run the test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*NewsRepositoryTest"`
Expected: PASS (5 tests — the 4 existing plus the new relevance test).

- [ ] **Step 5: Wire DI**

In `NetworkModule.kt`, add the import:

```kotlin
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
```

Add this provider inside the `NetworkModule` object (after `newsClassifier`):

```kotlin
    @Provides
    @Singleton
    fun liverpoolRelevanceFilter(): LiverpoolRelevanceFilter = LiverpoolRelevanceFilter()
```

In `DataBindingsModule.kt`, add the import:

```kotlin
import dev.crushingdigits.littlebird.data.news.classifier.LiverpoolRelevanceFilter
```

Update the `newsRepository` provider to take and pass the filter:

```kotlin
    @Provides
    @Singleton
    fun newsRepository(
        remote: NewsRemoteDataSource,
        local: NewsLocalDataSource,
        classifier: NewsClassifier,
        sources: List<FeedSource>,
        relevanceFilter: LiverpoolRelevanceFilter,
    ): NewsRepository = NewsRepositoryImpl(remote, local, classifier, sources, relevanceFilter)
```

- [ ] **Step 6: Verify the whole module compiles + tests pass**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS (full suite; confirms DI signatures and constructor wiring compile).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/NewsRepository.kt app/src/main/java/dev/crushingdigits/littlebird/di/ app/src/test/java/dev/crushingdigits/littlebird/data/news/NewsRepositoryTest.kt
git commit -m "feat: filter non-club-scoped feeds to Liverpool items at ingestion"
```

---

## Task 5: Add The Guardian — Liverpool source (+ real fixture)

Guardian is an inherently reliable institution → Tier 1 floor, club-scoped.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt`
- Create: `app/src/test/resources/feeds/source_guardian_liverpool.xml`

- [ ] **Step 1: Add the source entry**

Add to `FeedSources.all` in `FeedSources.kt` (inside the `listOf(...)`):

```kotlin
        FeedSource(
            id = "guardian_liverpool",
            name = "The Guardian — Liverpool",
            url = "https://www.theguardian.com/football/liverpool/rss",
            defaultTier = Tier.TIER_1_RELIABLE,
        ),
```

- [ ] **Step 2: Run the guard — expect FAIL (no fixture yet)**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest"`
Expected: FAIL — `guardian_liverpool: no fixture (expected feeds/source_guardian_liverpool.xml)`.

- [ ] **Step 3: Capture the real fixture (trimmed to 3 items)**

Run from the repo root. Fetches the live feed and trims to the first 3 items, preserving namespaces:

```bash
curl -sSL -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36" \
  -o /tmp/feed_guardian.xml "https://www.theguardian.com/football/liverpool/rss"
python3 - <<'PY'
import re, pathlib
raw = pathlib.Path("/tmp/feed_guardian.xml").read_text()
items = re.findall(r"<item>.*?</item>", raw, re.S)[:3]
assert items, "no <item> elements found — feed shape changed; inspect /tmp/feed_guardian.xml"
head = raw[:raw.index("<item>")]
out = head + "\n" + "\n".join(items) + "\n  </channel>\n</rss>\n"
pathlib.Path("app/src/test/resources/feeds/source_guardian_liverpool.xml").write_text(out)
print("guardian items kept:", len(items))
PY
```

Expected output: `guardian items kept: 3`.

> If the feed ever 404s, fall back to `https://www.theguardian.com/football/rss` (whole-football) in both the curl URL above and the `url` in Step 1. Verified live 2026-06-03: HTTP 200, named-zone GMT `pubDate`.

- [ ] **Step 4: Add the source assertion**

Add to `FeedSourcesTest.kt`:

```kotlin
    @Test fun `guardian is an inherently reliable club-scoped tier-1 source`() {
        val guardian = source("guardian_liverpool")
        assertThat(guardian.defaultTier).isEqualTo(Tier.TIER_1_RELIABLE)
        assertThat(guardian.clubScoped).isTrue()
    }
```

- [ ] **Step 5: Run the guard + source test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest" --tests "*FeedSourcesTest"`
Expected: PASS — the fixture parses to ≥1 dated item.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt app/src/test/resources/feeds/source_guardian_liverpool.xml
git commit -m "feat: add The Guardian — Liverpool source (Tier 1, club-scoped)"
```

---

## Task 6: Add David Lynch source (+ real fixture)

Dedicated Liverpool correspondent → Tier 2 floor, club-scoped; trusted-journalist name already in config (Task 1) lifts his confirmation-language items to T1.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt`
- Create: `app/src/test/resources/feeds/source_david_lynch.xml`

- [ ] **Step 1: Add the source entry**

Add to `FeedSources.all`:

```kotlin
        FeedSource(
            id = "david_lynch",
            name = "David Lynch",
            url = "https://www.davidlynchlfc.co.uk/feed",
            defaultTier = Tier.TIER_2_BEAT,
        ),
```

- [ ] **Step 2: Run the guard — expect FAIL (no fixture yet)**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest"`
Expected: FAIL — `david_lynch: no fixture`.

- [ ] **Step 3: Capture the real fixture (trimmed to 3 items)**

```bash
curl -sSL -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36" \
  -o /tmp/feed_lynch.xml "https://www.davidlynchlfc.co.uk/feed"
python3 - <<'PY'
import re, pathlib
raw = pathlib.Path("/tmp/feed_lynch.xml").read_text()
items = re.findall(r"<item>.*?</item>", raw, re.S)[:3]
assert items, "no <item> elements found — feed shape changed; inspect /tmp/feed_lynch.xml"
head = raw[:raw.index("<item>")]
out = head + "\n" + "\n".join(items) + "\n  </channel>\n</rss>\n"
pathlib.Path("app/src/test/resources/feeds/source_david_lynch.xml").write_text(out)
print("lynch items kept:", len(items))
PY
```

Expected output: `lynch items kept: 3`. Verified live 2026-06-03: HTTP 200, `dc:creator` = `David Lynch` (CDATA), named-zone GMT `pubDate`.

- [ ] **Step 4: Add the source assertion**

Add to `FeedSourcesTest.kt`:

```kotlin
    @Test fun `david lynch is a club-scoped tier-2 beat source`() {
        val lynch = source("david_lynch")
        assertThat(lynch.defaultTier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(lynch.clubScoped).isTrue()
    }
```

- [ ] **Step 5: Run the guard + source test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest" --tests "*FeedSourcesTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt app/src/test/resources/feeds/source_david_lynch.xml
git commit -m "feat: add David Lynch source (Tier 2 beat, club-scoped)"
```

---

## Task 7: Add Fabrizio Romano source (+ real fixture)

Transfer-wide column → Tier 2 floor, **not** club-scoped (relevance filter from Task 4 applies). Romano already in the trusted-journalist set.

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt`
- Create: `app/src/test/resources/feeds/source_fabrizio_romano.xml`

- [ ] **Step 1: Add the source entry**

Add to `FeedSources.all`:

```kotlin
        FeedSource(
            id = "fabrizio_romano",
            name = "Fabrizio Romano",
            url = "https://www.caughtoffside.com/author/fabrizio-romano/feed/",
            defaultTier = Tier.TIER_2_BEAT,
            clubScoped = false,
        ),
```

- [ ] **Step 2: Run the guard — expect FAIL (no fixture yet)**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest"`
Expected: FAIL — `fabrizio_romano: no fixture`.

- [ ] **Step 3: Capture the real fixture (trimmed to 3 items)**

```bash
curl -sSL -A "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36" \
  -o /tmp/feed_romano.xml "https://www.caughtoffside.com/author/fabrizio-romano/feed/"
python3 - <<'PY'
import re, pathlib
raw = pathlib.Path("/tmp/feed_romano.xml").read_text()
items = re.findall(r"<item>.*?</item>", raw, re.S)[:3]
assert items, "no <item> elements found — feed shape changed; inspect /tmp/feed_romano.xml"
head = raw[:raw.index("<item>")]
out = head + "\n" + "\n".join(items) + "\n  </channel>\n</rss>\n"
pathlib.Path("app/src/test/resources/feeds/source_fabrizio_romano.xml").write_text(out)
print("romano items kept:", len(items))
PY
```

Expected output: `romano items kept: 3`. Verified live 2026-06-03: HTTP 200, `dc:creator` = `Fabrizio Romano` (CDATA), numeric-offset `pubDate` (`+0000`). The feed is transfer-wide (its top item is often non-Liverpool) — that is expected; the relevance filter handles it at runtime, while the guard only requires a dated item.

- [ ] **Step 4: Add the source assertion**

Add to `FeedSourcesTest.kt`:

```kotlin
    @Test fun `fabrizio romano is a non-club-scoped tier-2 source`() {
        val romano = source("fabrizio_romano")
        assertThat(romano.defaultTier).isEqualTo(Tier.TIER_2_BEAT)
        assertThat(romano.clubScoped).isFalse()
    }
```

- [ ] **Step 5: Run the guard + source test — expect PASS**

Run: `./gradlew :app:testDebugUnitTest --tests "*FeedSourcesGuardTest" --tests "*FeedSourcesTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesTest.kt app/src/test/resources/feeds/source_fabrizio_romano.xml
git commit -m "feat: add Fabrizio Romano source (Tier 2, non-club-scoped + relevance-filtered)"
```

---

## Task 8: Final verification

- [ ] **Step 1: Run the full unit-test suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS — all tests green, including the guard parameterized over all 6 sources.

- [ ] **Step 2: Assemble the debug APK (confirms DI graph + main compiles)**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Update the spec status**

In `docs/superpowers/specs/2026-06-03-news-sources-and-tier-reliability-design.md`, change the Status line from `Approved (brainstormed) — not yet implemented` to `Implemented on feat/news-sources-and-tier-reliability`.

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-06-03-news-sources-and-tier-reliability-design.md
git commit -m "docs: mark news-sources-and-tier design as implemented"
```

---

## Self-review notes (verification against the spec)

- **§2 single axis from two factors** → Task 1 (resolver) + Task 2 (BBC floor) + Task 5 (Guardian floor).
- **§3 official site out of scope** → no task; explicitly excluded (no feed exists).
- **§4 three new sources + fixtures** → Tasks 5, 6, 7, each with a real captured fixture; `FeedSourcesGuardTest` (existing) enforces them.
- **§5 clubScoped + relevance filter** → Task 2 (field) + Task 3 (filter) + Task 4 (ingestion wiring).
- **§6 resolution algorithm (ordered precedence + floor)** → Task 1 resolver; full matrix in `TierResolverTest`.
- **§7 config (BBC→T1, journalists incl. Lynch, confirmation/tentative phrases, tabloids, labels unchanged)** → Tasks 1 & 2. Labels in `Tier.kt` are deliberately untouched.
- **§8 unit tests only, real fixtures** → every task is JVM/Robolectric unit tests; fixtures are live captures.
- **Type consistency:** `resolve(defaultTier, author, text)` signature unchanged (no ripple to `NewsClassifier`); `clubScoped` defaulted last so positional `FeedSource(...)` calls in existing tests still compile; `NewsRepositoryImpl` filter param added last and threaded through both DI modules and the test helper.
