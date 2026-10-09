/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Lightweight starter sticker tray. Selection is handed to the caller so the
 * messaging layer can encode and send the sticker as an encrypted event.
 */
data class ChatSticker(
    val id: String,
    val emoji: String,
    val description: String
)

val StarterStickers = listOf(
    ChatSticker("wave", "👋", "Wave"),
    ChatSticker("heart", "💙", "Blue heart"),
    ChatSticker("laugh", "😂", "Laughing"),
    ChatSticker("love", "🥹", "Touched"),
    ChatSticker("sparkles", "✨", "Sparkles"),
    ChatSticker("thumbs-up", "👍", "Thumbs up"),
    ChatSticker("cat", "😺", "Cat"),
    ChatSticker("party", "🎉", "Party")
)

@Composable
fun StickerPicker(
    onStickerSelected: (ChatSticker) -> Unit,
    modifier: Modifier = Modifier,
    stickers: List<ChatSticker> = StarterStickers
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stickers, key = { it.id }) { sticker ->
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(56.dp)
                    .semantics {
                        contentDescription = "Send " + sticker.description + " sticker"
                    }
                    .clickable { onStickerSelected(sticker) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = sticker.emoji,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
