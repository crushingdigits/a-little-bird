package dev.crushingdigits.littlebird.ui.feed.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.crushingdigits.littlebird.domain.model.Tier
import dev.crushingdigits.littlebird.ui.theme.tierColor

/**
 * Connected 3-segment cumulative reliability control (T1·T2·T3). Segments at/below [selected]
 * are "included"; the chosen segment carries a 3dp bottom accent in primary. Tapping a segment
 * sets the ceiling via [onSelect].
 */
@Composable
fun ReliabilityLadder(
    selected: Tier,
    onSelect: (Tier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val outline = MaterialTheme.colorScheme.outline
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, outline, shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tier.entries.forEachIndexed { index, tier ->
            if (index > 0) {
                Box(Modifier.width(1.dp).height(32.dp).background(outline))
            }
            val included = tier.ordinal <= selected.ordinal
            val isChosen = tier == selected
            val pip = tierColor(tier).copy(alpha = if (included) 1f else 0.35f)
            Box(
                modifier = Modifier
                    .testTag("tier_${tier.ordinal + 1}")
                    .height(32.dp)
                    .background(if (included) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
                    .drawBehind {
                        if (isChosen) {
                            val h = 3.dp.toPx()
                            drawRect(accent, topLeft = Offset(0f, size.height - h), size = Size(size.width, h))
                        }
                    }
                    .selectable(selected = isChosen, role = Role.RadioButton, onClick = { onSelect(tier) })
                    .padding(horizontal = 13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(pip))
                    Text(
                        text = "T${tier.ordinal + 1}",
                        color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
