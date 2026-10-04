import http from "node:http";
import { createAuthService, AuthError } from "./auth.js";
import { createSocialService, SocialError } from "./social.js";
import { getPool } from "./db.js";

const port = Number.parseInt(process.env.PORT ?? "8080", 10);
const MAX_BODY_BYTES = 64 * 1024;

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

function bearerToken(request) {
    const header = request.headers.authorization;
    if (typeof header !== "string") return null;

    const match = /^Bearer\s+(.+)$/i.exec(header);
    return match?.[1] ?? null;
}

export function createServer({ pool = getPool(), accessTokenSecret = process.env.ACCESS_TOKEN_SECRET } = {}) {
    let authService = null;
    let socialService = null;
    if (pool && accessTokenSecret) {
        authService = createAuthService({ pool, accessTokenSecret });
        socialService = createSocialService({ pool });
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
                const body = await readJsonBody(request);
                const result = await authService.register(body);
                sendJson(response, 201, result);
                return;
            }

            if (request.method === "POST" && path === "/v1/auth/sign-in") {
                const body = await readJsonBody(request);
                const result = await authService.signIn(body);
                sendJson(response, 200, result);
                return;
            }

            if (request.method === "POST" && path === "/v1/auth/refresh") {
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
