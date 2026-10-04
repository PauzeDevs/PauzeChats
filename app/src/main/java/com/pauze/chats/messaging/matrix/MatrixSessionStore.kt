/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import android.content.Context
import android.util.Base64
import com.pauze.chats.security.AndroidKeyStore
import org.json.JSONObject
import org.matrix.rustcomponents.sdk.Session
import org.matrix.rustcomponents.sdk.SlidingSyncVersion
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class MatrixSessionStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(session: Session) {
        val plainText = JSONObject()
            .put("version", STORAGE_VERSION)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("userId", session.userId)
            .put("deviceId", session.deviceId)
            .put("homeserverUrl", session.homeserverUrl)
            .put("oauthData", session.oauthData)
            .put("slidingSyncVersion", session.slidingSyncVersion.name)
            .toString()

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, AndroidKeyStore.getOrCreateLocalKey())
        }

        val ciphertext = cipher.doFinal(
            plainText.toByteArray(StandardCharsets.UTF_8)
        )

        val payload = JSONObject()
            .put("version", STORAGE_VERSION)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))

        preferences.edit()
            .putString(KEY_SESSION, payload.toString())
            .apply()
    }

    fun read(): Session? {
        val raw = preferences.getString(KEY_SESSION, null) ?: return null

        return runCatching {
            val payload = JSONObject(raw)
            require(payload.getInt("version") == STORAGE_VERSION)

            val iv = Base64.decode(payload.getString("iv"), Base64.NO_WRAP)
            val ciphertext =
                Base64.decode(payload.getString("ciphertext"), Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(
                    Cipher.DECRYPT_MODE,
                    AndroidKeyStore.getOrCreateLocalKey(),
                    GCMParameterSpec(128, iv)
                )
            }

            val session = JSONObject(
                String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            )

            Session(
                accessToken = session.getString("accessToken"),
                refreshToken = session.optString("refreshToken").ifBlank { null },
                userId = session.getString("userId"),
                deviceId = session.getString("deviceId"),
                homeserverUrl = session.getString("homeserverUrl"),
                oauthData = session.optString("oauthData").ifBlank { null },
                slidingSyncVersion = when (
                    session.getString("slidingSyncVersion")
                ) {
                    SlidingSyncVersion.NATIVE.name -> SlidingSyncVersion.NATIVE
                    SlidingSyncVersion.NONE.name -> SlidingSyncVersion.NONE
                    else -> error("Unsupported Matrix sliding sync version")
                }
            )
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().remove(KEY_SESSION).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pauze_matrix_session"
        const val KEY_SESSION = "session"
        const val STORAGE_VERSION = 1
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
