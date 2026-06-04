package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.littlebird.domain.model.FeedType
import dev.crushingdigits.littlebird.ui.feed.FeedTab

/**
 * Horizontally-scrollable single-select news-type tab strip. The active tab gets a primary
 * underline; tabs with a zero count are dimmed; the pinned feed shows a filled-pin glyph.
 */
@Composable
fun FeedTypeTabs(
    tabs: List<FeedTab>,
    selected: FeedType,
    pinned: FeedType?,
    onSelect: (FeedType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectableGroup()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.type == selected
            val dim = tab.count == 0 && !isSelected
            val labelColor = when {
                isSelected -> MaterialTheme.colorScheme.onSurface
                dim -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(tab.type) },
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (tab.type == pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        text = tab.type.shortLabel(),
                        color = labelColor,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    Text(text = "${tab.count}", color = labelColor.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .height(2.dp)
                        .width(if (isSelected) 20.dp else 0.dp)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                )
            }
        }
    }
}
