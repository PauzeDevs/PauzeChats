package com.pauze.chats.account

import android.content.Context
import android.util.Base64
import com.pauze.chats.security.AndroidKeyStore
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class EncryptedSessionStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveRefreshToken(refreshToken: String) {
        require(refreshToken.isNotBlank())

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, AndroidKeyStore.getOrCreateLocalKey())
        }

        val ciphertext = cipher.doFinal(
            refreshToken.toByteArray(StandardCharsets.UTF_8)
        )

        val payload = JSONObject()
            .put("version", STORAGE_VERSION)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(ciphertext, Base64.NO_WRAP))

        preferences.edit()
            .putString(KEY_REFRESH_TOKEN, payload.toString())
            .apply()
    }

    fun readRefreshToken(): String? {
        val raw = preferences.getString(KEY_REFRESH_TOKEN, null) ?: return null

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

            String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "pauze_secure_session"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val STORAGE_VERSION = 1
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
