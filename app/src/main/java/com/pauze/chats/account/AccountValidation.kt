package com.pauze.chats.account

private const val MIN_PASSWORD_LENGTH = 12
private const val MAX_PASSWORD_LENGTH = 128
private const val MIN_USERNAME_LENGTH = 3
private const val MAX_USERNAME_LENGTH = 24
private const val MAX_DISPLAY_NAME_LENGTH = 40
private const val MAX_BIO_LENGTH = 200
private const val MAX_EMAIL_LENGTH = 254

private val emailPattern =
    Regex("^[A-Za-z0-9.!#\$%&'*+/=?^_{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+\$")

private val usernamePattern = Regex("^[a-z0-9_]+\$")

object AccountValidation {
    fun normalizeEmail(email: String): String? {
        val normalized = email.trim().lowercase()
        return normalized.takeIf {
            normalized.length in 3..MAX_EMAIL_LENGTH &&
                emailPattern.matches(normalized)
        }
    }

    fun normalizeUsername(username: String): String? {
        val normalized = username.trim().lowercase()
        return normalized.takeIf {
            normalized.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH &&
                usernamePattern.matches(normalized)
        }
    }

    fun validatePassword(password: String): Boolean =
        password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH &&
            password.any(Char::isLetter) &&
            password.any(Char::isDigit)

    fun validateDisplayName(displayName: String?): Boolean =
        displayName == null ||
            displayName.trim().length in 1..MAX_DISPLAY_NAME_LENGTH

    fun validateBio(bio: String?): Boolean =
        bio == null || bio.length <= MAX_BIO_LENGTH
}
