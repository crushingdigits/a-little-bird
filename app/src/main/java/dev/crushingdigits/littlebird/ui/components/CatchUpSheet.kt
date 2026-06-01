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
import androidx.compose.material.icons.filled.Visibility
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
import dev.crushingdigits.littlebird.ui.theme.Confirmed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchUpSheet(count: Int, onConfirm: () -> Unit, onNotYet: () -> Unit, onDismiss: () -> Unit) {
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
                    .background(Confirmed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = Confirmed)
            }
            Text("All caught up?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Marking the match as watched reveals $count held update${if (count > 1) "s" else ""} — " +
                    "scores, results and player ratings will be shown.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("Yes, reveal them")
            }
            FilledTonalButton(onClick = onNotYet, modifier = Modifier.fillMaxWidth()) {
                Text("Not yet")
            }
        }
    }
}
