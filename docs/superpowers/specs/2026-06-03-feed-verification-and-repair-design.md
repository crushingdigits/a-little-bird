# Feed Verification & Repair — Design Spec

- **Date:** 2026-06-03
- **Status:** Approved (brainstorming complete; pending implementation plan)
- **Branch:** `fix/feed-verification-and-repair`
- **Scope:** Door 1 of a three-part news-pipeline investigation (see *Future doors*)

---

## 1. Background

An analysis of the news pipeline surfaced three independent problems wearing one coat:

1. **News sources** — only This Is Anfield appears in the feed, despite four sources being configured.
2. **De-duplication** — the same story from multiple sources is not collapsed.
3. **Match-watched** — the "mark as watched" affordance is driven by a text classifier, not by real fixtures, so it appears even when no game has been played.

These are three separate efforts. **This spec covers only Door 1: news sources.** Doors 2 and 3 get their own specs.

## 2. Evidence

A live probe of the four configured sources in [`FeedSources.all`](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt) returned:

| Source | Result | Items |
|---|---|---|
| This Is Anfield | 200, valid RSS | 16 |
| **TIA — Transfers** | **404** (HTML error page served with an `rss+xml` content-type) | **0 — dead** |
| Liverpool Echo (`liverpool.com?service=rss`) | 200, valid RSS | 25 |
| BBC Sport — Liverpool | 200, valid XML | 24 |

So at the network layer, **three of four sources deliver ~65 items**. Yet only This Is Anfield is visible. The probe also revealed the real-world feed shapes:

- BBC `pubDate`: `Wed, 03 Jun 2026 06:26:52 GMT` — zone **name**, not a numeric offset.
- Echo `pubDate`: `Wed, 3 Jun 2026 08:15:56 +0000` — **single-digit** day.

### Why the test suite never caught it

The parser/mapper tests pass against **synthetic fixtures** whose dates use the one format that parses cleanly:

- `app/src/test/resources/feeds/rss_sample.xml` → `Mon, 01 Jun 2026 09:00:00 +0000` (zero-padded day, numeric offset).
- `app/src/test/resources/feeds/atom_sample.xml` → ISO `2026-06-01T07:00:00Z`.
- [`NewsMappersTest`](../../../app/src/test/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappersTest.kt) asserts only `publishedAtEpochMs > 0` against a zero-padded `+0000` date.

The verification ran perfectly — against a world that does not exist. The single-digit-day and zone-name formats used by real feeds never appeared in a single test. **The gap is comprehensiveness of the fixtures, not the absence of tests.**

## 3. Root-cause analysis

### Primary: date-parse strictness → items exiled to EPOCH

[`parseDate`](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappers.kt) uses `DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")`:

- `dd` requires **two** digits → fails on Echo's `3 Jun`.
- `Z` requires a numeric **offset** → fails on BBC's `GMT`.

Both fall through the recover path (`OffsetDateTime.parse(value)` — ISO only, also fails) to `Instant.EPOCH`. The feed is ordered `ORDER BY publishedAtEpochMs DESC` ([`NewsDao`](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/NewsDao.kt)), so EPOCH-dated BBC/Echo items sink **beneath every correctly-dated This Is Anfield item** — present in the database, effectively invisible in the UI.

This is the leading hypothesis for "I only see This Is Anfield." The fixture tests below confirm or refute it deterministically.

### Secondary: parser drops BBC images

[`FeedParser`](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParser.kt) reads images from `media:content` and `enclosure`, but BBC supplies `media:thumbnail` — so BBC items carry no image. Cosmetic, surfaced by the real fixture.

### Tertiary: a genuinely dead source

TIA Transfers (`.../liverpool-fc-transfer-rumours/feed/`) is a hard 404. A frozen fixture cannot detect a *live* URL dying — that blind spot is acknowledged under *Non-goals*.

## 4. Goals

- Make feed verification **comprehensive and hermetic**: tests exercise real-world feed formats from captured payloads, with no network access.
- **Fix** the bugs those tests expose (date parsing; BBC image tag).
- Make "a configured source silently contributes nothing" **impossible to ship** via a structural guard.
- Remove the dead source.

## 5. Non-goals (YAGNI)

- **Runtime device logger** — considered and dropped. A flashlight a human reads after the fact is not verification.
- **Live network test / CI monitor** — deferred. Network tests are inherently flaky and need automation (CI) to have teeth; no CI exists yet. Detecting a *live* URL death (e.g. the Transfers 404 recurring on another source) is left to a future CI door.
- **User-facing source UI**, **new sources**, **cross-source de-duplication**, **fixtures/match-watched** — separate doors.

