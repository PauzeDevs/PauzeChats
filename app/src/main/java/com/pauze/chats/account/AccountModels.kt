package com.pauze.chats.account

@JvmInline
value class UserId(val value: String)

@JvmInline
value class FriendRequestId(val value: String)

data class AccountCredentials(
    val email: String,
    val password: String
)

data class RegistrationRequest(
    val email: String,
    val password: String,
    val username: String,
    val displayName: String? = null,
    val bio: String? = null
)

data class UserProfile(
    val userId: UserId,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarMimeType: String?
)

data class Session(
    val userId: UserId,
    val sessionId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    CANCELLED,
    BLOCKED
}

data class FriendRequest(
    val id: FriendRequestId,
    val fromUserId: UserId,
    val toUserId: UserId,
    val status: FriendRequestStatus,
    val createdAtEpochSeconds: Long
)

interface AuthRepository {
    suspend fun signIn(credentials: AccountCredentials): Result<Session>

    suspend fun signUp(request: RegistrationRequest): Result<Session>

    suspend fun signOut(): Result<Unit>

    suspend fun currentSession(): Session?
}
data class FriendProfile(
    val userId: UserId,
    val username: String,
    val displayName: String,
    val bio: String,
    val avatarMimeType: String?
)

data class PendingFriendRequest(
    val id: FriendRequestId,
    val status: FriendRequestStatus,
    val createdAtEpochSeconds: Long,
    val user: FriendProfile
)

