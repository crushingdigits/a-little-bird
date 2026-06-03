# News Sources & Reliability Tier — Design

- **Date:** 2026-06-03
- **Status:** Approved (brainstormed) — not yet implemented
- **Branch:** `feat/news-sources-and-tier-reliability`
- **Scope:** Door 1 *continued* from the feed-verification effort — add curated sources and make the tier system coherent.

This is a design record agreed during brainstorming. It precedes the implementation plan.

---

## 1. Background

The feed-verification door ([2026-06-03-feed-verification-and-repair](2026-06-03-feed-verification-and-repair-design.md)) listed a next step: *"add curated sources (LFC.com, The Athletic, Guardian…) with `Tier` assignments."* This effort delivers that, plus a redefinition of what a **tier** means — prompted by the observation that **BBC should be Tier 1** because it is an inherently reputable publication, which the current model does not allow.

Two things change together because they are entangled:

1. **New sources** — David Lynch, Fabrizio Romano (via CaughtOffside), and The Guardian.
2. **Tier model** — a single reliability axis resolved from *several factors*, so an institution like the BBC can be inherently Tier 1 while an individual journalist is Tier 1 *only under certain conditions*.

## 2. The guiding principle

> *"The reliability tier is a single value, but it is evaluated by several factors — BBC news is inherently reliable, but news from journalists is reliable only under certain conditions."* — the user

A tier remains **one number per item** (one badge, not two). It is no longer a pure transfer-ITK score; it is **overall reliability**, derived from two kinds of trust:

- **Institutional reliability.** Some outlets are *inherently* reliable: the BBC, The Guardian. Their items sit at a high floor and are **not downgraded** by cautious wording — the institution's editorial process is the guarantee.
- **Conditional journalist reliability.** An individual reporter (Ornstein, Romano, Lynch) is trusted **only when their language is definitive**. The same byline yields Tier 1 when they confirm a deal and Tier 2 when they merely speculate.

## 3. The official Liverpool website — explicitly out of scope

The original ask included the **official club site (liverpoolfc.com)**. A live probe found **no working RSS/Atom feed**: `/news/rss`, `/rss`, and `?service=rss` all 404, and the legacy feed page now redirects to the homepage. Under this app's RSS-only architecture, guarded by `FeedSourcesGuardTest` (every source must have a parseable real-payload fixture), the official site **cannot be added today**. It is dropped from scope by decision.

> Note: the existing `liverpool_echo` source points at `liverpool.com` — that is **Reach plc's** network, not the official club. The Reach "transfer news" feed was considered as a proxy and **rejected** (not official; overlaps the existing Echo source).

## 4. Sources to add

`FeedSources.all` gains three entries. Each requires a real captured fixture (`feeds/source_<id>.xml`); the parameterized `FeedSourcesGuardTest` then enforces that it parses to ≥1 dated item.

| id | name | url | floor tier | club-scoped? |
|---|---|---|---|---|
| `david_lynch` | David Lynch | `https://www.davidlynchlfc.co.uk/feed` | `TIER_2_BEAT` | ✅ yes |
| `fabrizio_romano` | Fabrizio Romano | `https://www.caughtoffside.com/author/fabrizio-romano/feed/` | `TIER_2_BEAT` | ❌ **no** |
| `guardian_liverpool` | The Guardian — Liverpool | `https://www.theguardian.com/football/liverpool/rss` | **`TIER_1_RELIABLE`** | ✅ yes |

- **David Lynch** — verified, free, 100% Liverpool, dedicated correspondent. Floor T2; his confirmation-language items lift to T1 (added to the trusted-journalist set).
- **Fabrizio Romano (CaughtOffside)** — verified, free; the only clean Romano-only feed. `dc:creator = "Fabrizio Romano"`, which the parser already reads. Floor T2; language gates the lift. **Transfer-wide, not Liverpool-only** → relevance filter applies (§5).
- **The Guardian — Liverpool** — reputable broadsheet, topic-scoped to Liverpool. An *inherently reliable* institution → floor **T1**. **Verified live 2026-06-03** via direct fetch: HTTP 200, valid RSS 2.0, 20 Liverpool items, `pubDate` in RFC-822 named-zone form (`Wed, 03 Jun 2026 12:31:14 GMT`) which `parseFeedDate` already handles, `dc:creator` bylines present, `media:content` images present. Fallback if it ever dies: `https://www.theguardian.com/football/rss`.

The parser ([FeedParser.kt](../../../app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/remote/FeedParser.kt)) already extracts `dc:creator` and Atom `<author><name>`, so journalist bylines on these feeds are visible to the resolver with no parser change.

## 5. Liverpool relevance filter (new, small)

Romano's CaughtOffside feed is transfer-wide. Left raw it would inject non-Liverpool stories into a Liverpool reader. Therefore:

- `FeedSource` gains `clubScoped: Boolean = true`.
- `fabrizio_romano` sets `clubScoped = false`; every existing/other source stays `true`.
- A `LiverpoolRelevanceFilter` runs at **ingestion time** (in the remote data source, after parse, before classify/persist). For non-club-scoped sources only, it keeps items whose normalized `title + description` contains any term in a config allowlist: `liverpool`, `anfield`, `reds`, `lfc`, and the current manager surname (e.g. `slot`). Club-scoped sources bypass the filter entirely.
- The allowlist is config data (lives beside `TierConfig`), tunable without code changes.

## 6. Tier resolution algorithm

`TierResolver.resolve(defaultTier, author, text)` is rewritten. Working on normalized `author` and `haystack = author + " " + text`, in **precedence order**:

