package com.voicebot.alo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Paleta clara, sobria y de alto contraste inspirada en interfaces de iOS. */
private val LightColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F2FF),
    onPrimaryContainer = Color(0xFF003B73),
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF9F0A),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
    error = Color(0xFFFF3B30),
    onError = Color.White,
)

private val AloTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

private val AloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun AloTheme(content: @Composable () -> Unit) {
    // Aló usa deliberadamente una apariencia clara y constante. Además de ser más legible
    // durante la conducción, evita que cada fabricante altere el contraste de los estados.
    MaterialTheme(
        colorScheme = LightColors,
        typography = AloTypography,
        shapes = AloShapes,
        content = content,
    )
}

/** Colores semánticos del filtro; no dependen del color principal de la interfaz. */
object AloColors {
    val read = Color(0xFF34C759)
    val discarded = Color(0xFFFF3B30)
    val review = Color(0xFFFF9F0A)
    val info = Color(0xFF007AFF)
}
