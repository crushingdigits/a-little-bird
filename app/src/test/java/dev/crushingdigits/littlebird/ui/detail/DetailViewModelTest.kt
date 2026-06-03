package dev.crushingdigits.littlebird.ui.detail

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.MainDispatcherRule
import dev.crushingdigits.littlebird.domain.model.Category
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.domain.usecase.ObserveNewsItem
import dev.crushingdigits.littlebird.ui.navigation.DetailKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DetailViewModelTest {
    @get:Rule val mainRule = MainDispatcherRule()

    private fun item(id: String) = NewsItem(
        id = id, source = "BBC", tier = Tier.TIER_2_BEAT, category = Category.TRANSFER,
        isMatch = false, confirmed = false, author = "David Ornstein", role = "The Athletic",
        title = "t", summary = "s", link = "https://x/$id", imageUrl = null, publishedAt = Instant.ofEpochMilli(1),
    )

    private fun vm(flow: MutableStateFlow<NewsItem?>, id: String = "a") =
        DetailViewModel(DetailKey(id), ObserveNewsItem { flow })

    @Test fun `emits the resolved item`() = runTest {
        val sut = vm(MutableStateFlow(item("a")))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.item?.id).isEqualTo("a")
            assertThat(s.notFound).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun `emits notFound when the item resolves to null`() = runTest {
        val sut = vm(MutableStateFlow(null))
        sut.uiState.test {
            val s = awaitItem()
            assertThat(s.item).isNull()
            assertThat(s.notFound).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
