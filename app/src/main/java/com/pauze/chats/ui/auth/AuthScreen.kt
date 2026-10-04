package com.pauze.chats.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun AuthScreen(
    state: AuthUiState,
    onModeChange: (AuthMode) -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (
        email: String,
        password: String,
        username: String,
        displayName: String,
        bio: String,
        inviteCode: String
    ) -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var inviteCode by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PauzeChats",
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Private chats. Just your people.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.mode == AuthMode.SIGN_IN) {
                Button(
                    onClick = { onModeChange(AuthMode.SIGN_IN) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign in")
                }
                OutlinedButton(
                    onClick = { onModeChange(AuthMode.SIGN_UP) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create account")
                }
            } else {
                OutlinedButton(
                    onClick = { onModeChange(AuthMode.SIGN_IN) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sign in")
                }
                Button(
                    onClick = { onModeChange(AuthMode.SIGN_UP) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Create account")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (state.mode == AuthMode.SIGN_UP) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username") },
                supportingText = { Text("3–24 lowercase letters, numbers, or _") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Display name") },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it.take(200) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bio") },
                minLines = 3,
                maxLines = 4
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = inviteCode,
                onValueChange = { inviteCode = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Invite code") },
                supportingText = { Text("PauzeChats is invite-only.") },
                singleLine = true
            )
        }

        state.errorMessage?.let { message ->
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (state.mode == AuthMode.SIGN_IN) {
                    onSignIn(email, password)
                } else {
                    onSignUp(
                        email,
                        password,
                        username,
                        displayName,
                        bio,
                        inviteCode
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isBusy &&
                email.isNotBlank() &&
                password.isNotBlank() &&
                (state.mode == AuthMode.SIGN_IN ||
                    (username.isNotBlank() && inviteCode.isNotBlank()))
        ) {
            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(vertical = 2.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (state.mode == AuthMode.SIGN_IN) {
                        "Sign in"
                    } else {
                        "Create account"
                    }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (state.mode == AuthMode.SIGN_IN) {
            TextButton(
                onClick = { },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = false
            ) {
                Text("Password recovery coming next")
            }
        }
    }
}
