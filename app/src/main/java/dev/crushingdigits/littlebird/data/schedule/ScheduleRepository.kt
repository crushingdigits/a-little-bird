package dev.crushingdigits.littlebird.data.schedule

import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.ScheduleDataSource
import dev.crushingdigits.littlebird.data.schedule.mapper.toDomain
import dev.crushingdigits.littlebird.data.schedule.mapper.toEntity
import dev.crushingdigits.littlebird.domain.model.Game
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    fun observeSchedule(): Flow<List<Game>>
    suspend fun refresh(): Result<Unit>
}

class ScheduleRepositoryImpl(
    private val remote: ScheduleDataSource,
    private val local: ScheduleLocalDataSource,
) : ScheduleRepository {

    override fun observeSchedule(): Flow<List<Game>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun refresh(): Result<Unit> = runCatching {
        val entities = remote.fetchFixtures().mapNotNull { it.toEntity() }
        if (entities.isNotEmpty()) local.upsert(entities)
    }
}
