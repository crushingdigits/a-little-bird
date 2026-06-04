package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import dev.crushingdigits.littlebird.domain.model.Tier
import javax.inject.Inject

fun interface SetReliabilityLevel { suspend operator fun invoke(maxTier: Tier) }

internal class SetReliabilityLevelUseCase @Inject constructor(
    private val prefs: PreferencesRepository,
) : SetReliabilityLevel {
    override suspend fun invoke(maxTier: Tier) = prefs.setReliabilityLevel(maxTier.ordinal + 1)
}
