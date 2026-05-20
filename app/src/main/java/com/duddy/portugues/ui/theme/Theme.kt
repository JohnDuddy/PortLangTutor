package com.duddy.portugues.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DuddyColorScheme = lightColorScheme(
    primary = Color(0xFF14776A),
    onPrimary = Color.White,
    secondary = Color(0xFFD95241),
    onSecondary = Color.White,
    tertiary = Color(0xFFE6AD39),
    background = Color(0xFFFFFDF7),
    onBackground = Color(0xFF16312F),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF16312F),
    surfaceVariant = Color(0xFFE7F4ED),
    onSurfaceVariant = Color(0xFF4E5F59)
)

@Composable
fun DuddyPortuguesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DuddyColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
