package dev.crushingdigits.littlebird.ui.feed.wall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.domain.model.MatchResult
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.theme.Primary
import dev.crushingdigits.littlebird.ui.theme.Tier3
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM").withZone(ZoneId.systemDefault())
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@Composable
fun FixtureRow(
    fixture: FixtureUi,
    onMarkWatched: (String) -> Unit,
    onUnmark: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CrestTile(fixture)
        Column(Modifier.weight(1f)) {
            if (fixture.isWall) {
                Text("NEXT TO CATCH UP", style = MaterialTheme.typography.labelSmall, color = Primary)
            }
            Text(fixture.opponent, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(competitionTag(fixture.competition))
                    append(" · ")
                    append(if (fixture.isHome) "Home" else "Away")
                    append(" · ")
                    append(dayFmt.format(fixture.kickoff))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (fixture.state) {
            FixtureState.WATCHED -> WatchedTrailing(fixture, onUnmark)
            FixtureState.UNWATCHED -> FilledTonalButton(onClick = { onMarkWatched(fixture.id) }) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Mark watched")
            }
            FixtureState.UPCOMING -> Column(horizontalAlignment = Alignment.End) {
                Text(timeFmt.format(fixture.kickoff), style = MaterialTheme.typography.titleMedium)
                Text("UPCOMING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WatchedTrailing(fixture: FixtureUi, onUnmark: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        fixture.result?.let { r ->
            val color = when (r) {
                MatchResult.WIN -> Confirmed
                MatchResult.DRAW -> MaterialTheme.colorScheme.onSurfaceVariant
                MatchResult.LOSS -> Tier3
            }
            Text(r.name.take(1), style = MaterialTheme.typography.labelSmall, color = color)
        }
        Text(
            fixture.scoreLine.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )
        IconButton(onClick = { onUnmark(fixture.id) }) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Un-mark watched")
        }
    }
}

@Composable
private fun CrestTile(fixture: FixtureUi) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            fixture.abbreviation,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
