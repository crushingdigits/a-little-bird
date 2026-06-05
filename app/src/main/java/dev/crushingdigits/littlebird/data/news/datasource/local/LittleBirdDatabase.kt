package dev.crushingdigits.littlebird.data.news.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameDao
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameEntity

@Database(entities = [NewsItemEntity::class, GameEntity::class], version = 2, exportSchema = false)
abstract class LittleBirdDatabase : RoomDatabase() {
    abstract fun newsDao(): NewsDao
    abstract fun gameDao(): GameDao
}
