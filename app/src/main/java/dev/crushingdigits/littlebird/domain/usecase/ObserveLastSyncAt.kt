package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveLastSyncAt { operator fun invoke(): Flow<Long?> }

class ObserveLastSyncAtUseCase @Inject constructor(private val prefs: PreferencesRepository) : ObserveLastSyncAt {
    override fun invoke(): Flow<Long?> = prefs.observeLastScheduleSyncAt()
}
