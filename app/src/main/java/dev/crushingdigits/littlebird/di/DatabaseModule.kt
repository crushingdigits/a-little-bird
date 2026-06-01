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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext ctx: Context): LittleBirdDatabase =
        Room.databaseBuilder(ctx, LittleBirdDatabase::class.java, "little_bird.db").build()

    @Provides
    fun newsDao(db: LittleBirdDatabase): NewsDao = db.newsDao()

    @Provides
    @Singleton
    fun newsLocalDataSource(dao: NewsDao) = NewsLocalDataSource(dao)
}
