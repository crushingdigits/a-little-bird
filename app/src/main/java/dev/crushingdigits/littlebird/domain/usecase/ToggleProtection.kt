package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import javax.inject.Inject

fun interface ToggleProtection { suspend operator fun invoke() }

class ToggleProtectionUseCase @Inject constructor(private val prefs: PreferencesRepository) : ToggleProtection {
    override suspend fun invoke() = prefs.toggleProtection()
}
