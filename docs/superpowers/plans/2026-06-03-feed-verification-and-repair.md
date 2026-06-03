# Feed Verification & Repair — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make feed verification comprehensive and hermetic — exercise the parser/mapper against *real* captured feed formats — and fix the bugs that exposes (date-parse strictness burying BBC/Echo items at EPOCH; BBC images dropped; a dead 404 source).

**Architecture:** Pure data-layer change. Three real captured feeds become per-source test fixtures keyed by `source.id`. TDD: a failing mapper test drives a broadened `parseDate`; a failing parser test drives `media:thumbnail` image handling; a "no source silently empty" guard parameterized over `FeedSources.all` drives removal of the dead TIA Transfers source.

**Tech Stack:** Kotlin, JUnit4, Truth, Robolectric 4.14 (the parser touches `android.util.Xml`). `java.time` for dates. No new dependencies.

**Spec:** `docs/superpowers/specs/2026-06-03-feed-verification-and-repair-design.md`
**Branch:** `fix/feed-verification-and-repair` (already checked out).

---

## Conventions & guardrails

- **TDD throughout:** failing test first, watch it fail, minimal fix, watch it pass, ✅ checkpoint.
- **Test conventions** (`/write-unit-tests` Conventions): Truth `assertThat`, JUnit4 `@Test` with backtick names, fixtures loaded via `javaClass.classLoader!!.getResourceAsStream("feeds/<name>")`. Parser tests run under `@RunWith(RobolectricTestRunner::class)`; mapper tests are plain JVM.
- **Version control (this repo):** **do NOT use `/submit-mr`**, and do **not** push or open a PR unless the user explicitly asks. At each ✅ checkpoint, commit that task's files directly with `git` — descriptive message ending with the `Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>` trailer. Execution is **subagent-driven** (one subagent per task, reviewed between tasks).
- **Reference for test/setup config:** `github.com/crushingdigits/quotanoia`.
- **Weekday consistency:** `RFC_1123_DATE_TIME` validates the day-of-week against the date. All fixture/test `pubDate` strings below use correct weekdays (1 Jun 2026 = Mon, 2 Jun = Tue, 3 Jun = Wed). Keep it that way.
- **No YouTrack** on this project — ticketless branch already created.

---

## File map

| File | Action | Responsibility |
|---|---|---|
| `app/src/test/resources/feeds/source_this_is_anfield.xml` | Create | Real-format TIA (WordPress RSS) fixture |
| `app/src/test/resources/feeds/source_liverpool_echo.xml` | Create | Real Liverpool Echo fixture (single-digit day, media:thumbnail+enclosure+media:content) |
| `app/src/test/resources/feeds/source_bbc_liverpool.xml` | Create | Real BBC fixture (CDATA, isPermaLink guid, GMT zone, media:thumbnail) |
| `app/src/main/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappers.kt` | Modify | Broaden `parseDate` (RFC-1123 + ISO) |
| `app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParser.kt` | Modify | Handle `media:thumbnail` images |
| `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt` | Modify | Remove dead TIA Transfers source |
| `app/src/test/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappersTest.kt` | Modify | Real-date parsing tests |
| `app/src/test/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParserTest.kt` | Modify | Real BBC/Echo fixture + image tests |
| `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesGuardTest.kt` | Create | "No source silently empty" guard |

---

## Task 1: Capture real feeds as per-source fixtures

**Files:**
- Create: `app/src/test/resources/feeds/source_this_is_anfield.xml`
- Create: `app/src/test/resources/feeds/source_liverpool_echo.xml`
- Create: `app/src/test/resources/feeds/source_bbc_liverpool.xml`

These are trimmed **real captures** (2 items each) from the live feeds (BBC & Echo verbatim from the live probe; TIA in its real WordPress RSS format). They exercise every real-world quirk: CDATA, `isPermaLink` guid, `dc:creator`, single-digit days, zone-name dates, and the three image-tag variants.

- [ ] **Step 1: Create the TIA fixture**

