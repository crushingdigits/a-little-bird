package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_items ORDER BY publishedAtEpochMs DESC")
    fun observeAll(): Flow<List<NewsItemEntity>>

    @Query("SELECT * FROM news_items WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NewsItemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NewsItemEntity>)

    @Query("SELECT COUNT(*) FROM news_items")
    suspend fun count(): Int
}
