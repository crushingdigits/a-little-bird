package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [NewsItemEntity::class], version = 1, exportSchema = false)
abstract class LittleBirdDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
}
