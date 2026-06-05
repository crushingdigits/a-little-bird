# Spoiler Wall — Per-Game Watched State + Fixtures Schedule — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bulk "mark match as watched" with a real per-game spoiler wall: fetch a Liverpool fixtures schedule (TheSportsDB), mark each game watched, and hide all feed news published at/after the earliest unwatched played game's kickoff.

**Architecture:** New `data/schedule/` package (DTO → `ScheduleDataSource` → Room `games` table → `ScheduleRepository` → `Game` domain model) behind a swappable interface, parsed with kotlinx.serialization. Watched-state + sync timestamps in DataStore. Pure domain (`ComputeSpoilerWall`, `SeedWatchedOnFirstSync`) + orchestration (`SyncSchedule`). `FeedViewModel` redacts by a date wall; a `ProtectionCard` opens a `MatchWallSheet`. The score is preserved in data but rendered only for watched games (centralized gate).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, kotlinx.serialization, OkHttp; tests with JUnit4, Truth, Turbine, Robolectric, Roborazzi, MockWebServer, UIAutomator 2.4.

**Spec:** `docs/superpowers/specs/2026-06-05-spoiler-wall-watched-games-design.md`

---

## Conventions (follow exactly — verified from existing code)

- **Use case file:** `fun interface Name { … }` + `class NameUseCase @Inject constructor(...) : Name` in the **same file**; bind with `@Binds` in `DomainModule`. (Do NOT use `@AutoBind/@Reusable` — the project uses plain classes + manual binds.)
- **Unit test:** Truth `assertThat`, Turbine `.test { }`, `kotlinx.coroutines.test.runTest`, `@get:Rule val mainRule = MainDispatcherRule()` for ViewModels, **hand-written fakes** implementing interfaces (no mocking framework).
- **DAO test:** `@RunWith(RobolectricTestRunner::class)`, `Room.inMemoryDatabaseBuilder(...)`, Turbine.
- **DataStore test:** real `PreferenceDataStoreFactory.create` over a `TemporaryFolder`.
- **HTTP test:** `okhttp3.mockwebserver.MockWebServer`.
- **Screenshot test:** `@RunWith(RobolectricTestRunner::class)`, `@GraphicsMode(NATIVE)`, `@Config(application = android.app.Application::class, qualifiers = RobolectricDeviceQualifiers.Pixel8a)`, the `captureThemed("ClassName_case.png") { … }` helper.
- **Test run commands:**
  - One test: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.<FQN>"`
  - Record screenshots: `./gradlew :app:recordRoborazziDebug`
  - Verify screenshots: `./gradlew :app:verifyRoborazziDebug`
  - Emulator tests (needs device): `./gradlew :app:connectedDebugAndroidTest`
- **Package root:** `dev.crushingdigits.littlebird`.
- **No AI attribution** in any commit message.

---

## File Structure

**New — data:**
- `data/schedule/datasource/remote/ScheduleDataSource.kt` — interface `fetchFixtures(): List<GameDto>`.
- `data/schedule/datasource/remote/GameDto.kt` — `@Serializable` DTOs + response wrappers.
- `data/schedule/datasource/remote/TheSportsDbConfig.kt` — base URL, key, team id.
- `data/schedule/datasource/remote/TheSportsDbScheduleDataSource.kt` — impl (FeedFetcher + Json).
- `data/schedule/datasource/local/GameEntity.kt` — Room `games` row.
- `data/schedule/datasource/local/GameDao.kt` — `observeAll/upsertAll/count`.
- `data/schedule/datasource/local/ScheduleLocalDataSource.kt` — thin wrapper (mirrors `NewsLocalDataSource`).
- `data/schedule/mapper/ScheduleMappers.kt` — `GameDto.toEntity()`, `GameEntity.toDomain()`, kickoff parsing.
- `data/schedule/ScheduleRepository.kt` — interface + impl.

**New — domain:**
- `domain/model/Game.kt` — `Game`, `GameStatus`, `MatchResult`.
- `domain/usecase/ComputeSpoilerWall.kt`, `SeedWatchedOnFirstSync.kt`, `ObserveSchedule.kt`, `ObserveWatchedGames.kt`, `ObserveLastSyncAt.kt`, `MarkGameWatched.kt`, `RestoreWatchedGames.kt`, `SyncSchedule.kt`.

**New — UI:**
- `ui/components/ProtectionCard.kt` — entry point (replaces `ProtectionBanner` + `CatchUpCard`).
- `ui/feed/wall/MatchWallSheet.kt` — fixtures sheet.
- `ui/feed/wall/FixtureRow.kt` — row + crest + score trail.
- `ui/feed/wall/CompetitionTag.kt` — league → short tag helper.
- `ui/feed/FixtureUi.kt` — UI model (score gated).

**Modified:**
- `data/preferences/datasource/PreferencesDataSource.kt`, `data/preferences/PreferencesRepository.kt` — watched games + sync timestamps.
- `data/news/datasource/local/LittleBirdDatabase.kt` — register `GameEntity`, bump version, destructive fallback.
- `di/DatabaseModule.kt`, `di/DataBindingsModule.kt`, `di/NetworkModule.kt`, `di/DomainModule.kt` — wiring + `Json` + `Clock`.
- `ui/feed/FeedViewModel.kt`, `ui/feed/FeedUiState.kt`, `ui/feed/FeedScreen.kt` — wall redaction + sheet.
- `ui/components/GateSheet.kt` — date-based copy.
- `androidTest/.../FakeDataModule.kt`, `androidTest/.../SpoilerFlowUiTest.kt` — new prefs methods + new flow.

**Deleted:**
- `domain/usecase/MarkWatched.kt` + `test/.../domain/usecase/MarkWatchedTest.kt`.
- `ui/components/CatchUpCard.kt`, `ui/components/CatchUpSheet.kt`, `ui/components/ProtectionBanner.kt`.

---

## Phase 1 — Schedule fetch (DTO + remote data source)

### Task 1.1: Provide a `Json` instance

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt`

- [ ] **Step 1: Add the provider**

In `NetworkModule`, add these imports and provider:

```kotlin
import kotlinx.serialization.json.Json
```

```kotlin
    @Provides
    @Singleton
    fun json(): Json = Json { ignoreUnknownKeys = true }
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt
git commit -m "feat: provide kotlinx.serialization Json for schedule parsing"
```

### Task 1.2: Schedule DTOs

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/GameDto.kt`

- [ ] **Step 1: Write the DTOs**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One fixture/event as returned by TheSportsDB. Scores are strings ("2") or null until played. */
@Serializable
data class GameDto(
    @SerialName("idEvent") val id: String,
    @SerialName("strHomeTeam") val homeTeam: String = "",
    @SerialName("strAwayTeam") val awayTeam: String = "",
    @SerialName("intHomeScore") val homeScore: String? = null,
    @SerialName("intAwayScore") val awayScore: String? = null,
    @SerialName("strLeague") val league: String? = null,
    @SerialName("strTimestamp") val timestamp: String? = null,
    @SerialName("dateEvent") val date: String? = null,
    @SerialName("strTime") val time: String? = null,
)

/** `eventslast.php` wraps fixtures under "results"; `eventsnext.php` under "events". */
@Serializable
data class EventsLastResponse(@SerialName("results") val results: List<GameDto>? = null)

@Serializable
data class EventsNextResponse(@SerialName("events") val events: List<GameDto>? = null)
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/GameDto.kt
git commit -m "feat: add TheSportsDB schedule DTOs"
```

### Task 1.3: Config + `ScheduleDataSource` interface

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbConfig.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/ScheduleDataSource.kt`

- [ ] **Step 1: Write the config**

> Verify `teamId` (Liverpool) and the free `apiKey` against live TheSportsDB docs during implementation; defaults below are the documented public test values.

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.remote

data class TheSportsDbConfig(
    val baseUrl: String = "https://www.thesportsdb.com",
    val apiKey: String = "123",
    val teamId: String = "133602", // Liverpool FC
) {
    fun lastEventsUrl() = "$baseUrl/api/v1/json/$apiKey/eventslast.php?id=$teamId"
    fun nextEventsUrl() = "$baseUrl/api/v1/json/$apiKey/eventsnext.php?id=$teamId"
}
```

- [ ] **Step 2: Write the interface**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.remote

/** Fetches Liverpool fixtures (recent + upcoming). Throws on hard network failure. */
interface ScheduleDataSource {
    suspend fun fetchFixtures(): List<GameDto>
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbConfig.kt app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/ScheduleDataSource.kt
git commit -m "feat: add ScheduleDataSource interface and TheSportsDB config"
```

### Task 1.4: `TheSportsDbScheduleDataSource` (TDD)

