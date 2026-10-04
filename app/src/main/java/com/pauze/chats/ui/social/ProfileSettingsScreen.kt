package com.pauze.chats.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProfileSettingsScreen(
    state: SocialUiState,
    onSave: (displayName: String, bio: String) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.profile?.userId?.value) {
        state.profile?.let {
            displayName = it.displayName
            bio = it.bio
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall
        )

        state.profile?.let { profile ->
            Text(
                text = "@" + profile.username,
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                singleLine = true
            )

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 5
            )

            Button(
                onClick = { onSave(displayName, bio) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && displayName.isNotBlank()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Save profile")
                }
            }
        } ?: Text(
            text = "Loading profile…",
            style = MaterialTheme.typography.bodyMedium
        )

        state.message?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f, fill = false))

        TextButton(
            onClick = onSignOut,
            enabled = !state.isSaving
        ) {
            Text(
                text = "Sign out",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
