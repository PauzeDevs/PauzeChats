/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

import http from "node:http";
import { createAuthService, AuthError } from "./auth.js";
import { createMatrixTokenService } from "./matrix-auth.js";
import { createSocialService, SocialError } from "./social.js";
import { getPool } from "./db.js";
import { RateLimiter } from "./rate-limit.js";

const port = Number.parseInt(process.env.PORT ?? "8080", 10);
const MAX_BODY_BYTES = 64 * 1024;
const AUTH_RATE_WINDOW_MS = 15 * 60 * 1000;
const REGISTER_RATE_LIMIT = 5;
const SIGN_IN_RATE_LIMIT = 10;
const REFRESH_RATE_LIMIT = 30;
const MATRIX_TOKEN_RATE_LIMIT = 10;
const trustProxy = process.env.TRUST_PROXY === "true";

if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("PORT must be a valid TCP port");
}

function sendJson(response, status, body) {
    response.writeHead(status, {
        "content-type": "application/json; charset=utf-8",
        "cache-control": "no-store"
    });
    response.end(JSON.stringify(body));
}

async function readJsonBody(request) {
    const chunks = [];
    let size = 0;

    for await (const chunk of request) {
        size += chunk.length;
        if (size > MAX_BODY_BYTES) {
            throw new AuthError("request_too_large", 413);
        }
        chunks.push(chunk);
    }

    if (chunks.length === 0) return {};

    try {
        return JSON.parse(Buffer.concat(chunks).toString("utf8"));
    } catch {
        throw new AuthError("invalid_json", 400);
    }
}

function clientAddress(request) {
    if (trustProxy) {
        const forwarded = request.headers["x-forwarded-for"];
        if (typeof forwarded === "string" && forwarded.trim()) {
            return forwarded.split(",")[0].trim();
        }
    }

    return request.socket.remoteAddress ?? "unknown";
}

function rateKey(prefix, value) {
    return `${prefix}:${value}`;
}

function bearerToken(request) {
    const header = request.headers.authorization;
    if (typeof header !== "string") return null;

    const match = /^Bearer\s+(.+)$/i.exec(header);
    return match?.[1] ?? null;
}