**Files:**
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbScheduleDataSourceTest.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbScheduleDataSource.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.remote.OkHttpFeedFetcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class TheSportsDbScheduleDataSourceTest {
    private val server = MockWebServer()
    private lateinit var subject: TheSportsDbScheduleDataSource

    @Before fun setUp() {
        server.start()
        val config = TheSportsDbConfig(baseUrl = server.url("").toString().trimEnd('/'), apiKey = "123", teamId = "133602")
        subject = TheSportsDbScheduleDataSource(
            fetcher = OkHttpFeedFetcher(OkHttpClient()),
            json = Json { ignoreUnknownKeys = true },
            config = config,
        )
    }
    @After fun tearDown() = server.shutdown()

    @Test fun `parses last and next events and combines them`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"results":[{"idEvent":"1","strHomeTeam":"Liverpool","strAwayTeam":"Chelsea","intHomeScore":"2","intAwayScore":"1","strLeague":"English Premier League","strTimestamp":"2026-05-31T16:30:00+00:00"}]}""",
            ),
        )
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"events":[{"idEvent":"2","strHomeTeam":"Arsenal","strAwayTeam":"Liverpool","strLeague":"English Premier League","strTimestamp":"2026-06-14T17:30:00+00:00"}]}""",
            ),
        )

        val fixtures = subject.fetchFixtures()

        assertThat(fixtures.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(fixtures[0].homeScore).isEqualTo("2")
    }

    @Test fun `tolerates malformed json on one endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not json"))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"events":[{"idEvent":"2","strHomeTeam":"Arsenal","strAwayTeam":"Liverpool","strTimestamp":"2026-06-14T17:30:00+00:00"}]}""",
            ),
        )

        val fixtures = subject.fetchFixtures()

        assertThat(fixtures.map { it.id }).containsExactly("2")
    }

    @Test fun `throws when an endpoint returns an error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"events":[]}"""))

        val result = runCatching { subject.fetchFixtures() }

        assertThat(result.isFailure).isTrue()
    }
}
```

- [ ] **Step 2: Run it to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.datasource.remote.TheSportsDbScheduleDataSourceTest"`
Expected: FAIL — `TheSportsDbScheduleDataSource` is unresolved.

- [ ] **Step 3: Implement the data source**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.remote

import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import kotlinx.serialization.json.Json

/**
 * Fetches Liverpool fixtures from TheSportsDB (free tier: last + next events).
 * A hard network failure on either endpoint propagates (caller treats it as offline);
 * a malformed body for one endpoint is tolerated and contributes no fixtures.
 */
class TheSportsDbScheduleDataSource(
    private val fetcher: FeedFetcher,
    private val json: Json,
    private val config: TheSportsDbConfig = TheSportsDbConfig(),
) : ScheduleDataSource {

    override suspend fun fetchFixtures(): List<GameDto> {
        val lastBody = fetcher.fetch(config.lastEventsUrl()).getOrThrow()
        val nextBody = fetcher.fetch(config.nextEventsUrl()).getOrThrow()
        val last = runCatching { json.decodeFromString<EventsLastResponse>(lastBody).results.orEmpty() }
            .getOrDefault(emptyList())
        val next = runCatching { json.decodeFromString<EventsNextResponse>(nextBody).events.orEmpty() }
            .getOrDefault(emptyList())
        return last + next
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.datasource.remote.TheSportsDbScheduleDataSourceTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbScheduleDataSource.kt app/src/test/java/dev/crushingdigits/littlebird/data/schedule/datasource/remote/TheSportsDbScheduleDataSourceTest.kt
git commit -m "feat: implement TheSportsDbScheduleDataSource with kotlinx.serialization"
```

---

## Phase 2 — Persistence (Room games table)

### Task 2.1: `GameEntity` + `GameDao` (TDD on the DAO)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/GameEntity.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/GameDao.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/LittleBirdDatabase.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/GameDaoTest.kt`

- [ ] **Step 1: Write the entity**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoffEpochMs: Long,
    val homeScore: Int?,
    val awayScore: Int?,
)
```

- [ ] **Step 2: Write the DAO**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY kickoffEpochMs ASC")
    fun observeAll(): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(games: List<GameEntity>)

    @Query("SELECT COUNT(*) FROM games")
    suspend fun count(): Int
}
```

- [ ] **Step 3: Register the entity in the database (destructive migration)**

Replace the whole contents of `LittleBirdDatabase.kt`:

```kotlin
package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameDao
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameEntity

@Database(entities = [NewsItemEntity::class, GameEntity::class], version = 2, exportSchema = false)
abstract class LittleBirdDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun gameDao(): GameDao
}
```

> The news table is a disposable cache (re-fetched on refresh), so the DB uses destructive fallback (wired in Task 2.3) instead of a hand-written migration.

- [ ] **Step 4: Write the failing DAO test**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameDaoTest {
    private lateinit var db: LittleBirdDatabase
    private lateinit var dao: GameDao

    private fun game(id: String, ko: Long) = GameEntity(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "English Premier League", kickoffEpochMs = ko, homeScore = null, awayScore = null,
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LittleBirdDatabase::class.java).build()
        dao = db.gameDao()
    }
    @After fun tearDown() = db.close()

    @Test fun `upsert replaces by id and observeAll is kickoff-ascending`() = runTest {
        dao.upsertAll(listOf(game("b", ko = 200), game("a", ko = 100)))
        dao.upsertAll(listOf(game("a", ko = 100))) // duplicate id -> replaced

        dao.observeAll().test {
            assertThat(awaitItem().map { it.id }).containsExactly("a", "b").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dao.count()).isEqualTo(2)
    }
}
```

- [ ] **Step 5: Run the DAO test**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.datasource.local.GameDaoTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/ app/src/main/java/dev/crushingdigits/littlebird/data/news/datasource/local/LittleBirdDatabase.kt app/src/test/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/GameDaoTest.kt
git commit -m "feat: add games Room table, DAO, and register entity"
```

### Task 2.2: `ScheduleLocalDataSource`

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/ScheduleLocalDataSource.kt`

- [ ] **Step 1: Write the wrapper (mirrors `NewsLocalDataSource`)**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.datasource.local

import kotlinx.coroutines.flow.Flow

class ScheduleLocalDataSource(private val dao: GameDao) {
    fun observeAll(): Flow<List<GameEntity>> = dao.observeAll()
    suspend fun upsert(games: List<GameEntity>) = dao.upsertAll(games)
    suspend fun count(): Int = dao.count()
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/datasource/local/ScheduleLocalDataSource.kt
git commit -m "feat: add ScheduleLocalDataSource wrapper"
```

### Task 2.3: Wire DB providers + destructive fallback

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DatabaseModule.kt`

- [ ] **Step 1: Replace the module contents**

```kotlin
package dev.crushingdigits.littlebird.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsDao
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameDao
import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): LittleBirdDatabase =
        Room.databaseBuilder(ctx, LittleBirdDatabase::class.java, "little_bird.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun newsDao(db: LittleBirdDatabase): NewsDao = db.newsDao()

    @Provides
    fun gameDao(db: LittleBirdDatabase): GameDao = db.gameDao()

    @Provides
    @Singleton
    fun newsLocalDataSource(dao: NewsDao) = NewsLocalDataSource(dao)

    @Provides
    @Singleton
    fun scheduleLocalDataSource(dao: GameDao) = ScheduleLocalDataSource(dao)
}
```

> If `fallbackToDestructiveMigration(dropAllTables = true)` is unavailable in the project's Room version, use the deprecated no-arg `fallbackToDestructiveMigration()`.

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/DatabaseModule.kt
git commit -m "feat: provide gameDao + ScheduleLocalDataSource, destructive DB fallback"
```

---

## Phase 3 — Domain `Game` model + mappers

### Task 3.1: `Game` domain model

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/model/Game.kt`

- [ ] **Step 1: Write the model**

```kotlin
package dev.crushingdigits.littlebird.domain.model

import java.time.Instant

enum class GameStatus { PLAYED, UPCOMING }

enum class MatchResult { WIN, DRAW, LOSS }

/** A Liverpool fixture. Scores are present once the game has been played; `null` otherwise. */
data class Game(
    val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoff: Instant,
    val homeScore: Int?,
    val awayScore: Int?,
) {
    fun status(now: Instant): GameStatus =
        if (!kickoff.isAfter(now)) GameStatus.PLAYED else GameStatus.UPCOMING

    /** Liverpool's goals / opponent's goals from Liverpool's perspective, or null if not played. */
    val liverpoolScore: Int? get() = if (isHome) homeScore else awayScore
    val opponentScore: Int? get() = if (isHome) awayScore else homeScore

    /** Win/Draw/Loss from Liverpool's perspective. Null when the score is unknown. */
    fun result(): MatchResult? {
        val us = liverpoolScore ?: return null
        val them = opponentScore ?: return null
        return when {
            us > them -> MatchResult.WIN
            us < them -> MatchResult.LOSS
            else -> MatchResult.DRAW
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/model/Game.kt
git commit -m "feat: add Game domain model with status and result"
```

### Task 3.2: Schedule mappers (TDD)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/mapper/ScheduleMappers.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/schedule/mapper/ScheduleMappersTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.mapper

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.GameDto
import org.junit.Test
import java.time.Instant

class ScheduleMappersTest {

    private fun dto(
        id: String = "1",
        home: String = "Liverpool",
        away: String = "Chelsea",
        hs: String? = "2",
        as_: String? = "1",
        ts: String? = "2026-05-31T16:30:00+00:00",
    ) = GameDto(
        id = id, homeTeam = home, awayTeam = away, homeScore = hs, awayScore = as_,
        league = "English Premier League", timestamp = ts, date = null, time = null,
    )

    @Test fun `maps a home fixture from Liverpool's perspective`() {
        val e = dto().toEntity()!!
        assertThat(e.id).isEqualTo("1")
        assertThat(e.opponent).isEqualTo("Chelsea")
        assertThat(e.isHome).isTrue()
        assertThat(e.homeScore).isEqualTo(2)
        assertThat(e.awayScore).isEqualTo(1)
        assertThat(e.kickoffEpochMs).isEqualTo(Instant.parse("2026-05-31T16:30:00Z").toEpochMilli())
    }

    @Test fun `maps an away fixture and picks the opponent`() {
        val e = dto(home = "Arsenal", away = "Liverpool", hs = null, as_ = null).toEntity()!!
        assertThat(e.opponent).isEqualTo("Arsenal")
        assertThat(e.isHome).isFalse()
        assertThat(e.homeScore).isNull()
        assertThat(e.awayScore).isNull()
    }

    @Test fun `falls back to dateEvent plus strTime when timestamp is absent`() {
        val e = dto(ts = null).copy(date = "2026-05-31", time = "16:30:00").toEntity()!!
        assertThat(e.kickoffEpochMs).isEqualTo(Instant.parse("2026-05-31T16:30:00Z").toEpochMilli())
    }

    @Test fun `returns null when no kickoff can be parsed`() {
        assertThat(dto(ts = null).copy(date = null, time = null).toEntity()).isNull()
    }

    @Test fun `entity maps to domain and derives result`() {
        val game = dto().toEntity()!!.toDomain()
        assertThat(game.opponent).isEqualTo("Chelsea")
        assertThat(game.liverpoolScore).isEqualTo(2)
        assertThat(game.result()!!.name).isEqualTo("WIN")
    }
}
```

> `GameDto.copy(...)` is used in the test to override `date`/`time`, so keep `date` and `time` as DTO fields (Task 1.2 already does).

- [ ] **Step 2: Run it to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.mapper.ScheduleMappersTest"`
Expected: FAIL — `toEntity`/`toDomain` unresolved.

- [ ] **Step 3: Implement the mappers**

```kotlin
package dev.crushingdigits.littlebird.data.schedule.mapper

import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameEntity
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.GameDto
import dev.crushingdigits.littlebird.domain.model.Game
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

private const val LIVERPOOL = "Liverpool"

/** Prefer the ISO timestamp; fall back to dateEvent + strTime (assumed UTC). Null if neither parses. */
private fun parseKickoff(dto: GameDto): Instant? {
    dto.timestamp?.takeIf { it.isNotBlank() }?.let { ts ->
        runCatching { OffsetDateTime.parse(ts).toInstant() }.getOrNull()?.let { return it }
    }
    val date = dto.date?.takeIf { it.isNotBlank() } ?: return null
    val time = dto.time?.takeIf { it.isNotBlank() } ?: "00:00:00"
    return runCatching {
        LocalDate.parse(date).atTime(LocalTime.parse(time)).toInstant(ZoneOffset.UTC)
    }.getOrNull()
}

/** Maps a TheSportsDB event to a row, from Liverpool's perspective. Null if the kickoff is unparseable. */
fun GameDto.toEntity(): GameEntity? {
    val kickoff = parseKickoff(this) ?: return null
    val home = homeTeam.equals(LIVERPOOL, ignoreCase = true)
    val opponent = if (home) awayTeam else homeTeam
    return GameEntity(
        id = id,
        opponent = opponent,
        abbreviation = opponent.filter { it.isLetter() }.take(3).uppercase(),
        isHome = home,
        competition = league.orEmpty(),
        kickoffEpochMs = kickoff.toEpochMilli(),
        homeScore = homeScore?.toIntOrNull(),
        awayScore = awayScore?.toIntOrNull(),
    )
}

fun GameEntity.toDomain(): Game = Game(
    id = id,
    opponent = opponent,
    abbreviation = abbreviation,
    isHome = isHome,
    competition = competition,
    kickoff = Instant.ofEpochMilli(kickoffEpochMs),
    homeScore = homeScore,
    awayScore = awayScore,
)
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.mapper.ScheduleMappersTest"`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/mapper/ScheduleMappers.kt app/src/test/java/dev/crushingdigits/littlebird/data/schedule/mapper/ScheduleMappersTest.kt
git commit -m "feat: add schedule DTO/entity/domain mappers"
```

---

## Phase 4 — `ScheduleRepository`

### Task 4.1: Repository (TDD)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/data/schedule/ScheduleRepository.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/data/schedule/ScheduleRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.data.schedule

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.GameDto
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduleRepositoryTest {
    private lateinit var db: LittleBirdDatabase
    private lateinit var local: ScheduleLocalDataSource

    private class FakeRemote(var fixtures: List<GameDto>, var fail: Boolean = false) : ScheduleDataSource {
        override suspend fun fetchFixtures(): List<GameDto> =
            if (fail) throw RuntimeException("network") else fixtures
    }

    private fun dto(id: String) = GameDto(
        id = id, homeTeam = "Liverpool", awayTeam = "Opp$id", homeScore = null, awayScore = null,
        league = "English Premier League", timestamp = "2026-05-31T16:30:00+00:00", date = null, time = null,
    )

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LittleBirdDatabase::class.java).build()
        local = ScheduleLocalDataSource(db.gameDao())
    }
    @After fun tearDown() = db.close()

    @Test fun `refresh upserts fetched fixtures and observeSchedule emits them`() = runTest {
        val subject = ScheduleRepositoryImpl(FakeRemote(listOf(dto("1"), dto("2"))), local)

        val result = subject.refresh()

        assertThat(result.isSuccess).isTrue()
        subject.observeSchedule().test {
            assertThat(awaitItem().map { it.id }).containsExactly("1", "2")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `refresh returns failure on network error and leaves store empty`() = runTest {
        val subject = ScheduleRepositoryImpl(FakeRemote(emptyList(), fail = true), local)

        assertThat(subject.refresh().isFailure).isTrue()
        subject.observeSchedule().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `refresh skips fixtures with unparseable kickoff`() = runTest {
        val bad = dto("bad").copy(timestamp = null, date = null, time = null)
        val subject = ScheduleRepositoryImpl(FakeRemote(listOf(dto("1"), bad)), local)

        subject.refresh()

        subject.observeSchedule().test {
            assertThat(awaitItem().map { it.id }).containsExactly("1")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run it to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.ScheduleRepositoryTest"`
Expected: FAIL — `ScheduleRepositoryImpl` unresolved.

- [ ] **Step 3: Implement the repository**

```kotlin
package dev.crushingdigits.littlebird.data.schedule

import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
import dev.crushingdigits.littlebird.data.schedule.mapper.toDomain
import dev.crushingdigits.littlebird.data.schedule.mapper.toEntity
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    fun observeSchedule(): Flow<List<Game>>
    suspend fun refresh(): Result<Unit>
}

class ScheduleRepositoryImpl(
    private val remote: ScheduleDataSource,
    private val local: ScheduleLocalDataSource,
) : ScheduleRepository {

    override fun observeSchedule(): Flow<List<Game>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val entities = remote.fetchFixtures().mapNotNull { it.toEntity() }
        if (entities.isNotEmpty()) local.upsert(entities)
    }
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.schedule.ScheduleRepositoryTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/schedule/ScheduleRepository.kt app/src/test/java/dev/crushingdigits/littlebird/data/schedule/ScheduleRepositoryTest.kt
git commit -m "feat: add ScheduleRepository backed by Room"
```

### Task 4.2: Provide remote source + repository + config in DI

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DataBindingsModule.kt`

- [ ] **Step 1: Provide config + remote source in `NetworkModule`**

Add imports and providers to `NetworkModule`:

```kotlin
import dev.crushingdigits.littlebird.data.news.datasource.remote.FeedFetcher
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.TheSportsDbConfig
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.TheSportsDbScheduleDataSource
import kotlinx.serialization.json.Json
```

```kotlin
    @Provides
    @Singleton
    fun theSportsDbConfig(): TheSportsDbConfig = TheSportsDbConfig()

    @Provides
    @Singleton
    fun scheduleDataSource(fetcher: FeedFetcher, json: Json, config: TheSportsDbConfig): ScheduleDataSource =
        TheSportsDbScheduleDataSource(fetcher, json, config)
```

- [ ] **Step 2: Provide the repository in `DataBindingsModule`**

Add imports and provider to `DataBindingsModule`:

```kotlin
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepositoryImpl
import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
```

```kotlin
    @Provides
    @Singleton
    fun scheduleRepository(remote: ScheduleDataSource, local: ScheduleLocalDataSource): ScheduleRepository =
        ScheduleRepositoryImpl(remote, local)
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/NetworkModule.kt app/src/main/java/dev/crushingdigits/littlebird/di/DataBindingsModule.kt
git commit -m "feat: wire schedule data source and repository into DI"
```

---

## Phase 5 — Preferences (watched games + sync timestamps)

### Task 5.1: Extend `PreferencesDataSource` + `PreferencesRepository` (TDD)

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/preferences/datasource/PreferencesDataSource.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/data/preferences/PreferencesRepository.kt`
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/data/preferences/PreferencesRepositoryTest.kt`

- [ ] **Step 1: Add failing tests**

Append these tests inside `PreferencesRepositoryTest`:

```kotlin
    @Test fun `watched games default empty, mark and unmark`() = runTest {
        assertThat(subject.observeWatchedGames().first()).isEmpty()
        subject.markGameWatched("g1")
        subject.markGameWatched("g2")
        assertThat(subject.observeWatchedGames().first()).containsExactly("g1", "g2")
        subject.unmarkGameWatched("g1")
        assertThat(subject.observeWatchedGames().first()).containsExactly("g2")
    }

    @Test fun `setWatchedGames replaces the whole set`() = runTest {
        subject.markGameWatched("g1")
        subject.setWatchedGames(setOf("only"))
        assertThat(subject.observeWatchedGames().first()).containsExactly("only")
    }

    @Test fun `first and last sync timestamps are null by default and persist`() = runTest {
        assertThat(subject.getFirstScheduleSyncAt()).isNull()
        assertThat(subject.observeLastScheduleSyncAt().first()).isNull()
        subject.setFirstScheduleSyncAt(111L)
        subject.setLastScheduleSyncAt(222L)
        assertThat(subject.getFirstScheduleSyncAt()).isEqualTo(111L)
        assertThat(subject.observeLastScheduleSyncAt().first()).isEqualTo(222L)
    }
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.data.preferences.PreferencesRepositoryTest"`
Expected: FAIL — new methods unresolved.

- [ ] **Step 3: Add keys + flows to `PreferencesDataSource`**

Add inside `PreferencesDataSource` (after the existing keys/flows/setters):

```kotlin
import androidx.datastore.preferences.core.longPreferencesKey
```

```kotlin
    private val watchedGameIdsKey = stringSetPreferencesKey("watched_game_ids")
    private val firstScheduleSyncAtKey = longPreferencesKey("first_schedule_sync_at")
    private val lastScheduleSyncAtKey = longPreferencesKey("last_schedule_sync_at")

    val watchedGameIds: Flow<Set<String>> = dataStore.data.map { it[watchedGameIdsKey] ?: emptySet() }
    val firstScheduleSyncAt: Flow<Long?> = dataStore.data.map { it[firstScheduleSyncAtKey] }
    val lastScheduleSyncAt: Flow<Long?> = dataStore.data.map { it[lastScheduleSyncAtKey] }

    suspend fun setWatchedGameIds(ids: Set<String>) { dataStore.edit { it[watchedGameIdsKey] = ids } }
    suspend fun setFirstScheduleSyncAt(epochMs: Long) { dataStore.edit { it[firstScheduleSyncAtKey] = epochMs } }
    suspend fun setLastScheduleSyncAt(epochMs: Long) { dataStore.edit { it[lastScheduleSyncAtKey] = epochMs } }
```

- [ ] **Step 4: Add methods to `PreferencesRepository` interface + impl**

Add to the `PreferencesRepository` interface:

```kotlin
    fun observeWatchedGames(): Flow<Set<String>>
    suspend fun markGameWatched(id: String)
    suspend fun unmarkGameWatched(id: String)
    suspend fun setWatchedGames(ids: Set<String>)
    suspend fun getFirstScheduleSyncAt(): Long?
    suspend fun setFirstScheduleSyncAt(epochMs: Long)
    fun observeLastScheduleSyncAt(): Flow<Long?>
    suspend fun setLastScheduleSyncAt(epochMs: Long)
```

Add to `PreferencesRepositoryImpl`:

```kotlin
    override fun observeWatchedGames(): Flow<Set<String>> = source.watchedGameIds
    override suspend fun markGameWatched(id: String) = source.setWatchedGameIds(source.watchedGameIds.first() + id)
    override suspend fun unmarkGameWatched(id: String) = source.setWatchedGameIds(source.watchedGameIds.first() - id)
    override suspend fun setWatchedGames(ids: Set<String>) = source.setWatchedGameIds(ids)
    override suspend fun getFirstScheduleSyncAt(): Long? = source.firstScheduleSyncAt.first()
    override suspend fun setFirstScheduleSyncAt(epochMs: Long) = source.setFirstScheduleSyncAt(epochMs)
    override fun observeLastScheduleSyncAt(): Flow<Long?> = source.lastScheduleSyncAt
    override suspend fun setLastScheduleSyncAt(epochMs: Long) = source.setLastScheduleSyncAt(epochMs)
```

- [ ] **Step 5: Satisfy the new interface in existing test fakes**

> Kotlin compiles the entire `app/src/test` source set as one unit, so adding 8 methods to `PreferencesRepository` breaks every existing `FakePrefs` until they implement them. Add these 8 in-memory stubs to each `FakePrefs` in `domain/usecase/FilterPrefsUseCasesTest.kt`, `domain/usecase/MarkWatchedTest.kt`, and `ui/feed/FeedViewModelTest.kt` (these files already import `Flow`/`MutableStateFlow`). The `FeedViewModelTest` copy is overwritten in Task 8.4 and `MarkWatchedTest` is deleted in Task 11.2 — these stubs are interim; `FilterPrefsUseCasesTest`'s is permanent.

```kotlin
    private val watchedGames = MutableStateFlow<Set<String>>(emptySet())
    private val lastSync = MutableStateFlow<Long?>(null)
    private var firstSync: Long? = null
    override fun observeWatchedGames(): Flow<Set<String>> = watchedGames
    override suspend fun markGameWatched(id: String) { watchedGames.value = watchedGames.value + id }
    override suspend fun unmarkGameWatched(id: String) { watchedGames.value = watchedGames.value - id }
    override suspend fun setWatchedGames(ids: Set<String>) { watchedGames.value = ids }
    override suspend fun getFirstScheduleSyncAt(): Long? = firstSync
    override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
    override fun observeLastScheduleSyncAt(): Flow<Long?> = lastSync
    override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
```

- [ ] **Step 6: Run the FULL unit suite to confirm it compiles + passes**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all tests pass (incl. the 3 new `PreferencesRepositoryTest` cases).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/data/preferences/ app/src/test/java/dev/crushingdigits/littlebird
git commit -m "feat: persist watched games and schedule sync timestamps"
```

---

## Phase 6 — Pure domain use cases

### Task 6.1: `ComputeSpoilerWall` (TDD)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ComputeSpoilerWall.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/ComputeSpoilerWallTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Game
import org.junit.Test
import java.time.Instant

class ComputeSpoilerWallTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val subject = ComputeSpoilerWallUseCase()

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    @Test fun `wall is the earliest played unwatched kickoff`() {
        val games = listOf(
            game("a", "2026-06-01T16:00:00Z"), // played, unwatched -> wall
            game("b", "2026-06-03T16:00:00Z"), // played, unwatched
            game("c", "2026-06-10T16:00:00Z"), // upcoming
        )
        val wall = subject(games, watched = emptySet(), now = now)
        assertThat(wall).isEqualTo(Instant.parse("2026-06-01T16:00:00Z"))
    }

    @Test fun `watched games do not count and the wall advances`() {
        val games = listOf(game("a", "2026-06-01T16:00:00Z"), game("b", "2026-06-03T16:00:00Z"))
        val wall = subject(games, watched = setOf("a"), now = now)
        assertThat(wall).isEqualTo(Instant.parse("2026-06-03T16:00:00Z"))
    }

    @Test fun `upcoming games never form a wall`() {
        val games = listOf(game("c", "2026-06-10T16:00:00Z"))
        assertThat(subject(games, watched = emptySet(), now = now)).isNull()
    }

    @Test fun `all caught up gives a null wall`() {
        val games = listOf(game("a", "2026-06-01T16:00:00Z"))
        assertThat(subject(games, watched = setOf("a"), now = now)).isNull()
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallTest"`
Expected: FAIL — `ComputeSpoilerWallUseCase` unresolved.

- [ ] **Step 3: Implement**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.GameStatus
import java.time.Instant
import javax.inject.Inject

/** The spoiler wall: kickoff of the earliest PLAYED, unwatched game. Null when caught up. */
fun interface ComputeSpoilerWall {
    operator fun invoke(games: List<Game>, watched: Set<String>, now: Instant): Instant?
}

class ComputeSpoilerWallUseCase @Inject constructor() : ComputeSpoilerWall {
    override fun invoke(games: List<Game>, watched: Set<String>, now: Instant): Instant? =
        games.filter { it.status(now) == GameStatus.PLAYED && it.id !in watched }
            .minByOrNull { it.kickoff }
            ?.kickoff
}
```

- [ ] **Step 4: Run to confirm pass**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ComputeSpoilerWall.kt app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/ComputeSpoilerWallTest.kt
git commit -m "feat: add ComputeSpoilerWall use case"
```

### Task 6.2: `SeedWatchedOnFirstSync` (TDD)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SeedWatchedOnFirstSync.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/SeedWatchedOnFirstSyncTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.domain.model.Game
import org.junit.Test
import java.time.Instant

class SeedWatchedOnFirstSyncTest {
    private val firstSync = Instant.parse("2026-06-05T12:00:00Z")
    private val subject = SeedWatchedOnFirstSyncUseCase()

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    @Test fun `seeds games older than the window as watched, leaving recent and future unwatched`() {
        val games = listOf(
            game("old", "2026-05-20T16:00:00Z"),    // > 3 days before -> seeded
            game("recent", "2026-06-04T16:00:00Z"), // within 3 days -> NOT seeded
            game("future", "2026-06-10T16:00:00Z"), // future -> NOT seeded
        )
        val seeded = subject(games, firstSync, windowDays = 3)
        assertThat(seeded).containsExactly("old")
    }

    @Test fun `the window boundary is exclusive of the cutoff`() {
        // cutoff = firstSync - 3 days = 2026-06-02T12:00:00Z; a game exactly at cutoff is NOT seeded
        val games = listOf(game("boundary", "2026-06-02T12:00:00Z"))
        assertThat(subject(games, firstSync, windowDays = 3)).isEmpty()
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.Game
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/** Day-1 seed: games whose kickoff is strictly before (firstSyncAt - window) are pre-marked watched. */
fun interface SeedWatchedOnFirstSync {
    operator fun invoke(games: List<Game>, firstSyncAt: Instant, windowDays: Long = 3): Set<String>
}

class SeedWatchedOnFirstSyncUseCase @Inject constructor() : SeedWatchedOnFirstSync {
    override fun invoke(games: List<Game>, firstSyncAt: Instant, windowDays: Long): Set<String> {
        val cutoff = firstSyncAt.minus(Duration.ofDays(windowDays))
        return games.filter { it.kickoff.isBefore(cutoff) }.map { it.id }.toSet()
    }
}
```

- [ ] **Step 4: Run to confirm pass**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SeedWatchedOnFirstSync.kt app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/SeedWatchedOnFirstSyncTest.kt
git commit -m "feat: add SeedWatchedOnFirstSync use case"
```

---

## Phase 7 — Observe / mutate / orchestrate use cases + DI

### Task 7.1: Provide a `Clock`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DatabaseModule.kt`

> `Clock` goes in `DatabaseModule` (NOT `DataBindingsModule`) because the test `FakeDataModule` replaces `DataBindingsModule`; keeping `Clock` out of it means tests get the real clock without re-providing it.

- [ ] **Step 1: Add the provider**

Add to `DatabaseModule`:

```kotlin
import java.time.Clock
```

```kotlin
    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemUTC()
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/DatabaseModule.kt
git commit -m "feat: provide java.time.Clock"
```

### Task 7.2: Observe + mutate use cases (no separate tests — exercised via the ViewModel/SyncSchedule)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveSchedule.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveWatchedGames.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveLastSyncAt.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/MarkGameWatched.kt`
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/RestoreWatchedGames.kt`

- [ ] **Step 1: `ObserveSchedule`**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveSchedule { operator fun invoke(): Flow<List<Game>> }

class ObserveScheduleUseCase @Inject constructor(private val repo: ScheduleRepository) : ObserveSchedule {
    override fun invoke(): Flow<List<Game>> = repo.observeSchedule()
}
```

- [ ] **Step 2: `ObserveWatchedGames`**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveWatchedGames { operator fun invoke(): Flow<Set<String>> }

class ObserveWatchedGamesUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveWatchedGames {
    override fun invoke(): Flow<Set<String>> = prefs.observeWatchedGames()
}
```

- [ ] **Step 3: `ObserveLastSyncAt`**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveLastSyncAt { operator fun invoke(): Flow<Long?> }

class ObserveLastSyncAtUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveLastSyncAt {
    override fun invoke(): Flow<Long?> = prefs.observeLastScheduleSyncAt()
}
```

- [ ] **Step 4: `MarkGameWatched` (bidirectional, returns prior snapshot for Undo)**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Marks ([watched]=true) or un-marks a game; returns the prior watched set so the caller can Undo. */
fun interface MarkGameWatched { suspend operator fun invoke(id: String, watched: Boolean): Set<String> }

class MarkGameWatchedUseCase @Inject constructor(private val prefs: PreferencesRepository) : MarkGameWatched {
    override suspend fun invoke(id: String, watched: Boolean): Set<String> {
        val snapshot = prefs.observeWatchedGames().first()
        if (watched) prefs.markGameWatched(id) else prefs.unmarkGameWatched(id)
        return snapshot
    }
}
```

- [ ] **Step 5: `RestoreWatchedGames`**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

/** Restores the watched-game set to a prior snapshot — used to Undo a mark/un-mark. */
fun interface RestoreWatchedGames { suspend operator fun invoke(ids: Set<String>) }

class RestoreWatchedGamesUseCase @Inject constructor(private val prefs: PreferencesRepository) : RestoreWatchedGames {
    override suspend fun invoke(ids: Set<String>) = prefs.setWatchedGames(ids)
}
```

- [ ] **Step 6: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveSchedule.kt app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveWatchedGames.kt app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/ObserveLastSyncAt.kt app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/MarkGameWatched.kt app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/RestoreWatchedGames.kt
git commit -m "feat: add schedule observe/mutate use cases"
```

### Task 7.3: `SyncSchedule` orchestration (TDD)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SyncSchedule.kt`
- Test: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/SyncScheduleTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class SyncScheduleTest {
    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private fun game(id: String, ko: String) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true,
        competition = "PL", kickoff = Instant.parse(ko), homeScore = null, awayScore = null,
    )

    private class FakeRepo(private val games: List<Game>, private val ok: Boolean) : ScheduleRepository {
        val schedule = MutableStateFlow(emptyList<Game>())
        var refreshed = false
        override fun observeSchedule(): Flow<List<Game>> = schedule
        override suspend fun refresh(): Result<Unit> {
            refreshed = true
            return if (ok) { schedule.value = games; Result.success(Unit) } else Result.failure(RuntimeException())
        }
    }

    // Minimal in-memory PreferencesRepository (only the methods SyncSchedule touches).
    private class FakePrefs : dev.crushingdigits.littlebird.data.preferences.PreferencesRepository {
        var watched: Set<String> = emptySet()
        var firstSync: Long? = null
        var lastSync: Long? = null
        override fun observeProtection() = MutableStateFlow(true)
        override fun observeRevealed() = MutableStateFlow<Set<String>>(emptySet())
        override suspend fun setProtection(on: Boolean) {}
        override suspend fun toggleProtection() {}
        override suspend fun reveal(id: String) {}
        override suspend fun revealAll(ids: Set<String>) {}
        override suspend fun restoreRevealed(ids: Set<String>) {}
        override fun observeReliabilityLevel() = MutableStateFlow(3)
        override fun observePinnedFeed() = MutableStateFlow<String?>(null)
        override suspend fun setReliabilityLevel(level: Int) {}
        override suspend fun setPinnedFeed(key: String?) {}
        override fun observeWatchedGames() = MutableStateFlow(watched)
        override suspend fun markGameWatched(id: String) { watched = watched + id }
        override suspend fun unmarkGameWatched(id: String) { watched = watched - id }
        override suspend fun setWatchedGames(ids: Set<String>) { watched = ids }
        override suspend fun getFirstScheduleSyncAt() = firstSync
        override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
        override fun observeLastScheduleSyncAt() = MutableStateFlow(lastSync)
        override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync = epochMs }
    }

    private fun subject(repo: ScheduleRepository, prefs: FakePrefs) =
        SyncScheduleUseCase(repo, prefs, SeedWatchedOnFirstSyncUseCase(), clock)

    @Test fun `first successful sync seeds old games as watched and stamps timestamps`() = runTest {
        val games = listOf(game("old", "2026-05-20T16:00:00Z"), game("recent", "2026-06-04T16:00:00Z"))
        val prefs = FakePrefs()
        val result = subject(FakeRepo(games, ok = true), prefs).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(prefs.watched).containsExactly("old")
        assertThat(prefs.firstSync).isEqualTo(now.toEpochMilli())
        assertThat(prefs.lastSync).isEqualTo(now.toEpochMilli())
    }

    @Test fun `a later sync does not re-seed but updates lastSync`() = runTest {
        val prefs = FakePrefs().apply { firstSync = 999L; watched = setOf("manual") }
        val result = subject(FakeRepo(listOf(game("old", "2026-05-20T16:00:00Z")), ok = true), prefs).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(prefs.watched).containsExactly("manual") // unchanged
        assertThat(prefs.firstSync).isEqualTo(999L)         // unchanged
        assertThat(prefs.lastSync).isEqualTo(now.toEpochMilli())
    }

    @Test fun `a failed sync seeds nothing and does not stamp timestamps`() = runTest {
        val prefs = FakePrefs()
        val result = subject(FakeRepo(emptyList(), ok = false), prefs).invoke()

        assertThat(result.isFailure).isTrue()
        assertThat(prefs.watched).isEmpty()
        assertThat(prefs.firstSync).isNull()
        assertThat(prefs.lastSync).isNull()
    }
}
```

- [ ] **Step 2: Run to confirm failure**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.SyncScheduleTest"`
Expected: FAIL — `SyncScheduleUseCase` unresolved.

- [ ] **Step 3: Implement**

```kotlin
package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Refreshes fixtures, then (only on the very first successful sync) seeds the day-1 watched set and
 * stamps the first-sync time. Always stamps the last-sync time on success. Returns the refresh result
 * so the caller can surface offline state.
 */
fun interface SyncSchedule { suspend operator fun invoke(): Result<Unit> }

class SyncScheduleUseCase @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val prefs: PreferencesRepository,
    private val seedWatchedOnFirstSync: SeedWatchedOnFirstSync,
    private val clock: Clock,
) : SyncSchedule {
    override suspend fun invoke(): Result<Unit> {
        val result = scheduleRepository.refresh()
        if (result.isFailure) return result

        val now = clock.instant()
        if (prefs.getFirstScheduleSyncAt() == null) {
            val games = scheduleRepository.observeSchedule().first()
            prefs.setWatchedGames(seedWatchedOnFirstSync(games, now))
            prefs.setFirstScheduleSyncAt(now.toEpochMilli())
        }
        prefs.setLastScheduleSyncAt(now.toEpochMilli())
        return result
    }
}
```

- [ ] **Step 4: Run to confirm pass**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.domain.usecase.SyncScheduleTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/SyncSchedule.kt app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/SyncScheduleTest.kt
git commit -m "feat: add SyncSchedule orchestration with first-run seeding"
```

### Task 7.4: Bind the new use cases in `DomainModule`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt`

- [ ] **Step 1: Add binds + imports**

Add these imports:

```kotlin
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWall
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallUseCase
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatched
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAt
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAtUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveSchedule
import dev.crushingdigits.littlebird.domain.usecase.ObserveScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSync
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncUseCase
import dev.crushingdigits.littlebird.domain.usecase.SyncSchedule
import dev.crushingdigits.littlebird.domain.usecase.SyncScheduleUseCase
```

Add these `@Binds` inside the `DomainModule` class:

```kotlin
    @Binds abstract fun observeSchedule(impl: ObserveScheduleUseCase): ObserveSchedule
    @Binds abstract fun observeWatchedGames(impl: ObserveWatchedGamesUseCase): ObserveWatchedGames
    @Binds abstract fun observeLastSyncAt(impl: ObserveLastSyncAtUseCase): ObserveLastSyncAt
    @Binds abstract fun markGameWatched(impl: MarkGameWatchedUseCase): MarkGameWatched
    @Binds abstract fun restoreWatchedGames(impl: RestoreWatchedGamesUseCase): RestoreWatchedGames
    @Binds abstract fun computeSpoilerWall(impl: ComputeSpoilerWallUseCase): ComputeSpoilerWall
    @Binds abstract fun seedWatchedOnFirstSync(impl: SeedWatchedOnFirstSyncUseCase): SeedWatchedOnFirstSync
    @Binds abstract fun syncSchedule(impl: SyncScheduleUseCase): SyncSchedule
```

- [ ] **Step 2: Verify it compiles (Hilt graph resolves)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt
git commit -m "feat: bind schedule use cases in DomainModule"
```

---

## Phase 8 — ViewModel: wall-based redaction

### Task 8.1: `FixtureUi` UI model (centralized score gate)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FixtureUi.kt`

- [ ] **Step 1: Write the UI model + mapper**

```kotlin
package dev.crushingdigits.littlebird.ui.feed

import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.GameStatus
import dev.crushingdigits.littlebird.domain.model.MatchResult
import java.time.Instant

enum class FixtureState { WATCHED, UNWATCHED, UPCOMING }

/** A fixture as the sheet renders it. THE HARD RULE: [scoreLine]/[result] are non-null ONLY when WATCHED. */
data class FixtureUi(
    val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoff: Instant,
    val state: FixtureState,
    val scoreLine: String?,
    val result: MatchResult?,
    val isWall: Boolean,
)

/** Builds sheet rows. Score/result are only ever attached to WATCHED games — the single gate. */
fun List<Game>.toFixtureUi(watched: Set<String>, now: Instant, wall: Instant?): List<FixtureUi> = map { g ->
    val state = when {
        g.status(now) == GameStatus.UPCOMING -> FixtureState.UPCOMING
        g.id in watched -> FixtureState.WATCHED
        else -> FixtureState.UNWATCHED
    }
    val isWatched = state == FixtureState.WATCHED
    FixtureUi(
        id = g.id,
        opponent = g.opponent,
        abbreviation = g.abbreviation,
        isHome = g.isHome,
        competition = g.competition,
        kickoff = g.kickoff,
        state = state,
        scoreLine = if (isWatched) g.scoreLine() else null,
        result = if (isWatched) g.result() else null,
        isWall = state == FixtureState.UNWATCHED && wall != null && g.kickoff == wall,
    )
}

private fun Game.scoreLine(): String? {
    val h = homeScore ?: return null
    val a = awayScore ?: return null
    return "$h–$a" // en dash; home–away
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FixtureUi.kt
git commit -m "feat: add FixtureUi model with centralized score gate"
```

### Task 8.2: Update `FeedUiState`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedUiState.kt`

- [ ] **Step 1: Replace `FeedUiState` (keep `FeedTab` as-is)**

Replace the `FeedUiState` data class with:

```kotlin
import java.time.Instant
```

```kotlin
data class FeedUiState(
    val cards: List<FeedCard> = emptyList(),
    val protectionOn: Boolean = true,
    val hidingCount: Int = 0,
    val isRefreshing: Boolean = false,
    val isOffline: Boolean = false,
    val isEmpty: Boolean = false,
    val gateTarget: NewsItem? = null,
    val wallSheetVisible: Boolean = false,
    val fixtures: List<FixtureUi> = emptyList(),
    val lastSyncAt: Instant? = null,
    // Filter chrome
    val filterTabs: List<FeedTab> = emptyList(),
    val selectedType: FeedType = FeedType.ALL,
    val reliabilityLevel: Tier = Tier.TIER_3_SPECULATIVE,
    val pinnedType: FeedType? = null,
    val isFilteredEmpty: Boolean = false,
)
```

> This removes `heldMatchCount` and `catchUpVisible` (now `hidingCount`/`wallSheetVisible`). The screen (Task 9.4) and tests (Task 8.4) are updated to match.

- [ ] **Step 2: (compile deferred — `FeedViewModel`/`FeedScreen` update next; do not run a build yet.)**

### Task 8.3: Rewrite `FeedViewModel`

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt`

- [ ] **Step 1: Replace the whole file**

```kotlin
package dev.crushingdigits.littlebird.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.crushingdigits.littlebird.domain.model.FeedFilter
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.GameStatus
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWall
import dev.crushingdigits.littlebird.domain.usecase.FilterFeed
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatched
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeed
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefs
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAt
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtection
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealed
import dev.crushingdigits.littlebird.domain.usecase.ObserveSchedule
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeed
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGames
import dev.crushingdigits.littlebird.domain.usecase.RevealItem
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeed
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevel
import dev.crushingdigits.littlebird.domain.usecase.SyncSchedule
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
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val observeFeed: ObserveFeed,
    private val observeProtection: ObserveProtection,
    private val observeRevealed: ObserveRevealed,
    private val refreshFeed: RefreshFeed,
    private val toggleProtection: ToggleProtection,
    private val revealItem: RevealItem,
    private val filterFeed: FilterFeed,
    private val observeFilterPrefs: ObserveFilterPrefs,
    private val setReliabilityLevel: SetReliabilityLevel,
    private val setPinnedFeed: SetPinnedFeed,
    private val observeSchedule: ObserveSchedule,
    private val observeWatchedGames: ObserveWatchedGames,
    private val observeLastSyncAt: ObserveLastSyncAt,
    private val markGameWatched: MarkGameWatched,
    private val restoreWatchedGames: RestoreWatchedGames,
    private val syncSchedule: SyncSchedule,
    private val computeSpoilerWall: ComputeSpoilerWall,
    private val clock: Clock,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val offline = MutableStateFlow(false)
    private val gateTarget = MutableStateFlow<NewsItem?>(null)
    private val wallSheetVisible = MutableStateFlow(false)

    // Session-only selected feed; re-seeded from the pinned feed on cold launch.
    private val selectedType = MutableStateFlow(FeedType.ALL)

    private val _actions = MutableSharedFlow<FeedAction>(extraBufferCapacity = 1)
    val actions: SharedFlow<FeedAction> = _actions.asSharedFlow()

    init {
        viewModelScope.launch {
            selectedType.value = observeFilterPrefs().first().pinned ?: FeedType.ALL
        }
    }

    private data class Transient(val refreshing: Boolean, val offline: Boolean, val gate: NewsItem?, val sheet: Boolean)
    private val transient = combine(refreshing, offline, gateTarget, wallSheetVisible, ::Transient)

    private data class FilterState(val type: FeedType, val maxTier: Tier, val pinned: FeedType?)
    private val filterState = combine(selectedType, observeFilterPrefs()) { type, prefs ->
        FilterState(type, prefs.maxTier, prefs.pinned)
    }

    private data class Spoiler(
        val wall: Instant?,
        val revealed: Set<String>,
        val hidingCount: Int,
        val fixtures: List<FixtureUi>,
        val lastSyncAt: Instant?,
    )

    private val spoiler = combine(
        observeSchedule(), observeWatchedGames(), observeRevealed(), observeLastSyncAt(),
    ) { games, watched, revealed, lastSyncMs ->
        val now = clock.instant()
        val wall = computeSpoilerWall(games, watched, now)
        val hidingCount = games.count { it.status(now) == GameStatus.PLAYED && it.id !in watched }
        Spoiler(
            wall = wall,
            revealed = revealed,
            hidingCount = hidingCount,
            fixtures = games.toFixtureUi(watched, now, wall),
            lastSyncAt = lastSyncMs?.let(Instant::ofEpochMilli),
        )
    }

    val uiState: StateFlow<FeedUiState> = combine(
        observeFeed(), observeProtection(), filterState, transient, spoiler,
    ) { items, protectionOn, fs, t, sp ->
        val filtered = filterFeed(items, FeedFilter(fs.type, fs.maxTier))
        val cards = filtered.visible.map { item ->
            if (isHidden(item, protectionOn, sp.wall, sp.revealed)) FeedCard.Redacted(item) else FeedCard.Normal(item)
        }
        FeedUiState(
            cards = cards,
            protectionOn = protectionOn,
            hidingCount = sp.hidingCount,
            isRefreshing = t.refreshing,
            isOffline = t.offline,
            isEmpty = items.isEmpty() && !t.refreshing,
            gateTarget = t.gate,
            wallSheetVisible = t.sheet,
            fixtures = sp.fixtures,
            lastSyncAt = sp.lastSyncAt,
            filterTabs = FeedType.entries.map { FeedTab(it, filtered.counts[it] ?: 0) },
            selectedType = fs.type,
            reliabilityLevel = fs.maxTier,
            pinnedType = fs.pinned,
            isFilteredEmpty = items.isNotEmpty() && filtered.visible.isEmpty() && !t.refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FeedUiState())

    private fun isHidden(item: NewsItem, protectionOn: Boolean, wall: Instant?, revealed: Set<String>): Boolean =
        protectionOn && wall != null && !item.publishedAt.isBefore(wall) && item.id !in revealed

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

    fun onOpenWallSheet() { wallSheetVisible.value = true }
    fun onDismissWallSheet() { wallSheetVisible.value = false }

    fun onMarkGameWatched(gameId: String) = viewModelScope.launch {
        val before = currentWall()
        val snapshot = markGameWatched(gameId, watched = true)
        val after = currentWall()
        val unlocked = countUnlocked(before, after)
        _actions.emit(
            FeedAction.ShowToast(
                message = "$unlocked ${if (unlocked == 1) "story" else "stories"} unlocked",
                undoSnapshot = snapshot,
            ),
        )
    }

    fun onUnmarkGame(gameId: String) = viewModelScope.launch { markGameWatched(gameId, watched = false) }

    fun onUndo(snapshot: Set<String>) = viewModelScope.launch { restoreWatchedGames(snapshot) }

    private suspend fun currentWall(): Instant? =
        computeSpoilerWall(observeSchedule().first(), observeWatchedGames().first(), clock.instant())

    /** Feed items released by advancing the wall from [before] up to (but excluding) [after]. */
    private suspend fun countUnlocked(before: Instant?, after: Instant?): Int {
        if (before == null) return 0
        val revealed = observeRevealed().first()
        return observeFeed().first().count { item ->
            !item.publishedAt.isBefore(before) &&
                (after == null || item.publishedAt.isBefore(after)) &&
                item.id !in revealed
        }
    }

    fun onRefresh() = viewModelScope.launch {
        refreshing.value = true
        val feed = refreshFeed()
        syncSchedule()
        offline.value = feed.isFailure
        refreshing.value = false
    }

    fun onRefreshSchedule() = viewModelScope.launch {
        offline.value = syncSchedule().isFailure
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

> The retained per-item peek uses `RevealItem` + `ObserveRevealed`; the only "restore" path is now `restoreWatchedGames` (used by `onUndo`). `MarkWatched`/`RestoreRevealed` are no longer referenced here and are deleted in Phase 11.

- [ ] **Step 2: (compile after Task 9.4 — `FeedScreen` still references removed symbols until then.)**

### Task 8.4: Rewrite `FeedViewModelTest`

**Files:**
- Modify: `app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt`

- [ ] **Step 1: Replace the whole test file**

```kotlin
package dev.crushingdigits.littlebird.ui.feed

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.MainDispatcherRule
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ComputeSpoilerWallUseCase
import dev.crushingdigits.littlebird.domain.usecase.FilterFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.MarkGameWatchedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveFilterPrefsUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveLastSyncAtUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveProtectionUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveRevealedUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ObserveWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RefreshFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.RestoreWatchedGamesUseCase
import dev.crushingdigits.littlebird.domain.usecase.RevealItemUseCase
import dev.crushingdigits.littlebird.domain.usecase.SeedWatchedOnFirstSyncUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetPinnedFeedUseCase
import dev.crushingdigits.littlebird.domain.usecase.SetReliabilityLevelUseCase
import dev.crushingdigits.littlebird.domain.usecase.SyncScheduleUseCase
import dev.crushingdigits.littlebird.domain.usecase.ToggleProtectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FeedViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private val now = Instant.parse("2026-06-05T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val beforeWall = Instant.parse("2026-05-30T00:00:00Z") // before any game -> visible
    private val afterWall = Instant.parse("2026-06-02T00:00:00Z")  // after the wall game -> hidden

    private fun item(id: String, at: Instant = afterWall, category: Category = Category.TRANSFER, tier: Tier = Tier.TIER_1_RELIABLE) = NewsItem(
        id = id, source = "BBC", tier = tier, category = category, isMatch = category == Category.MATCH,
        confirmed = false, author = null, role = null, title = "t$id", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = at,
    )

    private fun game(id: String, ko: String, watchedScore: Pair<Int, Int>? = null) = Game(
        id = id, opponent = "Opp$id", abbreviation = "OPP", isHome = true, competition = "PL",
        kickoff = Instant.parse(ko), homeScore = watchedScore?.first, awayScore = watchedScore?.second,
    )

    private val wallGame = game("g1", "2026-06-01T16:00:00Z", watchedScore = 2 to 1) // played, unwatched -> wall

    private class FakeNews(items: List<NewsItem>) : NewsRepository {
        val flow = MutableStateFlow(items)
        override fun observeFeed(): Flow<List<NewsItem>> = flow
        override fun observeItem(id: String): Flow<NewsItem?> = flow.map { l -> l.firstOrNull { it.id == id } }
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun isEmpty(): Boolean = flow.value.isEmpty()
    }

    private class FakeSchedule(games: List<Game>) : ScheduleRepository {
        val flow = MutableStateFlow(games)
        override fun observeSchedule(): Flow<List<Game>> = flow
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    }

    private class FakePrefs(on: Boolean, pinned: String? = null, level: Int = 3, watchedGames: Set<String> = emptySet()) : PreferencesRepository {
        val protection = MutableStateFlow(on)
        val revealed = MutableStateFlow<Set<String>>(emptySet())
        val reliabilityLevel = MutableStateFlow(level)
        val pinnedFeed = MutableStateFlow(pinned)
        val watched = MutableStateFlow(watchedGames)
        val lastSync = MutableStateFlow<Long?>(null)
        var firstSync: Long? = null
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
        override fun observeWatchedGames() = watched
        override suspend fun markGameWatched(id: String) { watched.value = watched.value + id }
        override suspend fun unmarkGameWatched(id: String) { watched.value = watched.value - id }
        override suspend fun setWatchedGames(ids: Set<String>) { watched.value = ids }
        override suspend fun getFirstScheduleSyncAt() = firstSync
        override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
        override fun observeLastScheduleSyncAt() = lastSync
        override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
    }

    private fun vm(news: FakeNews, prefs: FakePrefs, schedule: FakeSchedule) = FeedViewModel(
        observeFeed = ObserveFeedUseCase(news),
        observeProtection = ObserveProtectionUseCase(prefs),
        observeRevealed = ObserveRevealedUseCase(prefs),
        refreshFeed = RefreshFeedUseCase(news),
        toggleProtection = ToggleProtectionUseCase(prefs),
        revealItem = RevealItemUseCase(prefs),
        filterFeed = FilterFeedUseCase(),
        observeFilterPrefs = ObserveFilterPrefsUseCase(prefs),
        setReliabilityLevel = SetReliabilityLevelUseCase(prefs),
        setPinnedFeed = SetPinnedFeedUseCase(prefs),
        observeSchedule = ObserveScheduleUseCase(schedule),
        observeWatchedGames = ObserveWatchedGamesUseCase(prefs),
        observeLastSyncAt = ObserveLastSyncAtUseCase(prefs),
        markGameWatched = MarkGameWatchedUseCase(prefs),
        restoreWatchedGames = RestoreWatchedGamesUseCase(prefs),
        syncSchedule = SyncScheduleUseCase(schedule, prefs, SeedWatchedOnFirstSyncUseCase(), clock),
        computeSpoilerWall = ComputeSpoilerWallUseCase(),
        clock = clock,
    )

    @Test fun `protection off renders everything Normal`() = runTest {
        val sut = vm(FakeNews(listOf(item("a", afterWall), item("b", afterWall))), FakePrefs(on = false), FakeSchedule(listOf(wallGame)))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.all { it is FeedCard.Normal }).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `items after the wall are redacted, items before are normal`() = runTest {
        val sut = vm(
            FakeNews(listOf(item("after", afterWall), item("before", beforeWall))),
            FakePrefs(on = true),
            FakeSchedule(listOf(wallGame)),
        )
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.first { it.item.id == "after" }).isInstanceOf(FeedCard.Redacted::class.java)
            assertThat(s.cards.first { it.item.id == "before" }).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.hidingCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `no wall means nothing is hidden`() = runTest {
        // The only game is upcoming -> no wall -> all visible even with protection on.
        val sut = vm(FakeNews(listOf(item("x", afterWall))), FakePrefs(on = true), FakeSchedule(listOf(game("u", "2026-06-20T16:00:00Z"))))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.single()).isInstanceOf(FeedCard.Normal::class.java)
            assertThat(s.hidingCount).isEqualTo(0)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `marking the wall game watched reveals its stories and emits an undo toast`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.actions.test {
            sut.onMarkGameWatched("g1")
            val action = awaitItem()
            assertThat(action).isInstanceOf(FeedAction.ShowToast::class.java)
            assertThat((action as FeedAction.ShowToast).undoSnapshot).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.watched.value).containsExactly("g1")
    }

    @Test fun `undo restores the prior watched set`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.onMarkGameWatched("g1")
        sut.onUndo(emptySet())
        assertThat(prefs.watched.value).isEmpty()
    }

    @Test fun `fixtures expose a score only for watched games`() = runTest {
        val prefs = FakePrefs(on = true, watchedGames = setOf("g1"))
        val sut = vm(FakeNews(emptyList()), prefs, FakeSchedule(listOf(wallGame, game("g2", "2026-06-03T16:00:00Z", 0 to 0))))
        sut.uiState.test {
            val s = awaitItem()
            val watched = s.fixtures.first { it.id == "g1" }
            val unwatched = s.fixtures.first { it.id == "g2" }
            assertThat(watched.scoreLine).isEqualTo("2–1")
            assertThat(unwatched.scoreLine).isNull()
            assertThat(unwatched.isWall).isTrue() // g2 is now the earliest unwatched played game
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `revealing a hidden item from the gate turns it Normal and navigates`() = runTest {
        val prefs = FakePrefs(on = true)
        val sut = vm(FakeNews(listOf(item("after", afterWall))), prefs, FakeSchedule(listOf(wallGame)))
        sut.actions.test {
            sut.onRevealFromGate("after")
            assertThat(awaitItem()).isEqualTo(FeedAction.NavigateToDetail("after"))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(prefs.revealed.value).contains("after")
    }

    @Test fun `clicking a redacted card opens the gate`() = runTest {
        val sut = vm(FakeNews(listOf(item("after", afterWall))), FakePrefs(on = true), FakeSchedule(listOf(wallGame)))
        sut.onCardClick(FeedCard.Redacted(item("after", afterWall)))
        sut.uiState.test {
            assertThat(awaitItem().gateTarget?.id).isEqualTo("after")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `selecting a type filters the visible cards`() = runTest {
        val news = FakeNews(listOf(item("t", beforeWall, Category.TRANSFER), item("m", beforeWall, Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false), FakeSchedule(listOf(wallGame)))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.cards.map { it.item.id }).containsExactly("t")
            assertThat(s.selectedType).isEqualTo(FeedType.TRANSFERS)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `toggling pin pins then unpins the current feed`() = runTest {
        val prefs = FakePrefs(on = false)
        val sut = vm(FakeNews(listOf(item("t", beforeWall, Category.TRANSFER))), prefs, FakeSchedule(emptyList()))
        sut.onSelectType(FeedType.TRANSFERS)
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isEqualTo("transfer")
        sut.onTogglePin()
        assertThat(prefs.pinnedFeed.value).isNull()
    }

    @Test fun `cold launch seeds the selected type from the pinned feed`() = runTest {
        val news = FakeNews(listOf(item("t", beforeWall, Category.TRANSFER), item("m", beforeWall, Category.MATCH)))
        val sut = vm(news, FakePrefs(on = false, pinned = "match"), FakeSchedule(emptyList()))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.selectedType).isEqualTo(FeedType.MATCH)
            assertThat(s.cards.map { it.item.id }).containsExactly("m")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: (run after Task 9.4 — `FeedScreen` must compile first.)**

> The ViewModel + its test won't run green until `FeedScreen` (Task 9.4) and component deletions (Phase 11) compile. Phase 9 follows immediately.

---

## Phase 9 — UI components

### Task 9.1: `competitionTag` helper

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/CompetitionTag.kt`

- [ ] **Step 1: Write it**

```kotlin
package dev.crushingdigits.littlebird.ui.feed.wall

/** Maps a full competition/league name to a short tag for the fixtures sheet. */
fun competitionTag(competition: String): String = when {
    competition.contains("Premier League", ignoreCase = true) -> "PL"
    competition.contains("Champions League", ignoreCase = true) -> "UCL"
    competition.contains("Europa League", ignoreCase = true) -> "UEL"
    competition.contains("FA Cup", ignoreCase = true) -> "FA"
    competition.contains("Carabao", ignoreCase = true) ||
        competition.contains("EFL Cup", ignoreCase = true) ||
        competition.contains("League Cup", ignoreCase = true) -> "EFL"
    competition.isBlank() -> "—"
    else -> competition.split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").take(3)
}
```

- [ ] **Step 2: Verify it compiles** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/CompetitionTag.kt
git commit -m "feat: add competitionTag helper"
```

### Task 9.2: `FixtureRow` (crest + meta + state-dependent trailing)

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/FixtureRow.kt`

- [ ] **Step 1: Write it**

```kotlin
package dev.crushingdigits.littlebird.ui.feed.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.domain.model.MatchResult
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.theme.Tier3
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun FixtureRow(
    fixture: FixtureUi,
    onMarkWatched: (String) -> Unit,
    onUnmark: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CrestTile(fixture)
        Column(Modifier.weight(1f)) {
            Text(fixture.opponent, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(competitionTag(fixture.competition))
                    append(" · ")
                    append(if (fixture.isHome) "Home" else "Away")
                    append(" · ")
                    append(dayFmt.format(fixture.kickoff))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (fixture.state) {
            FixtureState.WATCHED -> WatchedTrailing(fixture, onUnmark)
            FixtureState.UNWATCHED -> FilledTonalButton(onClick = { onMarkWatched(fixture.id) }) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Mark watched")
            }
            FixtureState.UPCOMING -> Column(horizontalAlignment = Alignment.End) {
                Text(timeFmt.format(fixture.kickoff), style = MaterialTheme.typography.titleMedium)
                Text("UPCOMING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WatchedTrailing(fixture: FixtureUi, onUnmark: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fixture.result?.let { r ->
            val color = when (r) {
                MatchResult.WIN -> Confirmed
                MatchResult.DRAW -> MaterialTheme.colorScheme.onSurfaceVariant
                MatchResult.LOSS -> Tier3
            }
            Text(r.name.take(1), style = MaterialTheme.typography.labelSmall, color = color)
        }
        Text(
            fixture.scoreLine.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
        IconButton(onClick = { onUnmark(fixture.id) }) {
            Icon(Icons.Default.Undo, contentDescription = "Un-mark watched")
        }
    }
}

@Composable
private fun CrestTile(fixture: FixtureUi) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            fixture.abbreviation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

> Crest is the abbreviation-tile fallback from the handoff; swapping in TheSportsDB badge images keyed by fixture id is a later enhancement (out of scope here).

- [ ] **Step 2: Verify it compiles** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/FixtureRow.kt
git commit -m "feat: add FixtureRow with score gated to watched games"
```

### Task 9.3: `MatchWallSheet`

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/MatchWallSheet.kt`

- [ ] **Step 1: Write it**

```kotlin
package dev.crushingdigits.littlebird.ui.feed.wall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val updatedFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchWallSheet(
    fixtures: List<FixtureUi>,
    hidingCount: Int,
    lastSyncAtEpochMs: Long?,
    isOffline: Boolean,
    onMarkWatched: (String) -> Unit,
    onUnmark: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showUpcoming by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            Text("Fixtures & spoilers", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                if (hidingCount > 0) {
                    "$hidingCount unwatched ${if (hidingCount > 1) "games are" else "game is"} hiding newer stories. " +
                        "Mark ${if (hidingCount > 1) "them" else "it"} watched to catch up."
                } else {
                    "All caught up — nothing's hidden. Every result below is yours to see."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    (if (isOffline) "No connection · " else "Updated ") +
                        (lastSyncAtEpochMs?.let { updatedFmt.format(java.time.Instant.ofEpochMilli(it)) } ?: "—"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isOffline) "Try again" else "Refresh")
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showUpcoming, onClick = { showUpcoming = false }, label = { Text("Results") })
                FilterChip(selected = showUpcoming, onClick = { showUpcoming = true }, label = { Text("Upcoming") })
            }
            Spacer(Modifier.height(8.dp))

            val results = fixtures.filter { it.state != FixtureState.UPCOMING }.sortedByDescending { it.kickoff }
            val upcoming = fixtures.filter { it.state == FixtureState.UPCOMING }.sortedBy { it.kickoff }
            val shown = if (showUpcoming) upcoming else results

            shown.forEachIndexed { index, fixture ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                FixtureRow(fixture = fixture, onMarkWatched = onMarkWatched, onUnmark = onUnmark)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "A score is only ever shown once its game is watched",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScopeHeightSpacerPlaceholder() = Unit
```

> Replace the `androidx.compose.foundation.layout.Spacer(Modifier.height(...))` calls with the imported `Spacer` + `Modifier.height` (add `import androidx.compose.foundation.layout.height`). Remove the unused `ColumnScopeHeightSpacerPlaceholder`. (Kept minimal here; the scrolling/`LazyColumn` polish and exact spacing come from the handoff.)

- [ ] **Step 2: Add the missing `height` import and delete the placeholder fun, then verify**

Add `import androidx.compose.foundation.layout.height`, delete `ColumnScopeHeightSpacerPlaceholder`.

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/feed/wall/MatchWallSheet.kt
git commit -m "feat: add MatchWallSheet fixtures sheet"
```

### Task 9.4: `ProtectionCard` + `GateSheet` copy + `FeedScreen` wiring

**Files:**
- Create: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/ProtectionCard.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/GateSheet.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt`

- [ ] **Step 1: Write `ProtectionCard`**

```kotlin
package dev.crushingdigits.littlebird.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.theme.Primary

/**
 * Feed entry point. Replaces ProtectionBanner + CatchUpCard.
 * - protection OFF: plain banner + toggle.
 * - protection ON, games hiding: toggle + a "Manage" action row.
 * - protection ON, caught up: toggle + a "Fixtures" action row.
 */
@Composable
fun ProtectionCard(
    on: Boolean,
    hidingCount: Int,
    onToggle: () -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (on) Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, if (on) Primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = if (on) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Spoiler protection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !on -> "Off · showing everything, including match content"
                            hidingCount > 0 -> "On · hiding news from games you haven't watched"
                            else -> "On · nothing to hide right now"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = on, onCheckedChange = { onToggle() })
            }
            if (on) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = onOpenSheet, modifier = Modifier.fillMaxWidth()) {
                    val hiding = hidingCount > 0
                    Icon(
                        if (hiding) Icons.Default.Lock else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (hiding) Primary else Confirmed,
                    )
                    Text(
                        if (hiding) {
                            "  $hidingCount unwatched ${if (hidingCount > 1) "games are" else "game is"} hiding newer stories"
                        } else {
                            "  You're all caught up — every game watched"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (hiding) "Manage" else "Fixtures", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update `GateSheet` copy to be date-based**

In `GateSheet.kt` replace the title/body/chip text:

```kotlin
            Text("This one might spoil you", style = MaterialTheme.typography.titleLarge)
            Text(
                "From $source. It was published after a game you haven't marked watched — it may reveal a result.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("PUBLISHED AFTER AN UNWATCHED GAME", color = Tier3) },
            )
```

- [ ] **Step 3: Rewire `FeedScreen`**

Replace the imports of `CatchUpCard`, `CatchUpSheet`, `ProtectionBanner` with `ProtectionCard` + `MatchWallSheet`:

```kotlin
import dev.crushingdigits.littlebird.ui.components.ProtectionCard
import dev.crushingdigits.littlebird.ui.feed.wall.MatchWallSheet
```

Remove these imports: `CatchUpCard`, `CatchUpSheet`, `ProtectionBanner`.

In the `LazyColumn`, replace the first `item { ProtectionBanner(...) }` and the `if (state.protectionOn && state.heldMatchCount > 0) { item { CatchUpCard(...) } }` block with a single item:

```kotlin
                        item {
                            ProtectionCard(
                                on = state.protectionOn,
                                hidingCount = state.hidingCount,
                                onToggle = vm::onToggleProtection,
                                onOpenSheet = vm::onOpenWallSheet,
                            )
                        }
```

Replace the trailing `if (state.catchUpVisible) { CatchUpSheet(...) }` block with:

```kotlin
    if (state.wallSheetVisible) {
        MatchWallSheet(
            fixtures = state.fixtures,
            hidingCount = state.hidingCount,
            lastSyncAtEpochMs = state.lastSyncAt?.toEpochMilli(),
            isOffline = state.isOffline,
            onMarkWatched = vm::onMarkGameWatched,
            onUnmark = vm::onUnmarkGame,
            onRefresh = vm::onRefreshSchedule,
            onDismiss = vm::onDismissWallSheet,
        )
    }
```

> `state.lastSyncAt` is an `Instant?`; `MatchWallSheet` takes `lastSyncAtEpochMs: Long?`, hence `?.toEpochMilli()`.

- [ ] **Step 4: Verify everything compiles** (ViewModel, screen, components all consistent now)

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (If it fails on `CatchUpCard`/`CatchUpSheet`/`ProtectionBanner` still referenced, those are deleted in Phase 11 — but FeedScreen must no longer reference them now, so deletion can also be pulled forward; see Phase 11.)

- [ ] **Step 5: Run the ViewModel unit tests (now compilable)**

Run: `./gradlew :app:testDebugUnitTest --tests "dev.crushingdigits.littlebird.ui.feed.FeedViewModelTest"`
Expected: PASS (all rewritten tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/components/ProtectionCard.kt app/src/main/java/dev/crushingdigits/littlebird/ui/components/GateSheet.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedScreen.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModel.kt app/src/main/java/dev/crushingdigits/littlebird/ui/feed/FeedUiState.kt app/src/test/java/dev/crushingdigits/littlebird/ui/feed/FeedViewModelTest.kt
git commit -m "feat: wall-based feed redaction with ProtectionCard + MatchWallSheet"
```

---

## Phase 10 — Screenshot tests (Roborazzi)

### Task 10.1: `ScheduleComponentsScreenshotTest`

**Files:**
- Create: `app/src/test/java/dev/crushingdigits/littlebird/ui/ScheduleComponentsScreenshotTest.kt`

- [ ] **Step 1: Write the test (reuses the `captureThemed` pattern from `FilterComponentsScreenshotTest`)**

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
import dev.crushingdigits.littlebird.domain.model.MatchResult
import dev.crushingdigits.littlebird.ui.components.ProtectionCard
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import dev.crushingdigits.littlebird.ui.feed.wall.FixtureRow
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
class ScheduleComponentsScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val ko = Instant.parse("2026-06-01T16:00:00Z")

    private fun fixture(state: FixtureState, score: String? = null, result: MatchResult? = null, isWall: Boolean = false) = FixtureUi(
        id = "g", opponent = "Chelsea", abbreviation = "CHE", isHome = true, competition = "English Premier League",
        kickoff = ko, state = state, scoreLine = score, result = result, isWall = isWall,
    )

    @Test fun `protection card - off`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_off.png") {
            ProtectionCard(on = false, hidingCount = 0, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `protection card - hiding`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_hiding.png") {
            ProtectionCard(on = true, hidingCount = 2, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `protection card - caught up`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_protection_card_caught_up.png") {
            ProtectionCard(on = true, hidingCount = 0, onToggle = {}, onOpenSheet = {})
        }
    }

    @Test fun `fixture row - watched shows score`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_watched.png") {
            FixtureRow(fixture("WATCHED".let { FixtureState.WATCHED }, score = "2–1", result = MatchResult.WIN), onMarkWatched = {}, onUnmark = {})
        }
    }

    @Test fun `fixture row - unwatched hides score and is the wall`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_unwatched.png") {
            FixtureRow(fixture(FixtureState.UNWATCHED, isWall = true), onMarkWatched = {}, onUnmark = {})
        }
    }

    @Test fun `fixture row - upcoming`() {
        composeTestRule.captureThemed("ScheduleComponentsScreenshotTest_fixture_upcoming.png") {
            FixtureRow(fixture(FixtureState.UPCOMING), onMarkWatched = {}, onUnmark = {})
        }
    }
}
```

> Replace the awkward `"WATCHED".let { FixtureState.WATCHED }` with simply `FixtureState.WATCHED` (typo guard — pass the enum directly).

- [ ] **Step 2: Fix the typo, then record baselines**

Run: `./gradlew :app:recordRoborazziDebug --tests "dev.crushingdigits.littlebird.ui.ScheduleComponentsScreenshotTest"`
Expected: PASS; 6 PNGs written under `app/src/test/screenshots/`.

- [ ] **Step 3: Verify the baselines compare clean**

Run: `./gradlew :app:verifyRoborazziDebug --tests "dev.crushingdigits.littlebird.ui.ScheduleComponentsScreenshotTest"`
Expected: PASS.

- [ ] **Step 4: Commit (test + baseline images)**

```bash
git add app/src/test/java/dev/crushingdigits/littlebird/ui/ScheduleComponentsScreenshotTest.kt app/src/test/screenshots/ScheduleComponentsScreenshotTest_*.png
git commit -m "test: screenshot baselines for ProtectionCard and FixtureRow"
```

> Manual visual check (test plan): the `fixture_unwatched` and `fixture_upcoming` baselines must contain **no score digits** — the hard rule, verified by eye.

---

## Phase 11 — Remove the old feature + update instrumented tests

### Task 11.1: Update `RedactedCard` copy (now date-based, not match-based)

**Files:**
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/RedactedCard.kt`

- [ ] **Step 1: Replace the two strings + content description**

Change `contentDescription = "Match content hidden"` → `contentDescription = "Hidden until you catch up"`, and `Text("Match content hidden", ...)` → `Text("Hidden until you catch up", ...)`. Leave the `"${item.source} · tap to reveal"` line unchanged.

- [ ] **Step 2: Verify it compiles** — `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/ui/components/RedactedCard.kt
git commit -m "refactor: redacted card copy reflects date-based hiding"
```

### Task 11.2: Delete `MarkWatched`, `CatchUpCard`, `CatchUpSheet`, `ProtectionBanner`

**Files:**
- Delete: `app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/MarkWatched.kt`
- Delete: `app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/MarkWatchedTest.kt`
- Delete: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/CatchUpCard.kt`
- Delete: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/CatchUpSheet.kt`
- Delete: `app/src/main/java/dev/crushingdigits/littlebird/ui/components/ProtectionBanner.kt`
- Modify: `app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt`

- [ ] **Step 1: Remove the `MarkWatched` bind + imports from `DomainModule`**

Delete the line `@Binds abstract fun markWatched(impl: MarkWatchedUseCase): MarkWatched` and the two imports `import ...MarkWatched` / `import ...MarkWatchedUseCase`.

- [ ] **Step 2: Delete the files**

```bash
git rm app/src/main/java/dev/crushingdigits/littlebird/domain/usecase/MarkWatched.kt \
       app/src/test/java/dev/crushingdigits/littlebird/domain/usecase/MarkWatchedTest.kt \
       app/src/main/java/dev/crushingdigits/littlebird/ui/components/CatchUpCard.kt \
       app/src/main/java/dev/crushingdigits/littlebird/ui/components/CatchUpSheet.kt \
       app/src/main/java/dev/crushingdigits/littlebird/ui/components/ProtectionBanner.kt
```

- [ ] **Step 3: Verify the app compiles**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (no remaining references to the deleted symbols).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/dev/crushingdigits/littlebird/di/DomainModule.kt
git commit -m "refactor: remove bulk MarkWatched, CatchUp card/sheet, and ProtectionBanner"
```

### Task 11.3: Update `FakeDataModule` for the new graph

**Files:**
- Modify: `app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt`

- [ ] **Step 1: Replace the whole file**

```kotlin
package dev.crushingdigits.littlebird

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.di.DataBindingsModule
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Singleton

class FakeNewsRepository : NewsRepository {
    private val items = MutableStateFlow(
        listOf(
            NewsItem(
                id = "match-1", source = "BBC Sport", tier = Tier.TIER_2_BEAT, category = Category.MATCH,
                isMatch = true, confirmed = false, author = null, role = null,
                title = "Match result hidden under spoiler protection", summary = "Full-time report.",
                link = "https://x/m1", imageUrl = null, publishedAt = Instant.ofEpochMilli(2_000), // after the wall -> hidden
            ),
            NewsItem(
                id = "transfer-1", source = "Fabrizio Romano", tier = Tier.TIER_1_RELIABLE, category = Category.TRANSFER,
                isMatch = false, confirmed = true, author = null, role = null,
                title = "Liverpool agree £65m deal for midfielder", summary = "Here we go.",
                link = "https://x/t1", imageUrl = null, publishedAt = Instant.ofEpochMilli(1_000), // before the wall -> visible
            ),
        ),
    )
    override fun observeFeed(): Flow<List<NewsItem>> = items
    override fun observeItem(id: String): Flow<NewsItem?> = items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    override suspend fun isEmpty(): Boolean = false
}

class FakeScheduleRepository : ScheduleRepository {
    // One played game at epoch 1500ms -> wall = 1500ms; the match item (2000ms) is hidden, transfer (1000ms) is visible.
    private val games = MutableStateFlow(
        listOf(
            Game(
                id = "g1", opponent = "Chelsea", abbreviation = "CHE", isHome = true, competition = "English Premier League",
                kickoff = Instant.ofEpochMilli(1_500), homeScore = 2, awayScore = 1,
            ),
        ),
    )
    override fun observeSchedule(): Flow<List<Game>> = games
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
}

class FakePreferencesRepository : PreferencesRepository {
    private val protection = MutableStateFlow(true)
    private val revealed = MutableStateFlow<Set<String>>(emptySet())
    private val reliabilityLevel = MutableStateFlow(3)
    private val pinnedFeed = MutableStateFlow<String?>(null)
    private val watched = MutableStateFlow<Set<String>>(emptySet())
    private val lastSync = MutableStateFlow<Long?>(null)
    private var firstSync: Long? = null

    override fun observeProtection(): Flow<Boolean> = protection
    override fun observeRevealed(): Flow<Set<String>> = revealed
    override suspend fun setProtection(on: Boolean) { protection.value = on }
    override suspend fun toggleProtection() { protection.value = !protection.value }
    override suspend fun reveal(id: String) { revealed.value = revealed.value + id }
    override suspend fun revealAll(ids: Set<String>) { revealed.value = revealed.value + ids }
    override suspend fun restoreRevealed(ids: Set<String>) { revealed.value = ids }
    override fun observeReliabilityLevel(): Flow<Int> = reliabilityLevel
    override fun observePinnedFeed(): Flow<String?> = pinnedFeed
    override suspend fun setReliabilityLevel(level: Int) { reliabilityLevel.value = level }
    override suspend fun setPinnedFeed(key: String?) { pinnedFeed.value = key }
    override fun observeWatchedGames(): Flow<Set<String>> = watched
    override suspend fun markGameWatched(id: String) { watched.value = watched.value + id }
    override suspend fun unmarkGameWatched(id: String) { watched.value = watched.value - id }
    override suspend fun setWatchedGames(ids: Set<String>) { watched.value = ids }
    override suspend fun getFirstScheduleSyncAt(): Long? = firstSync
    override suspend fun setFirstScheduleSyncAt(epochMs: Long) { firstSync = epochMs }
    override fun observeLastScheduleSyncAt(): Flow<Long?> = lastSync
    override suspend fun setLastScheduleSyncAt(epochMs: Long) { lastSync.value = epochMs }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataBindingsModule::class])
object FakeDataModule {
    @Provides @Singleton fun newsRepository(): NewsRepository = FakeNewsRepository()
    @Provides @Singleton fun scheduleRepository(): ScheduleRepository = FakeScheduleRepository()
    @Provides @Singleton fun preferencesRepository(): PreferencesRepository = FakePreferencesRepository()
}
```

> `FakeDataModule` replaces `DataBindingsModule` (which now also provides `scheduleRepository`), so it must provide it too. `Clock` is in `DatabaseModule` (Task 7.1) and is **not** replaced, so the real clock is used; the fake game's epoch-1500 kickoff is always in the past, keeping the wall deterministic.

- [ ] **Step 2: Verify androidTest compiles**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/dev/crushingdigits/littlebird/FakeDataModule.kt
git commit -m "test: update Hilt fakes for schedule graph"
```

### Task 11.4: Rewrite `SpoilerFlowUiTest` for the new flow

**Files:**
- Modify: `app/src/androidTest/java/dev/crushingdigits/littlebird/SpoilerFlowUiTest.kt`

- [ ] **Step 1: Replace the whole file**

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
 * End-to-end UIAutomator tests for the per-game spoiler wall.
 *
 * Fakes (FakeDataModule): one played game "Chelsea" (wall) + a match news item published after it
 * (hidden) and a transfer item before it (visible). Protection is ON by default.
 *
 * NOTE: COMPILED but RUN on a device via connectedDebugAndroidTest (Task 12.3).
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SpoilerFlowUiTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    /** A story after the wall renders redacted; tapping opens the (date-based) gate; reveal opens the reader. */
    @Test
    fun hidden_story_can_be_revealed_through_the_gate() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement(timeoutMs = 10_000L) { textAsString() == "Hidden until you catch up" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "This one might spoil you" }
        onElement(timeoutMs = 5_000L) { textAsString() == "Reveal anyway" }.click()
        // After reveal, the reader opens (assert its button; don't click — avoids leaving the app).
        onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }
    }

    /** Opening the fixtures sheet from the protection card and marking the wall game watched shows a toast. */
    @Test
    fun marking_a_game_watched_releases_held_stories() = uiAutomator {
        startActivity(MainActivity::class.java)

        // The protection card's action row opens the sheet.
        onElement(timeoutMs = 10_000L) { textAsString()?.contains("Manage") == true }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Fixtures & spoilers" }
        onElement(timeoutMs = 5_000L) { textAsString() == "Mark watched" }.click()
        // Toast confirms release.
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("unlocked") == true }
    }

    /** A non-match (before-wall) card opens the reader directly. */
    @Test
    fun tapping_a_visible_card_opens_the_detail_reader() = uiAutomator {
        startActivity(MainActivity::class.java)

        onElement(timeoutMs = 10_000L) { textAsString() == "Liverpool agree £65m deal for midfielder" }.click()
        onElement(timeoutMs = 5_000L) { textAsString() == "Open original" }
        pressBack()
        onElement(timeoutMs = 5_000L) { textAsString()?.contains("Spoiler protection") == true }
    }
}
```

- [ ] **Step 2: Verify androidTest compiles**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/dev/crushingdigits/littlebird/SpoilerFlowUiTest.kt
git commit -m "test: rewrite spoiler flow UI test for the per-game wall"
```

---

## Phase 12 — Verification & coverage

### Task 12.1: Full unit + screenshot suite green

- [ ] **Step 1: Run all JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all tests pass (new schedule/domain/VM tests + existing).

- [ ] **Step 2: Verify all screenshots**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: PASS — no unexpected diffs.

### Task 12.2: Close coverage gaps (kover)

- [ ] **Step 1: Invoke the `/write-unit-tests` skill — Workflow section**

Follow `/write-unit-tests` (Workflow): it runs the project's `unitTestsDebug koverXmlReport --rerun-tasks`, identifies uncovered lines in the new `data/schedule/**`, `domain/usecase/*Schedule*`/`*Wall*`/`*Game*`, `ui/feed/FixtureUi.kt`, and `FeedViewModel`, adds targeted tests to close gaps, and runs detekt. Commit any added tests.

- [ ] **Step 2: Commit (only if the workflow added tests)**

```bash
git add app/src/test
git commit -m "test: close schedule/wall coverage gaps"
```

### Task 12.3: Emulator tests on a device

- [ ] **Step 1: With an emulator/device connected, run the instrumented suite**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — `SpoilerFlowUiTest` (3 tests) + `FilterFlowUiTest` green.

> If no device is available, follow `/write-ui-tests` (Workflow) to launch one first. Do not mark this task done without a real run.

### Task 12.4: Manual test plan (human verification CI cannot do)

- [ ] Fresh install, online: the feed is **not** fully dark — only stories after the most-recent-unwatched-game's kickoff are hidden; games older than ~3 days are pre-marked watched.
- [ ] Mark the oldest unwatched game watched in the sheet → the band of stories up to the next unwatched game appears in the feed behind; toast shows "N stories unlocked" with a working **Undo**.
- [ ] Open the sheet: an **unwatched** played game shows **no score** anywhere; after marking it watched the score + W/D/L appear; tapping Undo on a watched row re-hides the score and the feed re-redacts.
- [ ] Airplane mode after a prior successful sync: cached fixtures still show; the sheet status bar reads "No connection · …"; marking a game watched still works locally.
- [ ] Airplane mode on a brand-new install (no prior sync): nothing is hidden (fail-open); the sheet offers "Try again".

### Task 12.5: Final branch verification

- [ ] **Step 1: Confirm clean tree + the full picture**

Run: `git status` (expect clean), `git log --oneline` (expect the design commit + the feature commits).

---

## Self-Review

**1. Spec coverage** — every spec section maps to a task:

| Spec | Task(s) |
|---|---|
| §2.1 TheSportsDB behind `ScheduleDataSource` | 1.2–1.4, 4.2 |
| §2.2 kotlinx.serialization | 1.1, 1.2 |
| §2.3 hide-ALL after the wall (drop `game` link) | 8.3 (`isHidden`), 8.4 |
| §2.4 keep per-item Gate (date-based copy) | 9.4, 11.1 |
| §2.5 day-1 seed (past=watched, 3-day window) | 6.2, 7.3 |
| §2.6 bottom-sheet UI | 9.3 |
| §2.7 entry point = ProtectionCard | 9.4 |
| §2.8 local-only watched state | 5.1, 7.2 |
| §2.9 wall = earliest played unwatched | 6.1 |
| §2.10 preserve score, gate in UI | 8.1, 9.2 |
| §3 model (wall, hidden, fxState) | 3.1, 6.1, 8.1, 8.3 |
| §4 data layer | Phase 1–4 |
| §5 domain (added/removed/kept) | Phase 6–7, 11.2 |
| §6 UI (card, sheet, rows, VM, GateSheet, fail-open) | Phase 8–9 |
| §7 edges (seed, fail-open, offline, refresh, time, out-of-order, all-caught-up, un-mark) | 7.3, 8.3, 9.3, 12.4 |
| §8 hard score rule (centralized) | 8.1 (`toFixtureUi`), 9.2 |
| §9 testing (unit/screenshot/UI) | Phases as TDD + 10, 11.4, 12 |

**2. Placeholder scan** — no "TBD/TODO"; the only "verify against live docs" note (TheSportsDB endpoints/team id/key, Task 1.3) is an external-API caveat carried from the spec (§11), not an internal gap.

**3. Type consistency** — `ScheduleRepository.{observeSchedule, refresh}`, `PreferencesRepository.{observeWatchedGames, markGameWatched, unmarkGameWatched, setWatchedGames, getFirstScheduleSyncAt, setFirstScheduleSyncAt, observeLastScheduleSyncAt, setLastScheduleSyncAt}`, `Game.{status(now), result(), liverpoolScore, opponentScore}`, `ComputeSpoilerWall(games, watched, now)`, `SeedWatchedOnFirstSync(games, firstSyncAt, windowDays)`, `MarkGameWatched(id, watched)`, `toFixtureUi(watched, now, wall)`, and the `FeedViewModel` constructor are used identically across production tasks (8.3) and tests (7.3, 8.4, 11.3) — verified consistent.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-05-spoiler-wall-watched-games.md`. Two execution options:

1. **Subagent-Driven (recommended)** — a fresh subagent per task, with a two-stage review between tasks; fast iteration.
2. **Inline Execution** — execute tasks in this session with checkpoints for review.

Which approach?
