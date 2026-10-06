package com.pauze.chats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pauze.chats.PauzeChatsApplication
import com.pauze.chats.ui.auth.AuthScreen
import com.pauze.chats.ui.chat.ChatsScreen
import com.pauze.chats.ui.chat.ChatsViewModel
import com.pauze.chats.ui.auth.AuthViewModel
import com.pauze.chats.ui.social.FriendsScreen
import com.pauze.chats.ui.social.ProfileSettingsScreen
import com.pauze.chats.ui.social.SocialViewModel
import com.pauze.chats.ui.theme.PauzeChatsTheme

@Composable
fun PauzeChatsApp(
    initialInviteCode: String? = null
) {
    val application =
        LocalContext.current.applicationContext as PauzeChatsApplication

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(application.authRepository)
    )
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    PauzeChatsTheme {
        if (!authState.isAuthenticated) {
            AuthScreen(
                state = authState,
                initialInviteCode = initialInviteCode,
                onModeChange = authViewModel::setMode,
                onSignIn = authViewModel::signIn,
                onSignUp = authViewModel::signUp
            )
        } else {
            AuthenticatedApp(
                authViewModel = authViewModel,
                application = application
            )
        }
    }
}

@Composable
private fun AuthenticatedApp(
    authViewModel: AuthViewModel,
    application: PauzeChatsApplication
) {
    val chatsViewModel: ChatsViewModel = viewModel(
        factory = ChatsViewModel.Factory(
            application.authRepository,
            application.messagingSession,
            application.conversationRepository
        )
    )
    val chatsState by chatsViewModel.state.collectAsStateWithLifecycle()

    val socialViewModel: SocialViewModel = viewModel(
        factory = SocialViewModel.Factory(application.socialRepository)
    )
    val socialState by socialViewModel.state.collectAsStateWithLifecycle()

    AuthenticatedNavigation(
        chatsState = chatsState,
        onRefreshChats = chatsViewModel::refresh,
        socialState = socialState,
        onRefreshSocial = socialViewModel::refresh,
        onLookup = socialViewModel::lookup,
        onSendFriendRequest = socialViewModel::sendFriendRequest,
        onRespondToRequest = socialViewModel::respondToRequest,
        onSaveProfile = socialViewModel::saveProfile,
        onSignOut = authViewModel::signOut
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedNavigation(
    chatsState: com.pauze.chats.ui.chat.ChatsUiState,
    onRefreshChats: () -> Unit,
    socialState: com.pauze.chats.ui.social.SocialUiState,
    onRefreshSocial: () -> Unit,
    onLookup: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onRespondToRequest: (String, String) -> Unit,
    onSaveProfile: (String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PauzeChats")
                        Text(
                            text = "Private alpha",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("C") },
                    label = { Text("Chats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("F") },
                    label = { Text("Friends") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("S") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            0 -> ChatsScreen(
                state = chatsState,
                onRefresh = onRefreshChats,
                modifier = Modifier.padding(paddingValues)
            )
            1 -> FriendsScreen(
                state = socialState,
                onLookup = onLookup,
                onSendFriendRequest = onSendFriendRequest,
                onRespondToRequest = onRespondToRequest,
                onRefresh = onRefreshSocial
            )
            else -> ProfileSettingsScreen(
                state = socialState,
                onSave = onSaveProfile,
                onSignOut = onSignOut
            )
        }
    }
}

@Composable
private fun ChatsFoundationScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Private chats",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                "Personal and group conversations will live here once messaging foundations are ready.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
