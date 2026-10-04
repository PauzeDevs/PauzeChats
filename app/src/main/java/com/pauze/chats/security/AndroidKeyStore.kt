package com.pauze.chats.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object AndroidKeyStore {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "pauze_local_storage_v1"

    fun getOrCreateLocalKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply {
            load(null)
        }

        val existing = keyStore.getKey(ALIAS, null)
        if (existing is SecretKey) {
            return existing
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            PROVIDER
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )

        return generator.generateKey()
    }
}
