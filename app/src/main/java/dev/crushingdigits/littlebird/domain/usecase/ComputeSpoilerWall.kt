package dev.crushingdigits.littlebird.domain.usecase

import dev.crushingdigits.littlebird.domain.model.Game
import dev.crushingdigits.littlebird.domain.model.GameStatus
import java.time.Instant
import javax.inject.Inject

/** The spoiler wall: kickoff of the earliest PLAYED, unwatched game. Null when caught up. */
fun interface ComputeSpoilerWall {
    operator fun invoke(games: List<Game>, watched: Set<String>, now: Instant): Instant?
}

class ComputeSpoilerWallUseCase @Inject constructor() : ComputeSpoilerWall {
    override fun invoke(games: List<Game>, watched: Set<String>, now: Instant): Instant? =
        games.filter { it.status(now) == GameStatus.PLAYED && it.id !in watched }
            .minByOrNull { it.kickoff }
            ?.kickoff
}
