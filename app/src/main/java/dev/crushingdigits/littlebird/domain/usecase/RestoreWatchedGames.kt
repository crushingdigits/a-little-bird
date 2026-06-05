package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

/** Restores the watched-game set to a prior snapshot — used to Undo a mark/un-mark. */
fun interface RestoreWatchedGames { suspend operator fun invoke(ids: Set<String>) }

class RestoreWatchedGamesUseCase @Inject constructor(private val prefs: PreferencesRepository) : RestoreWatchedGames {
    override suspend fun invoke(ids: Set<String>) = prefs.setWatchedGames(ids)
}
