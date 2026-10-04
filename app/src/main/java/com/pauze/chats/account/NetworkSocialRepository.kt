package com.pauze.chats.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkSocialRepository(
    private val authRepository: AuthRepository,
    private val api: SocialApiClient = SocialApiClient()
) {
    private suspend fun accessToken(): String =
        authRepository.currentSession()?.accessToken
            ?: throw AuthApiException("not_authenticated", 401)

    suspend fun me(): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching { api.me(accessToken()) }
        }

    suspend fun updateProfile(
        displayName: String,
        bio: String
    ): Result<UserProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.updateProfile(
                    accessToken(),
                    displayName,
                    bio
                )
            }
        }

    suspend fun lookupUsername(
        username: String
    ): Result<FriendProfile> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.lookupUsername(
                    accessToken(),
                    username
                )
            }
        }

    suspend fun sendFriendRequest(
        username: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.sendFriendRequest(
                    accessToken(),
                    username
                )
            }
        }

    suspend fun incomingRequests(): Result<List<PendingFriendRequest>> =
        withContext(Dispatchers.IO) {
            runCatching { api.incomingRequests(accessToken()) }
        }

    suspend fun outgoingRequests(): Result<List<PendingFriendRequest>> =
        withContext(Dispatchers.IO) {
            runCatching { api.outgoingRequests(accessToken()) }
        }

    suspend fun respondToRequest(
        requestId: String,
        action: String
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.respondToRequest(
                    accessToken(),
                    requestId,
                    action
                )
            }
        }

    suspend fun friends(): Result<List<FriendProfile>> =
        withContext(Dispatchers.IO) {
            runCatching { api.friends(accessToken()) }
        }
}
