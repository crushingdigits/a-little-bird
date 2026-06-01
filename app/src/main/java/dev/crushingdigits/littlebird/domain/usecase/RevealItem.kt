package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

fun interface RevealItem { suspend operator fun invoke(id: String) }

class RevealItemUseCase @Inject constructor(private val prefs: PreferencesRepository) : RevealItem {
    override suspend fun invoke(id: String) = prefs.reveal(id)
}
