package com.pauze.chats.account

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountValidationTest {
    @Test
    fun email_is_normalized_and_validated() {
        assertEquals("test@example.com", AccountValidation.normalizeEmail(" Test@Example.com "))
        assertNull(AccountValidation.normalizeEmail("not-an-email"))
    }

    @Test
    fun username_is_lowercase_and_ascii() {
        assertEquals("pauze_dev", AccountValidation.normalizeUsername("Pauze_Dev"))
        assertNull(AccountValidation.normalizeUsername("Pauze Dev"))
        assertNull(AccountValidation.normalizeUsername("ab"))
    }

    @Test
    fun password_requires_length_and_basic_complexity() {
        assertFalse(AccountValidation.validatePassword("short1A"))
        assertFalse(AccountValidation.validatePassword("longenoughpassword"))
        assertFalse(AccountValidation.validatePassword("123456789012"))
        assertTrue(AccountValidation.validatePassword("longEnough123"))
    }

    @Test
    fun optional_profile_fields_are_bounded() {
        assertTrue(AccountValidation.validateDisplayName(null))
        assertTrue(AccountValidation.validateDisplayName("Pauze"))
        assertFalse(AccountValidation.validateDisplayName("x".repeat(41)))

        assertTrue(AccountValidation.validateBio(null))
        assertTrue(AccountValidation.validateBio("hello"))
        assertFalse(AccountValidation.validateBio("x".repeat(201)))
    }
}
