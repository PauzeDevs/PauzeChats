package com.pauze.chats.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PauzeDarkColors = darkColorScheme(
    primary = Color(0xFF9295FF),
    onPrimary = Color(0xFF11132B),
    primaryContainer = Color(0xFF292D5B),
    onPrimaryContainer = Color(0xFFE5E6FF),
    secondary = Color(0xFF65D9EE),
    onSecondary = Color(0xFF08232A),
    secondaryContainer = Color(0xFF153B48),
    onSecondaryContainer = Color(0xFFD0F7FF),
    background = Color(0xFF090B14),
    onBackground = Color(0xFFF3F4FF),
    surface = Color(0xFF111522),
    onSurface = Color(0xFFF3F4FF),
    surfaceVariant = Color(0xFF1C2434),
    onSurfaceVariant = Color(0xFFB2BBD0),
    outline = Color(0xFF37435A),
    outlineVariant = Color(0xFF283246),
    error = Color(0xFFFF718D),
    onError = Color(0xFF2B0912)
)

@Composable
fun PauzeChatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PauzeDarkColors,
        content = content
    )
}
