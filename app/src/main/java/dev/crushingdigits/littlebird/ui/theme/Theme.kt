package dev.crushingdigits.littlebird.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.crushingdigits.littlebird.domain.model.Tier

// darkColorScheme in this BOM does not include surfaceContainerHighest as a named param,
// so we apply it via copy() after construction.
private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    surface = Surface,
    onSurface = OnSurface,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceVariant,
    outline = OutlineColor,
    background = Surface,
    onBackground = OnSurface,
).copy(surfaceContainerHighest = SurfaceContainerHighest)

fun tierColor(tier: Tier): Color = when (tier) {
    Tier.TIER_1_RELIABLE -> Tier1
    Tier.TIER_2_BEAT -> Tier2
    Tier.TIER_3_SPECULATIVE -> Tier3
}

@Composable
fun LittleBirdTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, typography = AppTypography, content = content)
}
