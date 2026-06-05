package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.data.schedule.ScheduleRepository
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

fun interface ObserveSchedule { operator fun invoke(): Flow<List<Game>> }

class ObserveScheduleUseCase @Inject constructor(private val repo: ScheduleRepository) : ObserveSchedule {
    override fun invoke(): Flow<List<Game>> = repo.observeSchedule()
}