1. **Trusted journalist present** *and* **confirmation language present** → `TIER_1_RELIABLE`, `confirmed = true`.
2. **Tabloid author** → `TIER_3_SPECULATIVE` (a journalist name-drop alone does not rescue a tabloid item — only rule 1's confirmation does).
3. **Trusted journalist present** *and* **tentative language present** → `TIER_2_BEAT`, **but never below the source's floor** (a `TIER_1` institution stays `TIER_1`).
4. **Otherwise** → `defaultTier` (BBC and Guardian rest at their `TIER_1` floor; beats at `TIER_2`).

"Never below the floor" matters only for T1-floor sources: it encodes *institutional reliability is not downgraded by cautious wording* (BBC stays T1 even when an item quotes a tentative journalist). Journalist-owned feeds (Lynch, Romano) have a T2 floor, so their tentative items resolve to T2.

`confirmed = (resolvedTier == TIER_1_RELIABLE) && confirmationLanguagePresent` — broadened from the single `"here we go"` phrase to the confirmation set, so "done deal / official / completed" also light the pill.

## 7. Config data (`TierConfig`, all tunable)

- **`bbc_liverpool` floor: `TIER_2_BEAT` → `TIER_1_RELIABLE`.** *Accepted consequence: every BBC item now reads RELIABLE, including match reports and previews — the price of treating the institution as inherently reliable.*
- **Trusted journalists:** `david ornstein`, `fabrizio romano`, **`david lynch`** (new).
- **Confirmation phrases** (multi-word, to avoid bare-word false positives like "win confirmed"): `"here we go"`, `"done deal"`, `"deal done"`, `"deal agreed"`, `"agreement reached"`, `"medical completed"`, `"medical booked"`, `"officially signed"`, `"transfer confirmed"`, `"deal confirmed"`, `"completed the signing"`, `"sealed"`. (Final list tuned against real fixtures.)
- **Tentative phrases** (cap a trusted journalist at T2): `"interested in"`, `"keen on"`, `"linked with"`, `"in talks"`, `"monitoring"`, `"tracking"`, `"eyeing"`, `"considering"`, `"reportedly"`, `"could sign"`, `"target"`, `"rumour"`, `"rumor"`. (Final list tuned against real fixtures.)
- **Tabloids:** unchanged (`daily mirror`, `the sun`, `the mirror`, `mirror`, `sun`).
- **Labels:** **unchanged** — `RELIABLE` / `BEAT` / `SPECULATIVE`. No UI churn; under the new model T2 ("BEAT") reads as "credible but unconfirmed," which still fits.

## 8. Testing

**Layer:** pure data layer → **unit tests only** (per the Android test-selection matrix; nothing visual changes — `TierChip` already renders all three tiers). Parser/guard run under Robolectric; resolver/filter/mapper are plain JVM. Conventions: Truth, JUnit4, fakes over mocks. Follow the realism lesson from the prior door — **use real captured payloads**, not synthetic fixtures.

- **`FeedSourcesGuardTest`** — auto-covers the three new sources once `feeds/source_david_lynch.xml`, `feeds/source_fabrizio_romano.xml`, `feeds/source_guardian_liverpool.xml` are captured.
- **`TierResolverTest`** — expanded matrix:
  - trusted journalist + confirmation → T1 + `confirmed`
  - trusted journalist + tentative → T2
  - tabloid + journalist name-drop (tentative) → T3
  - tabloid + journalist + confirmation → T1 (rule 1 wins)
  - BBC institution + tentative journalist quote → stays T1 (floor not downgraded)
  - neutral journalist item → falls to floor
  - false-positive guard: "win confirmed" does **not** fire the pill
- **`LiverpoolRelevanceFilterTest`** (new) — non-club-scoped Romano item without a Liverpool term is dropped; a Liverpool item is kept; club-scoped sources bypass the filter.
- **A BBC-tier assertion** — a real BBC fixture item resolves to `TIER_1_RELIABLE`.
- **`NewsClassifier` / mapper** tests updated as needed for the new resolver signature.

**Coverage:** enforced structurally (real fixtures + guard), consistent with the prior door; kover/detekt are not configured in this build.

## 9. Known edges (flagged, not solved here)

| Edge | Disposition |
|---|---|
| Guardian feed | **Verified live 2026-06-03** (HTTP 200, 20 Liverpool items, named-zone GMT dates parse). Fallback `/football/rss` if it ever dies. |
| Cross-source duplication (Guardian/BBC/Echo same story) | Out of scope — de-dup is its own future door; expect some repeats meanwhile. |
| Live source-health check (URL dying like TIA Transfers) | Still deferred to a CI-wired future door; frozen fixtures can't catch a dead URL. |
| Confirmation/tentative phrase false positives | Mitigated by multi-word phrases + real-headline tests; lists are tunable config. |

## 10. Decisions (final)

| Decision | Outcome |
|---|---|
| What a tier means | **Single reliability axis**, evaluated from institutional + conditional-journalist factors. |
| Official liverpoolfc.com | **Out of scope** — no RSS feed exists. |
| Sources added | David Lynch, Fabrizio Romano (CaughtOffside), The Guardian — Liverpool. |
| BBC tier | **Promoted to `TIER_1_RELIABLE`** (institution inherently reliable). |
| Guardian tier | **`TIER_1_RELIABLE` floor** (institution inherently reliable). |
| Journalist reliability | **Three-state by language**: confirmation → T1; tentative → T2; otherwise floor. |
| Non-club-scoped feeds | **Liverpool relevance filter** at ingestion (`clubScoped` flag). |
| Labels | **Unchanged** (RELIABLE / BEAT / SPECULATIVE). |
| Branch / process | Ticketless `feat/` branch; commits via raw `git` (this project doesn't use `/submit-mr`). |