`app/src/test/resources/feeds/source_this_is_anfield.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:media="http://search.yahoo.com/mrss/">
  <channel>
    <title>This Is Anfield</title>
    <item>
      <title>Liverpool target agrees personal terms ahead of summer move</title>
      <link>https://www.thisisanfield.com/2026/06/liverpool-target-agrees-terms/</link>
      <dc:creator><![CDATA[This Is Anfield]]></dc:creator>
      <pubDate>Tue, 02 Jun 2026 14:30:00 +0000</pubDate>
      <guid isPermaLink="false">https://www.thisisanfield.com/?p=341205</guid>
      <description><![CDATA[The Reds have reportedly won the race for the midfielder, with personal terms now agreed.]]></description>
      <content:encoded><![CDATA[<p>The Reds have reportedly won the race for the midfielder.</p>]]></content:encoded>
      <media:content url="https://www.thisisanfield.com/uploads/2026/06/target.jpg" medium="image"/>
    </item>
    <item>
      <title>Five things we learned from Liverpool's latest training session</title>
      <link>https://www.thisisanfield.com/2026/06/five-things-training/</link>
      <dc:creator><![CDATA[This Is Anfield]]></dc:creator>
      <pubDate>Mon, 01 Jun 2026 09:15:00 +0000</pubDate>
      <guid isPermaLink="false">https://www.thisisanfield.com/?p=341180</guid>
      <description><![CDATA[A closer look at the standout moments from the open training session at the AXA Centre.]]></description>
      <content:encoded><![CDATA[<p>A closer look at the standout moments.</p>]]></content:encoded>
      <media:content url="https://www.thisisanfield.com/uploads/2026/06/training.jpg" medium="image"/>
    </item>
  </channel>
</rss>
```

- [ ] **Step 2: Create the Liverpool Echo fixture**

`app/src/test/resources/feeds/source_liverpool_echo.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:media="http://search.yahoo.com/mrss/" xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
  <channel>
    <title>Liverpool - Liverpool FC</title>
    <item>
      <title>Who is on Andoni Iraola backroom team? The 4 coaches who could join Liverpool</title>
      <link>https://www.liverpool.com/liverpool-fc-news/features/iraola-coaches-34055350</link>
      <guid>https://www.liverpool.com/liverpool-fc-news/features/iraola-coaches-34055350</guid>
      <description><![CDATA[Andoni Iraola looks set to become the new Liverpool head coach, but which members of his staff could he bring?]]></description>
      <pubDate>Wed, 3 Jun 2026 08:15:56 +0000</pubDate>
      <author>Matt Addison</author>
      <category>Liverpool FC</category>
      <media:thumbnail url="https://i2-prod.liverpool.com/article34055390.ece/ALTERNATES/s98/0_image.jpg" width="98" height="98"></media:thumbnail>
      <enclosure url="https://i2-prod.liverpool.com/article34055390.ece/ALTERNATES/s615/0_image.jpg" type="image/jpeg" length="754605"/>
      <media:content url="https://i2-prod.liverpool.com/article34055390.ece/ALTERNATES/s615/0_image.jpg" type="image/jpeg" width="615" height="409"/>
    </item>
    <item>
      <title>Andoni Iraola already explained Liverpool contract length after following familiar path</title>
      <link>https://www.liverpool.com/liverpool-fc-news/features/iraola-contract-34055275</link>
      <guid>https://www.liverpool.com/liverpool-fc-news/features/iraola-contract-34055275</guid>
      <description><![CDATA[Liverpool is closing in on appointing Andoni Iraola as its new manager.]]></description>
      <pubDate>Wed, 3 Jun 2026 08:11:51 +0000</pubDate>
      <author>James Findlater</author>
      <category>Liverpool FC</category>
      <media:thumbnail url="https://i2-prod.liverpool.com/article34055296.ece/ALTERNATES/s98/0_image.jpg" width="98" height="98"></media:thumbnail>
      <enclosure url="https://i2-prod.liverpool.com/article34055296.ece/ALTERNATES/s615/0_image.jpg" type="image/jpeg" length="754605"/>
      <media:content url="https://i2-prod.liverpool.com/article34055296.ece/ALTERNATES/s615/0_image.jpg" type="image/jpeg" width="615" height="409"/>
    </item>
  </channel>
</rss>
```

- [ ] **Step 3: Create the BBC fixture**

