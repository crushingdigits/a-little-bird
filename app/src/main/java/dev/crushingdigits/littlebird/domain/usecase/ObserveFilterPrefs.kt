package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.domain.model.Tier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Persisted filter prefs mapped to domain types: the reliability ceiling and the pinned feed (null = none). */
data class FilterPrefs(val maxTier: Tier, val pinned: FeedType?)

fun interface ObserveFilterPrefs { operator fun invoke(): Flow<FilterPrefs> }

internal class ObserveFilterPrefsUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : ObserveFilterPrefs {
    override fun invoke(): Flow<FilterPrefs> = combine(
        prefs.observeReliabilityLevel(),
        prefs.observePinnedFeed(),
    ) { level, pinnedKey ->
        FilterPrefs(
            maxTier = Tier.entries[(level - 1).coerceIn(0, Tier.entries.lastIndex)],
            pinned = pinnedKey?.let { FeedType.fromKey(it) },
        )
    }
}
