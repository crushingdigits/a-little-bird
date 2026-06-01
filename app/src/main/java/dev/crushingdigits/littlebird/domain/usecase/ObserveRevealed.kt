package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveRevealed { operator fun invoke(): Flow<Set<String>> }

class ObserveRevealedUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveRevealed {
    override fun invoke(): Flow<Set<String>> = prefs.observeRevealed()
}
