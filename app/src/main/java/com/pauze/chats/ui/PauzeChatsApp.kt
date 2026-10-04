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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pauze.chats.ui.theme.PauzeChatsTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PauzeChatsApp() {
    PauzeChatsTheme {
        var selectedTab by remember { mutableIntStateOf(0) }

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
                        label = { Text("Settings") }
                    )
                }
            }
        ) { paddingValues ->
            when (selectedTab) {
                0 -> ChatsFoundationScreen(Modifier.padding(paddingValues))
                1 -> FriendsFoundationScreen(Modifier.padding(paddingValues))
                else -> SettingsFoundationScreen(Modifier.padding(paddingValues))
            }
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
                "Personal and group conversations will live here once account and messaging foundations are ready.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FriendsFoundationScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Friends",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                "Add people by their unique username.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsFoundationScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.padding(4.dp))
            Text(
                "Account, privacy, notifications, sessions, and presence controls will live here.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
