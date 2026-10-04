package com.pauze.chats.account

import com.pauze.chats.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class AuthApiException(
    val errorCode: String,
    val httpStatus: Int,
    val retryAfterSeconds: Long? = null
) : IOException(errorCode)

class AuthApiClient(
    private val baseUrl: String = BuildConfig.PAUZE_API_BASE_URL,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        require(baseUrl.isBlank() || baseUrl.startsWith("https://")) {
            "PauzeChats API must use HTTPS"
        }
    }

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
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorCode = runCatching {
                    JSONObject(body).optString("error")
                }.getOrNull().orEmpty().ifBlank {
                    "request_failed"
                }

                val retryAfterSeconds =
                    response.header("Retry-After")
                        ?.toLongOrNull()
                        ?.takeIf { it > 0 }

                throw AuthApiException(
                    errorCode = errorCode,
                    httpStatus = response.code,
                    retryAfterSeconds = retryAfterSeconds
                )
            }

            return body
        }
    }

    private fun url(path: String): String {
        if (baseUrl.isBlank()) {
            throw AuthApiException("api_not_configured", 503)
        }

        return baseUrl.trimEnd('/') + path
    }

    data class AuthResult(
        val session: Session,
        val profile: UserProfile
    )
}
