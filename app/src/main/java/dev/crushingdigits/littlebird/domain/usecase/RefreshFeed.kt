package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.news.NewsRepository
import javax.inject.Inject

fun interface RefreshFeed { suspend operator fun invoke(): Result<Unit> }

class RefreshFeedUseCase @Inject constructor(private val repo: NewsRepository) : RefreshFeed {
    override suspend fun invoke(): Result<Unit> = repo.refresh()
}