## 6. Design

### 6.1 Captured real fixtures ("mock data")

Pin a real probe response per **remaining** source as a test resource, keyed to the source `id` so the guard (§6.4) can map each source to its fixture — e.g. `app/src/test/resources/feeds/source_this_is_anfield.xml`, `source_liverpool_echo.xml`, `source_bbc_liverpool.xml`. These are realistic, frozen, and require no network; they become the source of truth the tests verify against. (The existing synthetic `rss_sample.xml` / `atom_sample.xml` stay for targeted format-edge tests, but the per-source fixtures are real captures.)

### 6.2 Comprehensive parser tests

Extend [`FeedParserTest`](../../../app/src/test/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParserTest.kt) against the real fixtures and assert the real-world quirks are handled:

- Full item count extracted from each fixture.
- CDATA-wrapped titles/descriptions.
- `<guid isPermaLink="false">`.
- Flat `<author>` text.
- **`media:thumbnail`** image extraction (currently dropped) → fix the parser to read it.

### 6.3 Comprehensive mapper / date tests — the failing test first

Extend [`NewsMappersTest`](../../../app/src/test/java/dev/crushingdigits/littlebird/data/news/mapper/NewsMappersTest.kt) with the real date strings:

- `"Wed, 3 Jun 2026 08:15:56 +0000"` (single-digit day)
- `"Wed, 03 Jun 2026 06:26:52 GMT"` (zone name)

Assert each resolves to a **non-EPOCH** `Instant`. These fail against the current `parseDate` — that red is the point.

**Fix:** broaden `parseDate` to handle real RSS pubDates. Recommended approach: parse RSS dates with `DateTimeFormatter.RFC_1123_DATE_TIME` (tolerates single-/double-digit days *and* both zone names and numeric offsets), fall back to ISO-8601 for Atom, and reserve `Instant.EPOCH` strictly for genuinely unparseable input. The tests drive the exact implementation.

### 6.4 The "no source silently empty" guard — the structural fix

A test parameterized over `FeedSources.all`: for each source, load its fixture (resolved by `source.id` → `feeds/source_<id>.xml`) and assert it parses to **≥1 dated item** (non-EPOCH). A missing fixture for a configured source is itself a failure. Adding a source without a fixture, or shipping a format the parser/mapper can't read, turns the build red. This makes comprehensiveness enforceable rather than aspirational — silence can no longer be shipped.

### 6.5 Remove the dead source

Remove TIA Transfers from [`FeedSources.all`](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/source/FeedSources.kt). The TIA main feed already carries transfer news; curating/adding sources belongs to a later door.

## 7. Testing strategy

- **Layer:** pure data layer (parser, mapper, source list) → **unit tests only**, per the Android test-selection matrix. Nothing visual changes — no screenshot or UI-emulator tests.
- **Conventions:** per `/write-unit-tests` (Truth, JUnit 4, fakes over mocks, lazy init).
- **Coverage:** closed with kover at the end of implementation; detekt clean.
- **Reference:** the [`quotanoia`](https://github.com/crushingdigits/quotanoia) repo is the reference for testing/setup config; consult it when wiring test config if a setup question arises.

## 8. Decision log

| # | Decision | Choice |
|---|---|---|
| Visibility | How visible should feed health be? | **Dev/test only** — no user-facing UI |
| Scope | Instrument only, or also fix? | **Diagnose & fix** |
| Observability surface | Runtime logger vs tests | **Tests** (hermetic, captured data); runtime logger **dropped** |
| Live check | In default suite / separate task / with CI | **Dropped for now**; CI is a future door |
| Dead source | Remove vs replace TIA Transfers | **Remove** |
| Branch / process | YouTrack ticket? | **Not used on this project** — ticketless `fix/` branch |

## 9. Future doors (separate specs)

1. **News sources, continued** — add real new sources (LFC.com, The Athletic, Guardian, etc.) with proper `Tier` assignments; later, a live source-health check wired to CI.
2. **Cross-source de-duplication** — collapse the same story reported by multiple feeds (title/URL/entity clustering).
3. **Fixtures-aware match-watched** — introduce a real fixture concept (opponent, kickoff, result) and tie spoiler protection + "mark watched" to actual games rather than keyword-matched articles.

## 10. Open items

- **`.idea` already tracked:** six `.idea/*` files are tracked in git; `.gitignore` now ignores `/.idea/` but tracked files persist. Untracking via `git rm --cached -r .idea` can ride along in this branch's MR if desired (pending user confirmation).
