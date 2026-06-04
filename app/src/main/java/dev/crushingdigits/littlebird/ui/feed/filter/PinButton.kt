package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Pins/unpins the current feed as the default landing feed. Filled (primary) when [pinned]. */
@Composable
fun PinButton(pinned: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    IconToggleButton(checked = pinned, onCheckedChange = { onToggle() }, modifier = modifier) {
        Icon(
            imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = if (pinned) "Unpin default feed" else "Pin as default feed",
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
