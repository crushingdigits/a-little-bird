package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Reveals all currently-held match ids; returns the prior revealed set so the caller can offer Undo. */
fun interface MarkWatched { suspend operator fun invoke(heldIds: Set<String>): Set<String> }

class MarkWatchedUseCase @Inject constructor(private val prefs: PreferencesRepository) : MarkWatched {
    override suspend fun invoke(heldIds: Set<String>): Set<String> {
        val snapshot = prefs.observeRevealed().first()
        prefs.revealAll(heldIds)
        return snapshot
    }
}