`app/src/test/resources/feeds/source_bbc_liverpool.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:media="http://search.yahoo.com/mrss/" xmlns:atom="http://www.w3.org/2005/Atom" version="2.0">
  <channel>
    <title>BBC Sport - Liverpool</title>
    <item>
      <title><![CDATA[Mohamed Salah - Egyptian king]]></title>
      <description><![CDATA[BBC Sport visits Egypt to discover what Mohamed Salah means to the people of the football-mad country.]]></description>
      <link>https://www.bbc.com/sport/football/articles/cx2xlk74llno?at_medium=RSS&amp;at_campaign=rss</link>
      <guid isPermaLink="false">https://www.bbc.com/sport/football/articles/cx2xlk74llno#0</guid>
      <pubDate>Wed, 03 Jun 2026 06:26:52 GMT</pubDate>
      <media:thumbnail width="240" height="135" url="https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/fa1c/live/salah.jpg"/>
    </item>
    <item>
      <title><![CDATA[Liverpool agree deal with Iraola to succeed Slot]]></title>
      <description><![CDATA[Liverpool reach a verbal agreement with Andoni Iraola to take over as head coach.]]></description>
      <link>https://www.bbc.com/sport/football/articles/c4gv1ydm4yro?at_medium=RSS&amp;at_campaign=rss</link>
      <guid isPermaLink="false">https://www.bbc.com/sport/football/articles/c4gv1ydm4yro#0</guid>
      <pubDate>Tue, 02 Jun 2026 20:56:47 GMT</pubDate>
      <media:thumbnail width="240" height="135" url="https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/cf3d/live/iraola.jpg"/>
    </item>
  </channel>
</rss>
```

- [ ] **Step 4: Verify the files exist**

Run: `ls app/src/test/resources/feeds/`
Expected: lists `source_this_is_anfield.xml`, `source_liverpool_echo.xml`, `source_bbc_liverpool.xml` (alongside the existing `rss_sample.xml`, `atom_sample.xml`).

- [ ] **Step 5: ✅ Checkpoint** — `git add` this task's created/modified files and `git commit` with a descriptive message + the `Co-Authored-By` trailer. Do **not** use `/submit-mr`.

---

## Task 2: Broaden date parsing (single-digit days + zone names)

**Files:**
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappersTest.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappers.kt`

- [ ] **Step 1: Write the failing tests**

Add this import to `NewsMappersTest.kt` (with the other imports):

```kotlin
import java.time.Instant
```

Add these tests inside the `NewsMappersTest` class:

```kotlin
private fun anyClassification() =
    Classification(isMatch = false, category = Category.OTHER, tier = Tier.TIER_2_BEAT, confirmed = false)

@Test fun `parses single-digit day pubDate (Liverpool Echo) to a real instant`() {
    val raw = RawFeedItem(
        sourceId = "liverpool_echo", title = "T", link = "https://x/echo", description = "d",
        pubDate = "Wed, 3 Jun 2026 08:15:56 +0000", author = "Matt Addison",
        categories = emptyList(), imageUrl = null, guid = "https://x/echo",
    )
    val entity = raw.toEntity("Liverpool Echo", anyClassification())
    assertThat(entity.publishedAtEpochMs)
        .isEqualTo(Instant.parse("2026-06-03T08:15:56Z").toEpochMilli())
}

@Test fun `parses zone-name pubDate (BBC GMT) to a real instant`() {
    val raw = RawFeedItem(
        sourceId = "bbc_liverpool", title = "T", link = "https://x/bbc", description = "d",
        pubDate = "Wed, 03 Jun 2026 06:26:52 GMT", author = null,
        categories = emptyList(), imageUrl = null, guid = "https://x/bbc",
    )
    val entity = raw.toEntity("BBC Sport", anyClassification())
    assertThat(entity.publishedAtEpochMs)
        .isEqualTo(Instant.parse("2026-06-03T06:26:52Z").toEpochMilli())
}

@Test fun `parses ISO-8601 (Atom) pubDate`() {
    val raw = RawFeedItem(
        sourceId = "bbc", title = "T", link = "https://x/iso", description = "d",
        pubDate = "2026-06-01T07:00:00Z", author = null,
        categories = emptyList(), imageUrl = null, guid = "https://x/iso",
    )
    val entity = raw.toEntity("Atom", anyClassification())
    assertThat(entity.publishedAtEpochMs)
        .isEqualTo(Instant.parse("2026-06-01T07:00:00Z").toEpochMilli())
}

