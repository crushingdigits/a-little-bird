package dev.crushingdigits.littlebird.data.schedule.datasource.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey val id: String,
    val opponent: String,
    val abbreviation: String,
    val isHome: Boolean,
    val competition: String,
    val kickoffEpochMs: Long,
    val homeScore: Int?,
    val awayScore: Int?,
)
