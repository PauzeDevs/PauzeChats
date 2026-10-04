package com.pauze.chats.ui.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pauze.chats.account.FriendProfile
import com.pauze.chats.account.PendingFriendRequest

@Composable
fun FriendsScreen(
    state: SocialUiState,
    onLookup: (String) -> Unit,
    onSendFriendRequest: (String) -> Unit,
    onRespondToRequest: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    var username by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Friends",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 20.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Username") },
                    singleLine = true
                )
                Button(
                    onClick = { onLookup(username) },
                    enabled = username.isNotBlank()
                ) {
                    Text("Find")
                }
            }
        }

        state.lookupResult?.let { profile ->
            item {
                UserResultCard(
                    profile = profile,
                    onAdd = { onSendFriendRequest(profile.username) }
                )
            }
        }

        state.message?.let { message ->
            item {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Text(
                text = "Incoming requests",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.incomingRequests.isEmpty()) {
            item {
                Text(
                    text = "No pending requests.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(
                items = state.incomingRequests,
                key = { it.id.value }
            ) { request ->
                IncomingRequestCard(
                    request = request,
                    onAccept = {
                        onRespondToRequest(request.id.value, "accept")
                    },
                    onDecline = {
                        onRespondToRequest(request.id.value, "decline")
                    }
                )
            }
        }

        item {
            Text(
                text = "Your friends",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.friends.isEmpty()) {
            item {
                Text(
                    text = "Add someone by username to get started.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(
                items = state.friends,
                key = { it.userId.value }
            ) { friend ->
                FriendCard(friend)
            }
        }

        item {
            OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh")
            }
        }
    }
}

@Composable
private fun UserResultCard(
    profile: FriendProfile,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "@" + profile.username,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Button(onClick = onAdd) {
                Text("Add")
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: PendingFriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = request.user.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "@" + request.user.username,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(onClick = onAccept) {
                    Text("Accept")
                }
                OutlinedButton(onClick = onDecline) {
                    Text("Decline")
                }
            }
        }
    }
}

@Composable
private fun FriendCard(friend: FriendProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = friend.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "@" + friend.username,
                style = MaterialTheme.typography.bodyMedium
            )
            if (friend.bio.isNotBlank()) {
                Text(
                    text = friend.bio,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
