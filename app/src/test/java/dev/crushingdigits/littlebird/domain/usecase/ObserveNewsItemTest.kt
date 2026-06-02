package dev.crushingdigits.littlebird.domain.usecase

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class ObserveNewsItemTest {

    private fun item(id: String) = NewsItem(
        id = id, source = "BBC", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = null, role = null, title = "t", summary = "s",
        link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private class FakeRepo(items: List<NewsItem>) : NewsRepository {
        val flow = MutableStateFlow(items)
        override fun observeFeed(): Flow<List<NewsItem>> = flow
        override fun observeItem(id: String): Flow<NewsItem?> = flow.map { it.firstOrNull { i -> i.id == id } }
        override suspend fun refresh(): Result<Unit> = Result.success(Unit)
        override suspend fun isEmpty(): Boolean = flow.value.isEmpty()
    }

    @Test fun `delegates to repository observeItem`() = runTest {
        val sut: ObserveNewsItem = ObserveNewsItemUseCase(FakeRepo(listOf(item("a"))))
        assertThat(sut("a").first()?.id).isEqualTo("a")
        assertThat(sut("b").first()).isNull()
    }
}
