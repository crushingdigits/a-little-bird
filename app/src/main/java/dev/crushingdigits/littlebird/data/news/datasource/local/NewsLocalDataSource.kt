package dev.crushingdigits.littlebird.data.news.datasource.local

import kotlinx.coroutines.flow.Flow

class NewsLocalDataSource(private val dao: NewsDao) {
    fun observeAll(): Flow<List<NewsItemEntity>> = dao.observeAll()
    fun observeById(id: String): Flow<NewsItemEntity?> = dao.observeById(id)
    suspend fun upsert(items: List<NewsItemEntity>) = dao.upsertAll(items)
    suspend fun count(): Int = dao.count()
}
