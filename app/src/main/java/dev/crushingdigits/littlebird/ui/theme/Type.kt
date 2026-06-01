package dev.crushingdigits.littlebird.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// TODO(assets, spec §12): replace with bundled Archivo + Space Mono families.
val DisplayFamily = FontFamily.SansSerif
val MonoFamily = FontFamily.Monospace

val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    bodyMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelSmall = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp),
)
