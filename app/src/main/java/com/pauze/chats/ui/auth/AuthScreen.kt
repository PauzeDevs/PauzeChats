package com.pauze.chats.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    state: AuthUiState,
    initialInviteCode: String? = null,
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
    var inviteCode by rememberSaveable {
        mutableStateOf(initialInviteCode.orEmpty())
    }

    LaunchedEffect(initialInviteCode) {
        if (!initialInviteCode.isNullOrBlank()) {
            onModeChange(AuthMode.SIGN_UP)
        }
    }

    val isSignIn = state.mode == AuthMode.SIGN_IN
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1020),
                        Color(0xFF090B14),
                        Color(0xFF101020)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x333E63FF), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.08f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.92f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x222A9DFF), Color.Transparent),
                    center = Offset(size.width * 0.02f, size.height * 0.72f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.02f, size.height * 0.72f)
            )

            val stars = listOf(
                0.08f to 0.10f, 0.22f to 0.17f, 0.81f to 0.13f,
                0.91f to 0.24f, 0.13f to 0.34f, 0.88f to 0.43f,
                0.05f to 0.55f, 0.75f to 0.61f, 0.21f to 0.78f,
                0.94f to 0.82f, 0.63f to 0.91f, 0.38f to 0.06f
            )
            stars.forEachIndexed { index, point ->
                drawCircle(
                    color = Color.White.copy(alpha = if (index % 3 == 0) 0.65f else 0.28f),
                    radius = if (index % 4 == 0) 2.dp.toPx() else 1.dp.toPx(),
                    center = Offset(size.width * point.first, size.height * point.second)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C83FF), Color(0xFF4D8DFF), Color(0xFF4DD8E8))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "P",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "PAUZECHATS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isSignIn) "Your people,\nyour space." else "Make it\nyour space.",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                lineHeight = 37.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Private conversations. No noise. Just your people.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            AuthModeOption(
                                text = "Sign in",
                                selected = isSignIn,
                                modifier = Modifier.weight(1f),
                                onClick = { onModeChange(AuthMode.SIGN_IN) }
                            )
                            AuthModeOption(
                                text = "Create account",
                                selected = !isSignIn,
                                modifier = Modifier.weight(1f),
                                onClick = { onModeChange(AuthMode.SIGN_UP) }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isSignIn) "Welcome back" else "Join your circle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSignIn) {
                                "Sign in to pick up where you left off."
                            } else {
                                "PauzeChats is invite-only by design."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        placeholder = { Text("you@example.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = fieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("Enter your password") },
                        singleLine = true,
                        shape = RoundedCornerShape(15.dp),
                        colors = fieldColors,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (!isSignIn) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Username") },
                            placeholder = { Text("your_username") },
                            supportingText = { Text("3–24 lowercase letters, numbers, or _") },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp),
                            colors = fieldColors
                        )

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Display name (optional)") },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp),
                            colors = fieldColors
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it.take(200) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("A little about you (optional)") },
                            placeholder = { Text("Your bio") },
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(15.dp),
                            colors = fieldColors
                        )

                        OutlinedTextField(
                            value = inviteCode,
                            onValueChange = { inviteCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Invite code") },
                            placeholder = { Text("Enter your invite code") },
                            supportingText = { Text("Only invited members can create an account.") },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp),
                            colors = fieldColors
                        )
                    }

                    state.errorMessage?.let { message ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(14.dp),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isSignIn) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !state.isBusy &&
                            email.isNotBlank() &&
                            password.isNotBlank() &&
                            (isSignIn || (username.isNotBlank() && inviteCode.isNotBlank()))
                    ) {
                        if (state.isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = if (isSignIn) "Sign in securely" else "Create account",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (isSignIn) {
                        Text(
                            text = "Your conversations stay yours.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF54D6A0))
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "PRIVATE BY DESIGN  •  INVITE ONLY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun AuthModeOption(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
