package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Marks ([watched]=true) or un-marks a game; returns the prior watched set so the caller can Undo. */
fun interface MarkGameWatched { suspend operator fun invoke(id: String, watched: Boolean): Set<String> }

class MarkGameWatchedUseCase @Inject constructor(private val prefs: PreferencesRepository) : MarkGameWatched {
    override suspend fun invoke(id: String, watched: Boolean): Set<String> {
        val snapshot = prefs.observeWatchedGames().first()
        if (watched) prefs.markGameWatched(id) else prefs.unmarkGameWatched(id)
        return snapshot
    }
}
