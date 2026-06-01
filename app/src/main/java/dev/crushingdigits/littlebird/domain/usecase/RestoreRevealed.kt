package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

/** Restores the revealed-id set to a prior snapshot — used to Undo a "mark as watched". */
fun interface RestoreRevealed { suspend operator fun invoke(ids: Set<String>) }

class RestoreRevealedUseCase @Inject constructor(private val prefs: PreferencesRepository) : RestoreRevealed {
    override suspend fun invoke(ids: Set<String>) = prefs.restoreRevealed(ids)
}
