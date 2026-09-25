package com.arcticfishtrail.game.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val ArcticColors = darkColorScheme(
    primary = IceTeal,
    onPrimary = PolarNight,
    secondary = SunGold,
    onSecondary = PolarNight,
    tertiary = AuroraGreen,
    onTertiary = PolarNight,
    background = PolarNight,
    onBackground = FrostWhite,
    surface = GlacierBlue,
    onSurface = FrostWhite,
    surfaceVariant = GlacierBlue,
    onSurfaceVariant = SlateIce,
    error = SalmonCoral,
    onError = PolarNight,
    outline = IceTeal,
)

/** Material 3 type scale on the system font (no bundled fonts), all in sp → honours font scaling. */
/** Soft dark shadow keeps light text readable over the bright ice/aurora art. */
private val TextShadow = Shadow(color = Color(0xCC050C18), offset = Offset(0f, 2f), blurRadius = 6f)

private val ArcticTypography = Typography(
    displaySmall = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp),
    labelMedium = TextStyle(shadow = TextShadow, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
)

@Composable
fun ArcticFishTrailTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ArcticColors, typography = ArcticTypography, content = content)
}
