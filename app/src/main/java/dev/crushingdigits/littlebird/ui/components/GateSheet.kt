package dev.crushingdigits.littlebird.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.crushingdigits.littlebird.ui.theme.Primary
import dev.crushingdigits.littlebird.ui.theme.Tier3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GateSheet(source: String, onReveal: () -> Unit, onKeepHidden: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Primary)
            }
            Text("This one might spoil you", style = MaterialTheme.typography.titleLarge)
            Text(
                "From $source. It was published after a game you haven't marked watched — it may reveal a result.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("PUBLISHED AFTER AN UNWATCHED GAME", color = Tier3) },
            )
            Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                Text("Reveal anyway")
            }
            FilledTonalButton(onClick = onKeepHidden, modifier = Modifier.fillMaxWidth()) {
                Text("Keep it hidden")
            }
        }
    }
}
