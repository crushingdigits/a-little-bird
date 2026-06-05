package dev.crushingdigits.littlebird.data.schedule.datasource.local

import kotlinx.coroutines.flow.Flow

class ScheduleLocalDataSource(private val dao: GameDao) {
    fun observeAll(): Flow<List<GameEntity>> = dao.observeAll()
    suspend fun upsert(games: List<GameEntity>) = dao.upsertAll(games)
    suspend fun count(): Int = dao.count()
}