@Test fun `unparseable pubDate falls back to EPOCH`() {
    val raw = RawFeedItem(
        sourceId = "x", title = "T", link = "https://x/z", description = "d",
        pubDate = "not a date", author = null,
        categories = emptyList(), imageUrl = null, guid = "https://x/z",
    )
    val entity = raw.toEntity("X", anyClassification())
    assertThat(entity.publishedAtEpochMs).isEqualTo(0L)
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.mapper.NewsMappersTest"`
Expected: FAIL — the single-digit-day and GMT tests resolve to `0L` (EPOCH) because the current formatter `"EEE, dd MMM yyyy HH:mm:ss Z"` rejects `3` (needs `dd`) and `GMT` (needs a numeric offset).

- [ ] **Step 3: Broaden `parseDate`**

In `NewsMappers.kt`: remove the unused `import java.util.Locale`, delete the `rfc822` field, and replace `parseDate`. The relevant region (currently lines 16, 20–25) becomes:

```kotlin
private val htmlTag = Regex("<[^>]+>")
private val whitespace = Regex("\\s+")

private fun String.stripHtml(): String = htmlTag.replace(this, "").replace(whitespace, " ").trim()

/**
 * RFC-1123/822 (RSS pubDate) — tolerant of single-digit days (`3 Jun`) and zone names
 * (`GMT`) as well as numeric offsets (`+0000`) — then ISO-8601 (Atom). Unknown/blank → EPOCH.
 */
private fun parseDate(raw: String?): Instant = raw?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
    runCatching { OffsetDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant() }
        .getOrNull()
} ?: Instant.EPOCH
```

Keep the existing imports `java.time.Instant`, `java.time.OffsetDateTime`, and `java.time.format.DateTimeFormatter` (the latter is still used for `RFC_1123_DATE_TIME`).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.mapper.NewsMappersTest"`
Expected: PASS — all four new tests plus the three pre-existing mapper tests (the existing `Mon, 01 Jun 2026 09:00:00 +0000` case still parses under RFC-1123).

- [ ] **Step 5: ✅ Checkpoint** — `git add` this task's created/modified files and `git commit` with a descriptive message + the `Co-Authored-By` trailer. Do **not** use `/submit-mr`.

---

## Task 3: Read `media:thumbnail` images (BBC)

**Files:**
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParserTest.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParser.kt`

- [ ] **Step 1: Write the failing test (BBC) + a regression test (Echo)**

Add these tests inside `FeedParserTest` (it already has the `fixture(name)` helper and the Robolectric runner):

```kotlin
@Test fun `parses BBC fixture incl media-thumbnail image and isPermaLink guid`() {
    val items = subject.parse(fixture("source_bbc_liverpool.xml"), sourceId = "bbc_liverpool")

    assertThat(items).hasSize(2)
    val first = items[0]
    assertThat(first.title).isEqualTo("Mohamed Salah - Egyptian king")
    assertThat(first.guid).isEqualTo("https://www.bbc.com/sport/football/articles/cx2xlk74llno#0")
    assertThat(first.pubDate).isEqualTo("Wed, 03 Jun 2026 06:26:52 GMT")
    assertThat(first.imageUrl)
        .isEqualTo("https://ichef.bbci.co.uk/ace/standard/240/cpsprodpb/fa1c/live/salah.jpg")
}

@Test fun `parses Echo fixture, larger media-content image wins over thumbnail`() {
    val items = subject.parse(fixture("source_liverpool_echo.xml"), sourceId = "liverpool_echo")

    assertThat(items).hasSize(2)
    val first = items[0]
    assertThat(first.author).isEqualTo("Matt Addison")
    assertThat(first.pubDate).isEqualTo("Wed, 3 Jun 2026 08:15:56 +0000")
    assertThat(first.imageUrl).contains("/ALTERNATES/s615/") // media:content (615) overwrites the 98px thumbnail
}
```

- [ ] **Step 2: Run to verify the BBC test fails**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParserTest"`
Expected: FAIL on `parses BBC fixture...` — `first.imageUrl` is `null` because the parser only reads `media:content`/`enclosure`, never `media:thumbnail` (BBC's only image tag). The Echo test passes already (it has a `media:content`), proving the fix won't regress ordering.

- [ ] **Step 3: Add `media:thumbnail` to the image-tag branch**

In `FeedParser.kt`, change the image branch (currently around line 44) from:

```kotlin
                    "media:content", "enclosure" ->
                        if (inItem && current != null) {
                            parser.getAttributeValue(null, "url")?.let { current["image"] = it }
                        }
```

to:

```kotlin
                    "media:content", "media:thumbnail", "enclosure" ->
                        if (inItem && current != null) {
                            parser.getAttributeValue(null, "url")?.let { current["image"] = it }
                        }
```

(Document order still wins: Echo's tiny `media:thumbnail` is overwritten by the later `media:content`, so the 615px image still wins; BBC now gets its 240px thumbnail.)

- [ ] **Step 4: Run to verify both pass**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParserTest"`
Expected: PASS — both new tests plus the two pre-existing parser tests (`rss_sample`, `atom_sample`).

- [ ] **Step 5: ✅ Checkpoint** — `git add` this task's created/modified files and `git commit` with a descriptive message + the `Co-Authored-By` trailer. Do **not** use `/submit-mr`.

---

## Task 4: The "no source silently empty" guard + remove the dead source

**Files:**
- Create: `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesGuardTest.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt`

- [ ] **Step 1: Write the guard test**

Create `app/src/test/java/dev/crushingdigits/littlebird/data/news/source/FeedSourcesGuardTest.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.source

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedParser
import dev.crushingdigits.littlebird.data.news.mapper.toEntity
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Classification
import dev.crushingdigits.littlebird.domain.model.Tier
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Every configured source MUST have a real captured fixture (feeds/source_<id>.xml) that parses
 * to at least one item with a real (non-EPOCH) date. This makes "a source silently contributes
 * nothing" impossible to ship — adding a source without a fixture, or shipping a format the
 * parser/mapper can't read, turns this red.
 */
@RunWith(RobolectricTestRunner::class)
class FeedSourcesGuardTest {
    private val parser = FeedParser()
    private val anyClassification =
        Classification(isMatch = false, category = Category.OTHER, tier = Tier.TIER_2_BEAT, confirmed = false)

    private fun fixtureOrNull(id: String) =
        javaClass.classLoader!!.getResourceAsStream("feeds/source_$id.xml")
            ?.bufferedReader()?.use { it.readText() }

    @Test fun `every configured source has a fixture that parses to at least one dated item`() {
        val failures = buildList {
            FeedSources.all.forEach { source ->
                val xml = fixtureOrNull(source.id)
                if (xml == null) {
                    add("${source.id}: no fixture (expected feeds/source_${source.id}.xml)")
                    return@forEach
                }
                val items = parser.parse(xml, source.id)
                if (items.isEmpty()) {
                    add("${source.id}: fixture parsed to 0 items")
                    return@forEach
                }
                val dated = items.map { it.toEntity(source.name, anyClassification) }
                    .count { it.publishedAtEpochMs > 0L }
                if (dated == 0) add("${source.id}: ${items.size} items but none had a non-EPOCH date")
            }
        }
        assertThat(failures).isEmpty()
    }
}
```

- [ ] **Step 2: Run to verify it fails on the dead source**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.source.FeedSourcesGuardTest"`
Expected: FAIL — failures contains `tia_transfers: no fixture (expected feeds/source_tia_transfers.xml)`. (TIA Transfers is a live 404 with no real content to capture — the guard is correctly refusing to let it ship as silent nothing.) The other three sources pass.

- [ ] **Step 3: Remove the dead TIA Transfers source**

In `FeedSources.kt`, delete the `tia_transfers` entry so `all` contains exactly the three live, fixtured sources:

```kotlin
object FeedSources {
    val all: List<FeedSource> = listOf(
        FeedSource(
            id = "this_is_anfield",
            name = "This Is Anfield",
            url = "https://www.thisisanfield.com/feed/",
            defaultTier = Tier.TIER_2_BEAT,
        ),
        FeedSource(
            id = "liverpool_echo",
            name = "Liverpool Echo",
            url = "https://www.liverpool.com/all-about/liverpool-fc?service=rss",
            defaultTier = Tier.TIER_2_BEAT,
        ),
        FeedSource(
            id = "bbc_liverpool",
            name = "BBC Sport — Liverpool",
            url = "https://feeds.bbci.co.uk/sport/football/teams/liverpool/rss.xml",
            defaultTier = Tier.TIER_2_BEAT,
        ),
    )
}
```

- [ ] **Step 4: Run the guard to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.news.source.FeedSourcesGuardTest"`
Expected: PASS — `failures` is empty; all three remaining sources parse to ≥1 dated item.

- [ ] **Step 5: ✅ Checkpoint** — `git add` this task's created/modified files and `git commit` with a descriptive message + the `Co-Authored-By` trailer. Do **not** use `/submit-mr`.

---

## Task 5: Full verification

**Files:** none (verification only).

- [ ] **Step 1: Run the whole unit suite**

Run: `./gradlew :app:testDebugUnitTest`
Expected: ALL PASS. Confirms removing `tia_transfers` regressed nothing (e.g. `NewsRemoteDataSourceTest`, `NewsRepositoryTest`).

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Coverage + lint per project convention**

Invoke the `/write-unit-tests` **Workflow** section (kover gap-closing pass) and run detekt per project conventions.
**Flag for the engineer:** no `kover` or `detekt` plugin is currently applied in `app/build.gradle.kts` or the version catalog. If the Workflow reports `koverXmlReport`/detekt tasks don't exist, do **not** improvise a tooling setup — coverage here is already enforced structurally by the comprehensive fixture tests + the source guard. Note the gap to the user and move on.

- [ ] **Step 4: Manual smoke (test-plan — CI can't catch this)**

On a device/emulator, cold-launch and pull-to-refresh:
- BBC Sport and Liverpool Echo items now appear **interleaved by recency** among This Is Anfield items — no longer all buried at the bottom at 1970.
- A BBC card renders its thumbnail image (previously blank).

- [ ] **Step 5: Final commit**

Commit any remaining changes directly with `git` (descriptive message + `Co-Authored-By` trailer). **Do NOT use `/submit-mr`; do not push or open a PR unless the user asks.**
**Also confirm with the user** whether to fold in untracking the six tracked `.idea/*` files (`git rm --cached -r .idea`) — spec §10 open item.

- [ ] **Step 6: ✅ Checkpoint.**

---

## Self-review — spec coverage

| Spec section | Covered by |
|---|---|
| §6.1 Captured real fixtures, keyed by `source.id` | Task 1 |
| §6.2 Comprehensive parser tests (CDATA, isPermaLink guid, flat author, **media:thumbnail**) | Task 3 |
| §6.3 Real-date mapper tests (single-digit day, zone name) + `parseDate` fix | Task 2 |
| §6.4 "No source silently empty" guard parameterized over `FeedSources.all` | Task 4 |
| §6.5 Remove dead TIA Transfers | Task 4 (Step 3) |
| §7 Unit-only, `/write-unit-tests` conventions, coverage pass | Tasks 2–4 (conventions), Task 5 (coverage) |
| §10 `.idea` untrack open item | Task 5 (Step 5) |

**Type-consistency check:** `RawFeedItem(...)` constructor args, `Classification(isMatch, category, tier, confirmed)`, `FeedSource(id, name, url, defaultTier)`, `toEntity(sourceName, classification)`, and `NewsItemEntity.publishedAtEpochMs` are used identically across Tasks 1–4 and match the production sources read in the spec. Fixture filenames `source_<id>.xml` match the `source.id` values (`this_is_anfield`, `liverpool_echo`, `bbc_liverpool`) used by the guard's `feeds/source_${source.id}.xml` lookup.

**Flagged for the engineer:**
- `RFC_1123_DATE_TIME` validates the day-of-week — keep fixture/test weekdays correct (Mon 1 / Tue 2 / Wed 3 Jun 2026). It recognizes `GMT` and numeric offsets but not arbitrary names like `EST`; real LFC feeds use `GMT`/`+0000`, so this is sufficient (note, don't over-engineer).
- kover/detekt are not wired into the build (Task 5, Step 3) — surface to the user rather than scaffolding tooling unasked.
- The dead-source removal keeps `tia_transfers`'s transfer coverage via the main This Is Anfield feed; re-adding curated sources is a separate future door.
