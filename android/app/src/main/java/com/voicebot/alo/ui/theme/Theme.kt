package com.voicebot.alo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AloGreen = Color(0xFF25D366)
private val AloGreenDark = Color(0xFF0E8F4A)
private val AloBlue = Color(0xFF4D9FFF)
private val AloAmber = Color(0xFFFFB020)
private val AloRed = Color(0xFFFF5C5C)

private val DarkColors = darkColorScheme(
    primary = AloGreen,
    onPrimary = Color(0xFF04170D),
    primaryContainer = AloGreenDark,
    onPrimaryContainer = Color(0xFFE8FBF0),
    secondary = AloBlue,
    onSecondary = Color(0xFF04121F),
    tertiary = AloAmber,
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFE8EEF6),
    surface = Color(0xFF121821),
    onSurface = Color(0xFFE8EEF6),
    surfaceVariant = Color(0xFF182130),
    onSurfaceVariant = Color(0xFF8AA0B8),
    error = AloRed,
)

private val LightColors = lightColorScheme(
    primary = AloGreenDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F5E3),
    onPrimaryContainer = Color(0xFF04291A),
    secondary = Color(0xFF1668C7),
    tertiary = Color(0xFFB26A00),
    error = Color(0xFFC62828),
)

@Composable
fun AloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/** Colores de semántica del filtro, reutilizados por las pantallas. */
object AloColors {
    val read = Color(0xFF25D366)
    val discarded = Color(0xFFFF5C5C)
    val review = Color(0xFFFFB020)
    val info = Color(0xFF8AA0B8)
}
