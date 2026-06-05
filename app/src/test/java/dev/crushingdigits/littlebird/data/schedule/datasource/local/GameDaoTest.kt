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
