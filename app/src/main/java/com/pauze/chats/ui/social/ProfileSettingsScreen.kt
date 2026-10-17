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
import com.pauze.chats.messaging.DeviceVerificationFlowState
import com.pauze.chats.messaging.DeviceVerificationStage
import com.pauze.chats.messaging.DeviceVerificationStatus
import com.pauze.chats.messaging.SasDisplayMode

@Composable
fun ProfileSettingsScreen(
    state: SocialUiState,
    verificationState: DeviceVerificationFlowState,
    onSave: (displayName: String, bio: String) -> Unit,
    onRequestVerification: () -> Unit,
    onAcceptVerification: () -> Unit,
    onStartSasVerification: () -> Unit,
    onConfirmSasMatches: () -> Unit,
    onDeclineVerification: () -> Unit,
    onCancelVerification: () -> Unit,
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

        DeviceVerificationCard(
            state = verificationState,
            onRequestVerification = onRequestVerification,
            onAcceptVerification = onAcceptVerification,
            onStartSasVerification = onStartSasVerification,
            onConfirmSasMatches = onConfirmSasMatches,
            onDeclineVerification = onDeclineVerification,
            onCancelVerification = onCancelVerification
        )

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

@Composable
private fun DeviceVerificationCard(
    state: DeviceVerificationFlowState,
    onRequestVerification: () -> Unit,
    onAcceptVerification: () -> Unit,
    onStartSasVerification: () -> Unit,
    onConfirmSasMatches: () -> Unit,
    onDeclineVerification: () -> Unit,
    onCancelVerification: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Device security", style = MaterialTheme.typography.titleMedium)
            Text(
                text = when (state.deviceStatus) {
                    DeviceVerificationStatus.VERIFIED -> "Matrix marks this device as verified."
                    DeviceVerificationStatus.UNVERIFIED -> "This device is not verified yet."
                    DeviceVerificationStatus.UNKNOWN -> "Matrix verification status is unavailable."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            when (state.stage) {
                DeviceVerificationStage.INCOMING_REQUEST -> {
                    Text(
                        "${state.otherDeviceName ?: "Another device"} is asking to verify. " +
                            "Accept only if you initiated this on that device."
                    )
                    Button(onClick = onAcceptVerification) { Text("Accept request") }
                    TextButton(onClick = onDeclineVerification) { Text("Decline request") }
                }
                DeviceVerificationStage.READY_TO_START_SAS -> {
                    Text("The other device accepted. Start the security code comparison.")
                    Button(onClick = onStartSasVerification) { Text("Start comparison") }
                    TextButton(onClick = onCancelVerification) { Text("Cancel") }
                }
                DeviceVerificationStage.SAS_COMPARISON -> {
                    Text("Compare every value with the other device.")
                    Text(
                        text = state.sasValues.joinToString(
                            separator = if (state.sasMode == SasDisplayMode.EMOJI) "   " else "  ·  "
                        ),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Button(
                        onClick = onConfirmSasMatches,
                        enabled = state.sasValues.isNotEmpty()
                    ) {
                        Text("The values match — verify")
                    }
                    TextButton(onClick = onCancelVerification) {
                        Text("They don't match — cancel")
                    }
                }
                DeviceVerificationStage.REQUESTED,
                DeviceVerificationStage.WAITING_FOR_SAS,
                DeviceVerificationStage.WAITING_FOR_CONFIRMATION -> {
                    Text(
                        when (state.stage) {
                            DeviceVerificationStage.REQUESTED ->
                                "Waiting for you to accept on your other device."
                            DeviceVerificationStage.WAITING_FOR_SAS ->
                                "Waiting for the security comparison to become available."
                            else -> "Waiting for the other device to confirm."
                        }
                    )
                    TextButton(onClick = onCancelVerification) { Text("Cancel verification") }
                }
                else -> {
                    if (state.stage == DeviceVerificationStage.COMPLETED &&
                        state.deviceStatus != DeviceVerificationStatus.VERIFIED
                    ) {
                        Text("The flow ended, but Matrix has not marked this device verified.")
                    }
                    if (state.deviceStatus != DeviceVerificationStatus.VERIFIED) {
                        Button(
                            onClick = onRequestVerification,
                            enabled = state.isReady
                        ) {
                            Text("Verify another session")
                        }
                        if (!state.isReady) {
                            Text("Connect messaging to check device verification.")
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
