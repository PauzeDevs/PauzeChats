/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.messaging.matrix

import org.matrix.rustcomponents.sdk.LogLevel
import org.matrix.rustcomponents.sdk.TracingConfiguration
import org.matrix.rustcomponents.sdk.initPlatform

object MatrixSdkPlatform {
    fun initialize() {
        initPlatform(
            config = TracingConfiguration(
                logLevel = LogLevel.WARN,
                traceLogPacks = emptyList(),
                extraTargets = emptyList(),
                writeToStdoutOrSystem = true,
                writeToFiles = null,
                sentryConfig = null,
            ),
            useLightweightTokioRuntime = false,
        )
    }
}
