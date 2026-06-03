package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveNewsItem { operator fun invoke(id: String): Flow<NewsItem?> }

class ObserveNewsItemUseCase @Inject constructor(private val repo: NewsRepository) : ObserveNewsItem {
    override fun invoke(id: String): Flow<NewsItem?> = repo.observeItem(id)
}
