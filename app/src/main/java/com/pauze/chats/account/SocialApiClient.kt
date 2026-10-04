package com.pauze.chats.account

import com.pauze.chats.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.net.URLEncoder

class SocialApiClient(
    private val baseUrl: String = BuildConfig.PAUZE_API_BASE_URL,
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    init {
        require(baseUrl.isBlank() || baseUrl.startsWith("https://")) {
            "PauzeChats API must use HTTPS"
        }
    }

    fun me(accessToken: String): UserProfile {
        val json = JSONObject(
            execute(
                Request.Builder()
                    .url(url("/v1/me"))
                    .get()
                    .header("Authorization", "Bearer " + accessToken)
                    .build()
            )
        )
        return parseProfile(json)
    }

    fun updateProfile(
        accessToken: String,
        displayName: String,
        bio: String
    ): UserProfile {
        val json = JSONObject(
            execute(
                Request.Builder()
                    .url(url("/v1/me/profile"))
                    .patch(
                        JSONObject()
                            .put("displayName", displayName)
                            .put("bio", bio)
                            .toString()
                            .toRequestBody(jsonMediaType)
                    )
                    .header("Authorization", "Bearer " + accessToken)
                    .build()
            )
        )
        return parseProfile(json)
    }

    fun lookupUsername(
        accessToken: String,
        username: String
    ): FriendProfile {
        val encodedUsername = URLEncoder.encode(username, "UTF-8")
        val targetUrl = url("/v1/users/lookup") + "?username=" + encodedUsername

        return parseFriendProfile(
            JSONObject(
                execute(
                    Request.Builder()
                        .url(targetUrl)
                        .get()
                        .header("Authorization", "Bearer " + accessToken)
                        .build()
                )
            )
        )
    }

    fun sendFriendRequest(
        accessToken: String,
        username: String
    ) {
        execute(
            Request.Builder()
                .url(url("/v1/friends/requests"))
                .post(
                    JSONObject()
                        .put("username", username)
                        .toString()
                        .toRequestBody(jsonMediaType)
                )
                .header("Authorization", "Bearer " + accessToken)
                .build()
        )
    }

    fun incomingRequests(accessToken: String): List<PendingFriendRequest> =
        parseRequests(
            executeList(accessToken, "/v1/friends/requests/incoming")
        )

    fun outgoingRequests(accessToken: String): List<PendingFriendRequest> =
        parseRequests(
            executeList(accessToken, "/v1/friends/requests/outgoing")
        )

    fun respondToRequest(
        accessToken: String,
        requestId: String,
        action: String
    ) {
        execute(
            Request.Builder()
                .url(url("/v1/friends/requests/" + requestId + "/" + action))
                .post("{}".toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer " + accessToken)
                .build()
        )
    }

    fun friends(accessToken: String): List<FriendProfile> =
        parseProfiles(executeList(accessToken, "/v1/friends"))

    private fun executeList(
        accessToken: String,
        path: String
    ): String =
        execute(
            Request.Builder()
                .url(url(path))
                .get()
                .header("Authorization", "Bearer " + accessToken)
                .build()
        )

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val errorCode = runCatching {
                    JSONObject(body).optString("error")
                }.getOrNull().orEmpty().ifBlank {
                    "request_failed"
                }

                throw AuthApiException(errorCode, response.code)
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

    private fun parseProfile(json: JSONObject): UserProfile =
        UserProfile(
            userId = UserId(json.getString("userId")),
            username = json.getString("username"),
            displayName = json.getString("displayName"),
            bio = json.getString("bio"),
            avatarMimeType = json.optString("avatarMimeType").ifBlank { null }
        )

    private fun parseFriendProfile(json: JSONObject): FriendProfile =
        FriendProfile(
            userId = UserId(json.getString("userId")),
            username = json.getString("username"),
            displayName = json.getString("displayName"),
            bio = json.getString("bio"),
            avatarMimeType = json.optString("avatarMimeType").ifBlank { null }
        )

    private fun parseProfiles(body: String): List<FriendProfile> {
        val json = JSONArray(body)
        return buildList(json.length()) {
            for (index in 0 until json.length()) {
                add(parseFriendProfile(json.getJSONObject(index)))
            }
        }
    }

    private fun parseRequests(body: String): List<PendingFriendRequest> {
        val json = JSONArray(body)
        return buildList(json.length()) {
            for (index in 0 until json.length()) {
                val item = json.getJSONObject(index)
                add(
                    PendingFriendRequest(
                        id = FriendRequestId(item.getString("id")),
                        status = FriendRequestStatus.valueOf(item.getString("status")),
                        createdAtEpochSeconds =
                            Instant.parse(item.getString("createdAt")).epochSecond,
                        user = parseFriendProfile(item.getJSONObject("user"))
                    )
                )
            }
        }
    }
}
