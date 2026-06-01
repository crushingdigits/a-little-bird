package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import dev.crushingdigits.littlebird.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveFeed { operator fun invoke(): Flow<List<NewsItem>> }

class ObserveFeedUseCase @Inject constructor(private val repo: NewsRepository) : ObserveFeed {
    override fun invoke(): Flow<List<NewsItem>> = repo.observeFeed()
}
