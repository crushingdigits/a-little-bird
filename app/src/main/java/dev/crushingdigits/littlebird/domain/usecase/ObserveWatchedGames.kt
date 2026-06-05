package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveWatchedGames { operator fun invoke(): Flow<Set<String>> }

class ObserveWatchedGamesUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveWatchedGames {
    override fun invoke(): Flow<Set<String>> = prefs.observeWatchedGames()
}