export function createServer({
    pool = getPool(),
    accessTokenSecret = process.env.ACCESS_TOKEN_SECRET,
    matrixJwtSecret = process.env.MATRIX_JWT_SECRET,
    matrixJwtIssuer = process.env.MATRIX_JWT_ISSUER,
    matrixJwtAudience = process.env.MATRIX_JWT_AUDIENCE,
    authRateLimiter = new RateLimiter()
} = {}) {
    let authService = null;
    let socialService = null;
    let matrixTokenService = null;
    if (pool && accessTokenSecret) {
        authService = createAuthService({ pool, accessTokenSecret });
        socialService = createSocialService({ pool });
    }

    if (matrixJwtSecret && matrixJwtIssuer && matrixJwtAudience) {
        matrixTokenService = createMatrixTokenService({
            matrixJwtSecret,
            issuer: matrixJwtIssuer,
            audience: matrixJwtAudience
        });
    }

    return http.createServer(async (request, response) => {
        try {
            if (request.method === "GET" && request.url === "/healthz") {
                sendJson(response, 200, { ok: true, service: "pauzechats-api" });
                return;
            }

            if (!authService) {
                sendJson(response, 503, { error: "service_not_configured" });
                return;
            }

            const requestUrl = new URL(request.url ?? "/", `http://${request.headers.host ?? "localhost"}`);
            const path = requestUrl.pathname;

            if (request.method === "POST" && path === "/v1/auth/register") {
                const limit = authRateLimiter.consume(
                    rateKey("register-ip", clientAddress(request)),
                    REGISTER_RATE_LIMIT,
                    AUTH_RATE_WINDOW_MS
                );

                if (!limit.allowed) {
                    response.setHeader("retry-after", String(limit.retryAfterSeconds));
                    sendJson(response, 429, { error: "rate_limited" });
                    return;
                }

                const body = await readJsonBody(request);
                const result = await authService.register(body);
                sendJson(response, 201, result);
                return;
            }

            if (request.method === "POST" && path === "/v1/auth/sign-in") {
                const ipLimit = authRateLimiter.consume(
                    rateKey("sign-in-ip", clientAddress(request)),
                    SIGN_IN_RATE_LIMIT,
                    AUTH_RATE_WINDOW_MS
                );

                const body = await readJsonBody(request);

                if (!ipLimit.allowed) {
                    response.setHeader("retry-after", String(ipLimit.retryAfterSeconds));
                    sendJson(response, 429, { error: "rate_limited" });
                    return;
                }

                const email =
                    typeof body?.email === "string"
                        ? body.email.trim().toLowerCase()
                        : "";

                if (email) {
                    const credentialLimit = authRateLimiter.consume(
                        rateKey("sign-in-email", email),
                        SIGN_IN_RATE_LIMIT,
                        AUTH_RATE_WINDOW_MS
                    );

                    if (!credentialLimit.allowed) {
                        response.setHeader(
                            "retry-after",
                            String(credentialLimit.retryAfterSeconds)
                        );
                        sendJson(response, 429, { error: "rate_limited" });
                        return;
                    }
                }

                const result = await authService.signIn(body);
                sendJson(response, 200, result);
                return;
            }

            if (request.method === "POST" && path === "/v1/auth/refresh") {
                const limit = authRateLimiter.consume(
                    rateKey("refresh-ip", clientAddress(request)),
                    REFRESH_RATE_LIMIT,
                    AUTH_RATE_WINDOW_MS
                );

                if (!limit.allowed) {
                    response.setHeader("retry-after", String(limit.retryAfterSeconds));
                    sendJson(response, 429, { error: "rate_limited" });
                    return;
                }

                const body = await readJsonBody(request);
                const result = await authService.refresh(body?.refreshToken);
                sendJson(response, 200, result);
                return;
            }

            const accessToken = bearerToken(request);
            if (!accessToken) {
                sendJson(response, 401, { error: "missing_access_token" });
                return;
            }

            const auth = await authService.authenticate(accessToken);

            if (request.method === "POST" && path === "/v1/auth/sign-out") {
                await authService.signOut(auth.sessionId);
                sendJson(response, 200, { ok: true });
                return;
            }

            if (request.method === "POST" && path === "/v1/messaging/matrix-token") {
                if (!matrixTokenService) {
                    sendJson(response, 503, { error: "messaging_not_configured" });
                    return;
                }

                const limit = authRateLimiter.consume(
                    rateKey("matrix-token-session", auth.sessionId),
                    MATRIX_TOKEN_RATE_LIMIT,
                    AUTH_RATE_WINDOW_MS
                );

                if (!limit.allowed) {
                    response.setHeader("retry-after", String(limit.retryAfterSeconds));
                    sendJson(response, 429, { error: "rate_limited" });
                    return;
                }

                const profile = await authService.getProfile(auth.userId);
                const token = await matrixTokenService.issueToken({
                    username: profile.username,
                    displayName: profile.displayName
                });

                sendJson(response, 200, {
                    token,
                    expiresInSeconds: matrixTokenService.ttlSeconds
                });
                return;
            }

            if (request.method === "GET" && path === "/v1/me") {
                const profile = await authService.getProfile(auth.userId);
                sendJson(response, 200, profile);
                return;
            }

            if (request.method === "PATCH" && path === "/v1/me/profile") {
                const body = await readJsonBody(request);
                const profile = await socialService.updateProfile(auth.userId, body);
                sendJson(response, 200, profile);
                return;
            }

            if (request.method === "GET" && path === "/v1/users/lookup") {
                const username = requestUrl.searchParams.get("username") ?? "";
                const profile = await socialService.lookupUserByUsername(username);
                sendJson(response, 200, profile);
                return;
            }

            if (request.method === "POST" && path === "/v1/friends/requests") {
                const body = await readJsonBody(request);
                const result = await socialService.createFriendRequest(
                    auth.userId,
                    body?.username
                );
                sendJson(response, 201, result);
                return;
            }

            if (request.method === "GET" && path === "/v1/friends/requests/incoming") {
                const result = await socialService.listFriendRequests(auth.userId, "incoming");
                sendJson(response, 200, result);
                return;
            }

            if (request.method === "GET" && path === "/v1/friends/requests/outgoing") {
                const result = await socialService.listFriendRequests(auth.userId, "outgoing");
                sendJson(response, 200, result);
                return;
            }

            const friendRequestMatch =
                /^\/v1\/friends\/requests\/([^/]+)\/(accept|decline|cancel)$/.exec(path);

            if (request.method === "POST" && friendRequestMatch) {
                const action = {
                    accept: "ACCEPTED",
                    decline: "DECLINED",
                    cancel: "CANCELLED"
                }[friendRequestMatch[2]];

                const result = await socialService.respondToFriendRequest(
                    auth.userId,
                    friendRequestMatch[1],
                    action
                );

                sendJson(response, 200, result);
                return;
            }

            if (request.method === "GET" && path === "/v1/friends") {
                const result = await socialService.listFriends(auth.userId);
                sendJson(response, 200, result);
                return;
            }

            sendJson(response, 404, { error: "not_found" });
        } catch (error) {
            if (error instanceof AuthError || error instanceof SocialError) {
                sendJson(response, error.status, { error: error.code });
                return;
            }

            console.error(error);
            sendJson(response, 500, { error: "internal_server_error" });
        }
    });
}

if (import.meta.url === `file://${process.argv[1]}`) {
    const server = createServer();
    server.listen(port, "0.0.0.0", () => {
        console.log(`PauzeChats API listening on port ${port}`);
    });
}
