package com.gyansetu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GyanColors {
    val Saffron     = Color(0xFFFF9933)
    val SaffronDeep = Color(0xFFF37820)
    val SaffronSoft = Color(0xFFFFE4C7)
    val Green       = Color(0xFF138808)
    val GreenDeep   = Color(0xFF0E6606)
    val GreenSoft   = Color(0xFFD6F0D2)
    val Yellow      = Color(0xFFFFD43B)
    val YellowSoft  = Color(0xFFFFF4C2)
    val Pink        = Color(0xFFFF6F91)
    val PinkSoft    = Color(0xFFFFE0E9)
    val Purple      = Color(0xFF9C6ADE)
    val PurpleSoft  = Color(0xFFEDE3FA)
    val Sky         = Color(0xFF4FC3F7)
    val SkySoft     = Color(0xFFE1F5FE)
    val Ink         = Color(0xFF2A1810)
    val InkSoft     = Color(0xFF5C4A3E)
    val Bg          = Color(0xFFFFFAF2)
    val Paper       = Color(0xFFFFFFFF)
}

/** WCAG-AAA contrast palette for kids with low vision or screen-reader-adjacent
 *  visual needs. Bumps text/background contrast to ≥7:1 across the board. */
object GyanColorsHC {
    val Saffron     = Color(0xFFCC5500)
    val SaffronDeep = Color(0xFF993D00)
    val SaffronSoft = Color(0xFFFFE0CC)
    val Green       = Color(0xFF005C00)
    val GreenDeep   = Color(0xFF003300)
    val GreenSoft   = Color(0xFFCCEFCC)
    val Yellow      = Color(0xFFFFD700)
    val YellowSoft  = Color(0xFFFFF8CC)
    val Pink        = Color(0xFFA00040)
    val PinkSoft    = Color(0xFFFFCCDA)
    val Purple      = Color(0xFF4A1F8C)
    val PurpleSoft  = Color(0xFFE0CCF5)
    val Sky         = Color(0xFF005A99)
    val SkySoft     = Color(0xFFCCE5F5)
    val Ink         = Color(0xFF000000)
    val InkSoft     = Color(0xFF333333)
    val Bg          = Color(0xFFFFFFFF)
    val Paper       = Color(0xFFFFFFFF)
}

private val LightScheme = lightColorScheme(
    primary = GyanColors.Saffron,
    onPrimary = Color.White,
    secondary = GyanColors.Green,
    background = GyanColors.Bg,
    onBackground = GyanColors.Ink,
    surface = GyanColors.Paper,
    onSurface = GyanColors.Ink,
)

/** Dark variant — saffron stays (it's brand identity) but everything that
 *  was cream/white inverts to warm-dark. Compose picks this when the system
 *  is in dark mode (auto matches the launcher icon's themed-icon tint). */
private val DarkScheme = darkColorScheme(
    primary = GyanColors.Saffron,
    onPrimary = Color(0xFF1F1610),
    secondary = GyanColors.Green,
    background = Color(0xFF1F1610),
    onBackground = GyanColors.SaffronSoft,
    surface = Color(0xFF2A1810),
    onSurface = GyanColors.SaffronSoft,
)

@Composable
fun GyanSetuTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
            bodyLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
        ),
        content = content,
    )
}
