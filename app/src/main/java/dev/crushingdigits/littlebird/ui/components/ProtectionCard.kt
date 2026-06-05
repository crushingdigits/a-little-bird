package dev.crushingdigits.littlebird.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.theme.Primary

/**
 * Feed entry point. Replaces ProtectionBanner + CatchUpCard.
 * - protection OFF: plain banner + toggle.
 * - protection ON, games hiding: toggle + a "Manage" action row.
 * - protection ON, caught up: toggle + a "Fixtures" action row.
 */
@Composable
fun ProtectionCard(
    on: Boolean,
    hidingCount: Int,
    onToggle: () -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (on) Primary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = BorderStroke(1.dp, if (on) Primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = if (on) Primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Spoiler protection", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !on -> "Off · showing everything, including match content"
                            hidingCount > 0 -> "On · hiding news from games you haven't watched"
                            else -> "On · nothing to hide right now"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = on, onCheckedChange = { onToggle() })
            }
            if (on) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline)
                TextButton(onClick = onOpenSheet, modifier = Modifier.fillMaxWidth()) {
                    val hiding = hidingCount > 0
                    Icon(
                        if (hiding) Icons.Default.Lock else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (hiding) Primary else Confirmed,
                    )
                    Text(
                        if (hiding) {
                            "  $hidingCount unwatched ${if (hidingCount > 1) "games are" else "game is"} hiding newer stories"
                        } else {
                            "  You're all caught up — every game watched"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(if (hiding) "Manage" else "Fixtures", style = MaterialTheme.typography.labelSmall)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }
}
