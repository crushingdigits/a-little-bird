package dev.crushingdigits.littlebird.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.littlebird.ui.theme.Primary

@Composable
fun TopBar(protectionOn: Boolean, onToggleShield: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App logo placeholder
        Spacer(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Primary),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("A Little Bird", style = MaterialTheme.typography.titleMedium)
            Text(
                "LFC TRANSFERS & BACKROOM",
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Shield button: FilledIconButton when protection is ON (Primary container),
        // plain IconButton when protection is OFF.
        // The plan used IconButtonDefaults.iconButtonColors() for the inactive state
        // which no longer exists as a standalone factory; IconButton handles that default.
        if (protectionOn) {
            FilledIconButton(
                onClick = onToggleShield,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary),
            ) {
                Icon(Icons.Default.Shield, contentDescription = "Spoiler protection")
            }
        } else {
            IconButton(onClick = onToggleShield) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = "Spoiler protection",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = "More")
        }
    }
}
