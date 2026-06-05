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
