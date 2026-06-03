# Feed Verification & Repair — Design (as built)

- **Date:** 2026-06-03
- **Status:** Implemented on `fix/feed-verification-and-repair`
- **Scope:** Door 1 of a three-part news-pipeline investigation (see *Future doors*)

This document describes the design **as built**. It is the design record — it reflects what shipped, not earlier alternatives that were considered and dropped.

---

## 1. Background

An analysis of the news pipeline surfaced three independent problems:

1. **News sources** — only This Is Anfield appeared in the feed, despite four sources being configured.
2. **De-duplication** — the same story from multiple sources is not collapsed.
3. **Match-watched** — the "mark as watched" affordance is driven by a text classifier, not real fixtures.

These are separate efforts. **This door covers #1: news sources.** #2 and #3 get their own specs.

## 2. Evidence & root cause

A live probe of the four configured sources returned: This Is Anfield 200/16 items; **TIA Transfers 404 (dead)**; Liverpool Echo 200/25 items; BBC Sport 200/24 items. Three of four delivered ~65 items, yet only This Is Anfield was visible. The probe also exposed the real-world date formats:

- BBC `pubDate`: `Wed, 03 Jun 2026 06:26:52 GMT` — a zone **name**, not a numeric offset.
- Echo `pubDate`: `Wed, 3 Jun 2026 08:15:56 +0000` — a **single-digit** day.

**Root cause:** the date parser used `DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z")`, which rejects single-digit days and zone names, sending those items to `Instant.EPOCH`. The feed is ordered `publishedAtEpochMs DESC`, so BBC/Echo items sank beneath every correctly-dated This Is Anfield item — present in the database, invisible in the UI.

**Why the tests never caught it:** the parser/mapper tests used **synthetic fixtures** whose dates used the one format that parses cleanly (`Mon, 01 Jun 2026 09:00:00 +0000`, ISO Atom). The suite verified a world that does not exist. The gap was the *realism* of the fixtures, not the absence of tests.

## 3. What was built

### 3.1 Hermetic real-payload fixtures + a structural guard

- Real captured payloads pinned as per-source test resources, keyed by `source.id`: `feeds/source_this_is_anfield.xml`, `source_liverpool_echo.xml`, `source_bbc_liverpool.xml`. Realistic, frozen, no network.
- **`FeedSourcesGuardTest`** — parameterized over `FeedSources.all`: each configured source must have a fixture that parses (via the real `FeedParser` → `toEntity` path) to **≥1 item with a non-EPOCH date**. A missing fixture, an unparseable format, or all-EPOCH dates turn the build red. This makes comprehensiveness *enforceable*: you cannot add a source — or ship a date format the parser can't read — without proving it parses. It would have been red on the original bug from day one.

### 3.2 Robust feed-date parsing (`parseFeedDate`, kotlinx-datetime)

A dedicated unit `FeedDate.kt` exposes `internal fun parseFeedDate(raw: String?): Long` (epoch millis, `0L` if unparseable), built on **kotlinx-datetime 0.7.1**. Feed dates are RFC-822/1123 (RSS) or ISO-8601 (Atom) — not arbitrary — and every assumption is **explicit**:

- The leading weekday is **stripped and never cross-validated**, so a feed with a wrong or missing weekday still parses (the old `RFC_1123_DATE_TIME` validated it and silently failed).
- Internal/folded whitespace is collapsed; single-digit days and absent seconds are tolerated.
- Time-zone **abbreviations are mapped explicitly** to offsets — `GMT/UTC/UT/Z`→+0000, `BST`/`CET`→+0100, `CEST`→+0200, `EST`→−0500, `EDT`→−0400, `CST`→−0600, `CDT`→−0500, `MST`→−0700, `MDT`→−0600, `PST`→−0800, `PDT`→−0700 — plus native numeric offsets. An abbreviation **not** in the map yields `0L` rather than a silent guess.
- Parse order: RFC-822/1123 (via a custom `DateTimeComponents.Format`) → ISO-8601 (`ISO_DATE_TIME_OFFSET`, for Atom) → `0L`.

`NewsMappers.toEntity` calls `parseFeedDate(pubDate)`. Behaviour is pinned by an 18-row matrix in `FeedDateTest` (named zones, single/double-digit days, missing & wrong weekday, folded whitespace, lowercase zones, absent seconds, numeric offsets, ISO, garbage→0L).

### 3.3 BBC images

`FeedParser` now reads `media:thumbnail` (BBC's only image tag) in addition to `media:content`/`enclosure`. Document order still wins, so feeds carrying both (Liverpool Echo) keep their larger image.

### 3.4 Dead source removed

TIA Transfers (a hard 404) was removed from `FeedSources.all`, leaving three live, fixtured sources. The TIA main feed already carries transfer news.

## 4. Testing

- **Layer:** pure data layer → **unit tests only** (per the Android test-selection matrix; nothing visual changed). Parser & guard run under Robolectric (`android.util.Xml`); the date/mapper tests are plain JVM.
- **Conventions:** Truth, JUnit4, fakes over mocks.
- **Result:** 92 tests pass; `assembleDebug` clean.
- **Coverage tooling:** kover and detekt are **not** configured in this build. Coverage is enforced *structurally* by the comprehensive real-payload fixtures + the source guard, rather than by a tooling gate.

## 5. Decisions (final)

| Decision | Outcome |
|---|---|
| Feed-health observability | **Tests**, not a runtime logger (a logger is a human-read flashlight, not verification). No live network test for now — it needs CI to have teeth, deferred to a future door. |
| Date engine | **kotlinx-datetime 0.7.1** with an explicit zone map and no weekday validation. |
| Dead TIA Transfers source | **Removed.** |
| Branch / process | Ticketless `fix/` branch (this project doesn't use YouTrack); commits via raw `git` (this project doesn't use `/submit-mr`). |

## 6. Future doors (separate specs)

1. **News sources, continued** — add curated sources (LFC.com, The Athletic, Guardian…) with `Tier` assignments; a live source-health check wired to CI (the only check a frozen fixture can't perform — e.g. a URL dying like TIA Transfers).
2. **Cross-source de-duplication** — collapse the same story reported by multiple feeds.
3. **Fixtures-aware match-watched** — a real fixture concept (opponent, kickoff, result) so spoiler protection and "mark watched" track actual games, not keyword-matched articles.
