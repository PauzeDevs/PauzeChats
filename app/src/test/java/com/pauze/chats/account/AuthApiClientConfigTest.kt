/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

package com.pauze.chats.account

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthApiClientConfigTest {
    @Test
    fun blankApiUrlIsReportedAsUnconfigured() {
        assertFalse(AuthApiClient("").isConfigured)
    }

    @Test
    fun normalizesHttpsApiBaseUrl() {
        assertEquals(
            "https://api.example.com/pauze",
            normalizeApiBaseUrl("  https://api.example.com/pauze/  ")
        )
        assertTrue(AuthApiClient("https://api.example.com").isConfigured)
    }

    @Test
    fun rejectsInsecureOrAmbiguousApiUrls() {
        listOf(
            "http://api.example.com",
            "https://user:password@api.example.com",
            "https://api.example.com?access_token=secret",
            "https://api.example.com#fragment",
            "https://"
        ).forEach { url ->
            assertFailsWith<IllegalArgumentException> {
                normalizeApiBaseUrl(url)
            }
        }
    }
}
