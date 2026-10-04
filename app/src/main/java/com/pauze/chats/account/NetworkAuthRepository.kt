package com.pauze.chats.account

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkAuthRepository(
    context: Context,
    private val api: AuthApiClient = AuthApiClient()
) : AuthRepository {
    private val sessionStore =
        EncryptedSessionStore(context.applicationContext)

    private var inMemorySession: Session? = null

    override suspend fun signIn(
        credentials: AccountCredentials
    ): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            val result = api.signIn(credentials)
            persist(result.session)
            result.session
        }
    }

    override suspend fun signUp(
        request: RegistrationRequest
    ): Result<Session> =
        Result.failure(
            IllegalArgumentException("Invite code is required")
        )

    suspend fun signUp(
        request: RegistrationRequest,
        inviteCode: String
    ): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            val result = api.signUp(request, inviteCode)
            persist(result.session)
            result.session
        }
    }

    override suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            inMemorySession?.accessToken?.let { api.signOut(it) }
        }.also {
            inMemorySession = null
            sessionStore.clear()
        }
    }

    override suspend fun currentSession(): Session? = withContext(Dispatchers.IO) {
        inMemorySession?.let { return@withContext it }

        val refreshToken = sessionStore.readRefreshToken()
            ?: return@withContext null

        runCatching {
            val result = api.refresh(refreshToken)
            persist(result.session)
            result.session
        }.getOrElse {
            sessionStore.clear()
            null
        }
    }

    private fun persist(session: Session) {
        inMemorySession = session
        sessionStore.saveRefreshToken(session.refreshToken)
    }
}
