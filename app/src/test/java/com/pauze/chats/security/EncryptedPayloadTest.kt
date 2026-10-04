package com.pauze.chats.security

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptedPayloadTest {
    @Test
    fun payload_has_no_plaintext_message_field() {
        val fields = EncryptedPayload::class.java.declaredFields.map { it.name }.toSet()

        assertTrue("ciphertextBase64" in fields)
        assertFalse("plaintext" in fields)
    }
}
