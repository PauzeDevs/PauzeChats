package com.pauze.chats.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PauzeDarkColors = darkColorScheme()

@Composable
fun PauzeChatsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PauzeDarkColors,
        content = content
    )
}
