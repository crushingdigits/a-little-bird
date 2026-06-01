package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveProtection { operator fun invoke(): Flow<Boolean> }

class ObserveProtectionUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveProtection {
    override fun invoke(): Flow<Boolean> = prefs.observeProtection()
}
