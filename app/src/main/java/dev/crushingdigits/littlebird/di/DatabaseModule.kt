package dev.crushingdigits.littlebird.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.crushingdigits.littlebird.data.news.datasource.local.LittleBirdDatabase
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsDao
import dev.crushingdigits.littlebird.data.news.datasource.local.NewsLocalDataSource
import dev.crushingdigits.littlebird.data.schedule.datasource.local.GameDao
import dev.crushingdigits.littlebird.data.schedule.datasource.local.ScheduleLocalDataSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): LittleBirdDatabase =
        Room.databaseBuilder(ctx, LittleBirdDatabase::class.java, "little_bird.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun newsDao(db: LittleBirdDatabase): NewsDao = db.newsDao()

    @Provides
    fun gameDao(db: LittleBirdDatabase): GameDao = db.gameDao()

    @Provides
    @Singleton
    fun newsLocalDataSource(dao: NewsDao) = NewsLocalDataSource(dao)

    @Provides
    @Singleton
    fun scheduleLocalDataSource(dao: GameDao) = ScheduleLocalDataSource(dao)
}
