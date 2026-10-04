/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import com.pauze.chats.BuildConfig
import com.pauze.chats.account.AuthApiException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class MatrixBootstrapToken(
    val token: String,
    val expiresInSeconds: Long
)

class MatrixAuthApiClient(
    private val baseUrl: String = BuildConfig.PAUZE_API_BASE_URL,
    private val client: OkHttpClient = OkHttpClient()
) {
    init {
        require(baseUrl.isBlank() || baseUrl.startsWith("https://")) {
            "PauzeChats API must use HTTPS"
        }
    }

    fun requestBootstrapToken(accessToken: String): MatrixBootstrapToken {
        val body = execute(
            Request.Builder()
                .url(url("/v1/messaging/matrix-token"))
                .post(
                    okhttp3.RequestBody.create(
                        "application/json; charset=utf-8".toMediaType(),
                        "{}"
                    )
                )
                .header("Authorization", "Bearer " + accessToken)
                .build()
        )

        val json = JSONObject(body)
        val token = json.optString("token")
        val expiresInSeconds = json.optLong("expiresInSeconds", -1L)

        require(token.isNotBlank()) {
            "Matrix bootstrap token response is missing token"
        }
        require(expiresInSeconds > 0L) {
            "Matrix bootstrap token response is missing expiry"
        }

        return MatrixBootstrapToken(
            token = token,
            expiresInSeconds = expiresInSeconds
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
}
