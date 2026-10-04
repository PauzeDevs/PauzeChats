/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

import { SignJWT } from "jose";

const MATRIX_TOKEN_TTL_SECONDS = 60;

function requireSecret(secret) {
    if (typeof secret !== "string" || secret.length < 32) {
        throw new Error("MATRIX_JWT_SECRET must be at least 32 characters");
    }
    return new TextEncoder().encode(secret);
}

export function createMatrixTokenService({
    matrixJwtSecret,
    issuer,
    audience,
    ttlSeconds = MATRIX_TOKEN_TTL_SECONDS
}) {
    if (typeof issuer !== "string" || issuer.length < 1) {
        throw new Error("MATRIX_JWT_ISSUER is required");
    }
    if (typeof audience !== "string" || audience.length < 1) {
        throw new Error("MATRIX_JWT_AUDIENCE is required");
    }
    if (!Number.isInteger(ttlSeconds) || ttlSeconds < 30 || ttlSeconds > 300) {
        throw new Error("Matrix JWT TTL must be between 30 and 300 seconds");
    }

    const jwtKey = requireSecret(matrixJwtSecret);

    async function issueToken({ username, displayName }) {
        if (typeof username !== "string" || !/^[a-z0-9_]+$/.test(username)) {
            throw new Error("Invalid Matrix username");
        }

        const now = Math.floor(Date.now() / 1000);

        return new SignJWT({
            display_name: displayName
        })
            .setProtectedHeader({ alg: "HS256" })
            .setSubject(username)
            .setIssuer(issuer)
            .setAudience(audience)
            .setIssuedAt(now)
            .setExpirationTime(now + ttlSeconds)
            .setJti(crypto.randomUUID())
            .sign(jwtKey);
    }

    return {
        issueToken,
        ttlSeconds
    };
}
