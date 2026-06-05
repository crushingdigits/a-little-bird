package dev.crushingdigits.littlebird.ui.feed.wall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.ui.feed.FixtureState
import dev.crushingdigits.littlebird.ui.feed.FixtureUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val updatedFmt = DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchWallSheet(
    fixtures: List<FixtureUi>,
    hidingCount: Int,
    lastSyncAtEpochMs: Long?,
    isOffline: Boolean,
    onMarkWatched: (String) -> Unit,
    onUnmark: (String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showUpcoming by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Fixtures & spoilers", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                if (hidingCount > 0) {
                    "$hidingCount unwatched ${if (hidingCount > 1) "games are" else "game is"} hiding newer stories. " +
                        "Mark ${if (hidingCount > 1) "them" else "it"} watched to catch up."
                } else {
                    "All caught up — nothing's hidden. Every result below is yours to see."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (isOffline) "No connection · " else "Updated ") +
                        (lastSyncAtEpochMs?.let { updatedFmt.format(Instant.ofEpochMilli(it)) } ?: "—"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isOffline) "Try again" else "Refresh")
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showUpcoming, onClick = { showUpcoming = false }, label = { Text("Results") })
                FilterChip(selected = showUpcoming, onClick = { showUpcoming = true }, label = { Text("Upcoming") })
            }
            Spacer(Modifier.height(8.dp))

            val results = fixtures.filter { it.state != FixtureState.UPCOMING }.sortedByDescending { it.kickoff }
            val upcoming = fixtures.filter { it.state == FixtureState.UPCOMING }.sortedBy { it.kickoff }
            val shown = if (showUpcoming) upcoming else results

            shown.forEachIndexed { index, fixture ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                FixtureRow(fixture = fixture, onMarkWatched = onMarkWatched, onUnmark = onUnmark)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "A score is only ever shown once its game is watched",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
