package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import javax.inject.Inject

fun interface SetPinnedFeed { suspend operator fun invoke(type: FeedType?) }

internal class SetPinnedFeedUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : SetPinnedFeed {
    override suspend fun invoke(type: FeedType?) = prefs.setPinnedFeed(type?.key)
}
