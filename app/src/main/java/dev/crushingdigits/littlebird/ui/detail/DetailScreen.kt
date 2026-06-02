package dev.crushingdigits.littlebird.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.crushingdigits.littlebird.domain.model.NewsItem
import dev.crushingdigits.littlebird.ui.components.TierChip
import dev.crushingdigits.littlebird.ui.theme.Confirmed
import dev.crushingdigits.littlebird.ui.util.relativeTime

@Composable
fun DetailScreen(viewModel: DetailViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val item = state.item
    when {
        state.notFound -> DetailMessage("Story unavailable", "It may have dropped from the latest feed.", onBack, modifier)
        item != null -> DetailContent(
            item = item,
            relativeTime = item.relativeTime(),
            onBack = onBack,
            onOpenOriginal = { openOriginal(context, item.link) },
            onShare = { shareItem(context, item) },
            modifier = modifier,
        )
        // Pre-emission snapshot: item not yet resolved (item == null, not yet notFound).
        else -> DetailMessage("Loading…", "", onBack, modifier)
    }
}

@Composable
fun DetailContent(
    item: NewsItem,
    relativeTime: String,
    onBack: () -> Unit,
    onOpenOriginal: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TierChip(item.tier)
                Text(if (relativeTime == "now") "just now" else "$relativeTime ago", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.confirmed) {
                    Spacer(Modifier.weight(1f))
                    Text("✓ Here we go", color = Confirmed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Text(item.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

            if (item.imageUrl != null) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                )
            }

            Byline(item)

            Text(
                text = item.summary.ifBlank { "Summary unavailable — open the original to read more." },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (item.link.isNotBlank()) {
                // Hidden for guid-only RSS items with no resolvable link.
                Button(onClick = onOpenOriginal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Open original")
                }
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Share")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Byline(item: NewsItem) {
    val name = item.author ?: item.source
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) { Text(initials, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        Column {
            Text(name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(
                listOfNotNull(item.role, "T${item.tier.ordinal + 1} ${item.tier.label}").joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun DetailMessage(title: String, subtitle: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(6.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
