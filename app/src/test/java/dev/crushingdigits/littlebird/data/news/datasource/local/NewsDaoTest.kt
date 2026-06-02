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
        dao.upsertAll(listOf(entity("1", at = 100))) // duplicate id -> replaced, not duplicated

        dao.observeAll().test {
            val rows = awaitItem()
            assertThat(rows.map { it.id }).containsExactly("2", "1").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(dao.count()).isEqualTo(2)
    }

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
}
