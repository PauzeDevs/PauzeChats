/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import android.content.Context
import java.io.File
import java.security.MessageDigest
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientBuilder
import org.matrix.rustcomponents.sdk.SlidingSyncVersionBuilder

class MatrixSdkClientFactory(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun build(
        accountId: String,
        homeserverUrl: String
    ): Result<Client> = runCatching {
        require(accountId.isNotBlank()) {
            "Matrix account id must not be blank"
        }
        require(homeserverUrl.startsWith("https://")) {
            "Matrix homeserver must use HTTPS"
        }

        val accountRoot = File(
            appContext.filesDir,
            "matrix/${accountKey(accountId)}"
        )
        requireDirectory(accountRoot)

        val dataPath = File(accountRoot, "data")
        requireDirectory(dataPath)

        val cachePath = File(accountRoot, "cache")
        requireDirectory(cachePath)

        ClientBuilder()
            .sessionPaths(
                dataPath = dataPath.absolutePath,
                cachePath = cachePath.absolutePath
            )
            .homeserverUrl(homeserverUrl)
            .slidingSyncVersionBuilder(SlidingSyncVersionBuilder.NATIVE)
            .build()
    }

    private fun accountKey(accountId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(accountId.toByteArray(Charsets.UTF_8))

        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    private fun requireDirectory(directory: File) {
        check(directory.isDirectory || directory.mkdirs()) {
            "Unable to create Matrix session directory: ${directory.absolutePath}"
        }
    }
}
