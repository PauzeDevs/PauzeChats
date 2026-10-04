const MAX_EMAIL_LENGTH = 254;
const MIN_PASSWORD_LENGTH = 12;
const MAX_PASSWORD_LENGTH = 128;
const MIN_USERNAME_LENGTH = 3;
const MAX_USERNAME_LENGTH = 24;
const MAX_DISPLAY_NAME_LENGTH = 40;
const MAX_BIO_LENGTH = 200;

const emailPattern =
    /^[A-Za-z0-9.!#\$%&'*+/=?^_{|}~-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+$/;
const usernamePattern = /^[a-z0-9_]+$/;

export function normalizeEmail(value) {
    if (typeof value !== "string") return null;
    const normalized = value.trim().toLowerCase();
    return normalized.length >= 3 &&
        normalized.length <= MAX_EMAIL_LENGTH &&
        emailPattern.test(normalized)
        ? normalized
        : null;
}

export function normalizeUsername(value) {
    if (typeof value !== "string") return null;
    const normalized = value.trim().toLowerCase();
    return normalized.length >= MIN_USERNAME_LENGTH &&
        normalized.length <= MAX_USERNAME_LENGTH &&
        usernamePattern.test(normalized)
        ? normalized
        : null;
}

export function validatePassword(value) {
    return typeof value === "string" &&
        value.length >= MIN_PASSWORD_LENGTH &&
        value.length <= MAX_PASSWORD_LENGTH &&
        /[A-Za-z]/.test(value) &&
        /[0-9]/.test(value);
}

export function normalizeProfileInput(displayName, bio) {
    const normalizedDisplayName =
        typeof displayName === "string" ? displayName.trim() : "";
    const normalizedBio =
        typeof bio === "string" ? bio.trim() : "";

    if (normalizedDisplayName.length < 1 ||
        normalizedDisplayName.length > MAX_DISPLAY_NAME_LENGTH) {
        return null;
    }

    if (normalizedBio.length > MAX_BIO_LENGTH) {
        return null;
    }

    return {
        displayName: normalizedDisplayName,
        bio: normalizedBio
    };
}
