package com.pauze.chats

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pauze.chats.ui.PauzeChatsApp

class MainActivity : ComponentActivity() {
    private var inviteCode by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        inviteCode = parseInviteCode(intent)

        setContent {
            PauzeChatsApp(initialInviteCode = inviteCode)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        inviteCode = parseInviteCode(intent)
    }

    private fun parseInviteCode(intent: Intent?): String? {
        val uri: Uri = intent?.data ?: return null

        if (uri.scheme != "pauzechats" || uri.host != "invite") {
            return null
        }

        val code = uri.pathSegments.singleOrNull() ?: return null

        return code.takeIf {
            it.length in 16..64 &&
                it.all { character ->
                    character.isLetterOrDigit() ||
                        character == '-' ||
                        character == '_'
                }
        }
    }
}
