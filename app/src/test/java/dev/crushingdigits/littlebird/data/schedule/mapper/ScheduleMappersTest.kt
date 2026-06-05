package dev.crushingdigits.littlebird.data.schedule.mapper

import com.google.common.truth.Truth.assertThat
import dev.crushingdigits.littlebird.data.schedule.datasource.remote.GameDto
import org.junit.Test
import java.time.Instant

class ScheduleMappersTest {

    private fun dto(
        id: String = "1",
        home: String = "Liverpool",
        away: String = "Chelsea",
        hs: String? = "2",
        as_: String? = "1",
        ts: String? = "2026-05-31T16:30:00+00:00",
    ) = GameDto(
        id = id, homeTeam = home, awayTeam = away, homeScore = hs, awayScore = as_,
        league = "English Premier League", timestamp = ts, date = null, time = null,
    )

    @Test fun `maps a home fixture from Liverpool's perspective`() {
        val e = dto().toEntity()!!
        assertThat(e.id).isEqualTo("1")
        assertThat(e.opponent).isEqualTo("Chelsea")
        assertThat(e.isHome).isTrue()
        assertThat(e.homeScore).isEqualTo(2)
        assertThat(e.awayScore).isEqualTo(1)
        assertThat(e.kickoffEpochMs).isEqualTo(Instant.parse("2026-05-31T16:30:00Z").toEpochMilli())
    }

    @Test fun `maps an away fixture and picks the opponent`() {
        val e = dto(home = "Arsenal", away = "Liverpool", hs = null, as_ = null).toEntity()!!
        assertThat(e.opponent).isEqualTo("Arsenal")
        assertThat(e.isHome).isFalse()
        assertThat(e.homeScore).isNull()
        assertThat(e.awayScore).isNull()
    }

    @Test fun `falls back to dateEvent plus strTime when timestamp is absent`() {
        val e = dto(ts = null).copy(date = "2026-05-31", time = "16:30:00").toEntity()!!
        assertThat(e.kickoffEpochMs).isEqualTo(Instant.parse("2026-05-31T16:30:00Z").toEpochMilli())
    }

    @Test fun `returns null when no kickoff can be parsed`() {
        assertThat(dto(ts = null).copy(date = null, time = null).toEntity()).isNull()
    }

    @Test fun `entity maps to domain and derives result`() {
        val game = dto().toEntity()!!.toDomain()
        assertThat(game.opponent).isEqualTo("Chelsea")
        assertThat(game.liverpoolScore).isEqualTo(2)
        assertThat(game.result()!!.name).isEqualTo("WIN")
    }
}
