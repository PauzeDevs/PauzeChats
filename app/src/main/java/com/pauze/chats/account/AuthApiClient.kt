/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.account

import com.pauze.chats.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class AuthApiException(
    val errorCode: String,
    val httpStatus: Int,
    val retryAfterSeconds: Long? = null
) : IOException(errorCode)

internal data class AuthHttpResponse(
    val statusCode: Int,
    val body: String,
    val retryAfter: String? = null
)

internal fun interface AuthRequestExecutor {
    fun execute(request: Request): AuthHttpResponse
}

class AuthApiClient private constructor(
    baseUrl: String,
    private val client: OkHttpClient,
    private val requestExecutor: AuthRequestExecutor?
) {
    constructor(
        baseUrl: String = BuildConfig.PAUZE_API_BASE_URL,
        client: OkHttpClient = OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    ) : this(baseUrl, client, null)

    internal constructor(
        baseUrl: String,
        requestExecutor: AuthRequestExecutor
    ) : this(baseUrl, OkHttpClient(), requestExecutor)

    private val normalizedBaseUrl = normalizeApiBaseUrl(baseUrl)
    val isConfigured: Boolean
        get() = normalizedBaseUrl.isNotBlank()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun signIn(credentials: AccountCredentials): AuthResult =
        post(
            "/v1/auth/sign-in",
            JSONObject()
                .put("email", credentials.email)
                .put("password", credentials.password)
        )

    fun signUp(request: RegistrationRequest, inviteCode: String): AuthResult =
        post(
            "/v1/auth/register",
            JSONObject()
                .put("email", request.email)
                .put("password", request.password)
                .put("username", request.username)
                .put("displayName", request.displayName ?: "")
                .put("bio", request.bio ?: "")
                .put("inviteCode", inviteCode)
        )

    fun refresh(refreshToken: String): AuthResult =
        post(
            "/v1/auth/refresh",
            JSONObject().put("refreshToken", refreshToken)
        )

    fun signOut(accessToken: String) {
        execute(
            Request.Builder()
                .url(url("/v1/auth/sign-out"))
                .post("{}".toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $accessToken")
                .build()
        )
    }

    fun me(accessToken: String): UserProfile {
        val payload = JSONObject(
            execute(
                Request.Builder()
                    .url(url("/v1/me"))
                    .get()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            )
        )

        return UserProfile(
            userId = UserId(payload.getString("userId")),
            username = payload.getString("username"),
            displayName = payload.getString("displayName"),
            bio = payload.getString("bio"),
            avatarMimeType = payload.optString("avatarMimeType").ifBlank { null }
        )
    }

    private fun post(path: String, payload: JSONObject): AuthResult {
        val response = JSONObject(
            execute(
                Request.Builder()
                    .url(url(path))
                    .post(payload.toString().toRequestBody(jsonMediaType))
                    .build()
            )
        )

        val session = response.getJSONObject("session")
        val profile = response.getJSONObject("profile")
        val sessionId = session.getString("sessionId")
        val accessToken = session.getString("accessToken")
        val refreshToken = session.getString("refreshToken")

        return AuthResult(
            session = Session(
                userId = UserId(profile.getString("userId")),
                sessionId = sessionId,
                accessToken = accessToken,
                refreshToken = refreshToken,
                expiresAtEpochSeconds =
                    (System.currentTimeMillis() / 1000L) +
                        session.getLong("expiresInSeconds")
            ),
            profile = UserProfile(
                userId = UserId(profile.getString("userId")),
                username = profile.getString("username"),
                displayName = profile.getString("displayName"),
                bio = profile.getString("bio"),
                avatarMimeType = profile.optString("avatarMimeType").ifBlank { null }
            )
        )
    }

    private fun execute(request: Request): String {
        val result = requestExecutor?.execute(request) ?: client.newCall(request).execute().use {
            AuthHttpResponse(
                statusCode = it.code,
                body = it.body?.string().orEmpty(),
                retryAfter = it.header("Retry-After")
            )
        }

        if (result.statusCode !in 200..299) {
            val errorCode = runCatching {
                JSONObject(result.body).optString("error")
            }.getOrNull().orEmpty().ifBlank {
                "request_failed"
            }

            val retryAfterSeconds = result.retryAfter
                ?.toLongOrNull()
                ?.takeIf { it > 0 }

            throw AuthApiException(
                errorCode = errorCode,
                httpStatus = result.statusCode,
                retryAfterSeconds = retryAfterSeconds
            )
        }

        return result.body
    }

    private fun url(path: String): String {
        if (!isConfigured) {
            throw AuthApiException("api_not_configured", 503)
        }

        return normalizedBaseUrl + path
    }

    data class AuthResult(
        val session: Session,
        val profile: UserProfile
    )
}

internal fun normalizeApiBaseUrl(baseUrl: String): String {
    val normalized = baseUrl.trim().trimEnd('/')
    if (normalized.isBlank()) return ""

    val uri = runCatching { URI(normalized) }.getOrNull()
    require(
        uri != null &&
            uri.scheme?.equals("https", ignoreCase = true) == true &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null
    ) {
        "PauzeChats API base URL must be an HTTPS URL without credentials, query, or fragment"
    }

    return normalized
}
