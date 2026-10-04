package com.pauze.chats.messaging.matrix

import android.content.Context
import java.io.File
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
            "matrix/$accountId"
        ).apply {
            mkdirs()
        }

        val dataPath = File(accountRoot, "data").apply {
            mkdirs()
        }
        val cachePath = File(accountRoot, "cache").apply {
            mkdirs()
        }

        ClientBuilder()
            .sessionPaths(
                dataPath = dataPath.absolutePath,
                cachePath = cachePath.absolutePath
            )
            .homeserverUrl(homeserverUrl)
            .slidingSyncVersionBuilder(SlidingSyncVersionBuilder.NATIVE)
            .build()
    }
}
