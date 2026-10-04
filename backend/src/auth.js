import { createHash, randomBytes, randomUUID } from "node:crypto";
import argon2 from "argon2";
import { jwtVerify, SignJWT } from "jose";
import {
    normalizeEmail,
    normalizeProfileInput,
    normalizeUsername,
    validatePassword
} from "./validation.js";

const ACCESS_TOKEN_TTL_SECONDS = 15 * 60;
const REFRESH_TOKEN_TTL_SECONDS = 30 * 24 * 60 * 60;

export class AuthError extends Error {
    constructor(code, status = 400) {
        super(code);
        this.code = code;
        this.status = status;
    }
}

function requireSecret(secret) {
    if (typeof secret !== "string" || secret.length < 32) {
        throw new Error("ACCESS_TOKEN_SECRET must be at least 32 characters");
    }
    return new TextEncoder().encode(secret);
}

function hashOpaqueToken(token) {
    return createHash("sha256").update(token, "utf8").digest();
}

function issueRefreshToken() {
    return randomBytes(32).toString("base64url");
}

export async function hashPassword(password) {
    return argon2.hash(password, { type: argon2.argon2id });
}

export async function verifyPassword(passwordHash, password) {
    return argon2.verify(passwordHash, password);
}

export function createAuthService({ pool, accessTokenSecret }) {
    if (!pool) {
        throw new Error("Database pool is required");
    }

    const jwtKey = requireSecret(accessTokenSecret);

    async function createSession(client, userId) {
        const sessionId = randomUUID();
        const refreshToken = issueRefreshToken();
        const refreshTokenHash = hashOpaqueToken(refreshToken);
        const expiresAt = new Date(Date.now() + REFRESH_TOKEN_TTL_SECONDS * 1000);

        await client.query(
            `insert into sessions (
                id, user_id, refresh_token_hash, expires_at
            ) values ($1, $2, $3, $4)`,
            [sessionId, userId, refreshTokenHash, expiresAt]
        );

        const accessToken = await new SignJWT({ sid: sessionId })
            .setProtectedHeader({ alg: "HS256" })
            .setSubject(userId)
            .setIssuedAt()
            .setExpirationTime(`${ACCESS_TOKEN_TTL_SECONDS}s`)
            .sign(jwtKey);

        return {
            sessionId,
            accessToken,
            refreshToken,
            expiresInSeconds: ACCESS_TOKEN_TTL_SECONDS
        };
    }

    async function register(input) {
        const email = normalizeEmail(input?.email);
        const username = normalizeUsername(input?.username);
        const password = input?.password;
        const profile = normalizeProfileInput(input?.displayName, input?.bio);
        const inviteCode =
            typeof input?.inviteCode === "string" ? input.inviteCode.trim() : "";

        if (!email || !username || !validatePassword(password) || !profile || inviteCode.length < 16) {
            throw new AuthError("invalid_registration", 400);
        }

        const client = await pool.connect();

        try {
            await client.query("begin");

            const inviteResult = await client.query(
                `select id, max_uses, use_count, expires_at, revoked_at
                 from invites
                 where code_hash = $1
                 for update`,
                [hashOpaqueToken(inviteCode)]
            );

            const invite = inviteResult.rows[0];
            if (!invite ||
                invite.revoked_at ||
                (invite.expires_at && new Date(invite.expires_at).getTime() <= Date.now()) ||
                invite.use_count >= invite.max_uses) {
                throw new AuthError("invalid_invite", 403);
            }

            const passwordHash = await hashPassword(password);
            const userId = randomUUID();

            try {
                await client.query(
                    `insert into users (id, email, password_hash)
                     values ($1, $2, $3)`,
                    [userId, email, passwordHash]
                );

                await client.query(
                    `insert into profiles (
                        user_id, username, display_name, bio
                    ) values ($1, $2, $3, $4)`,
                    [userId, username, profile.displayName, profile.bio]
                );
            } catch (error) {
                if (error?.code === "23505") {
                    throw new AuthError("email_or_username_taken", 409);
                }
                throw error;
            }

            await client.query(
                `update invites
                 set use_count = use_count + 1
                 where id = $1`,
                [invite.id]
            );

            const session = await createSession(client, userId);
            await client.query("commit");

            return {
                session,
                profile: {
                    userId,
                    username,
                    displayName: profile.displayName,
                    bio: profile.bio,
                    avatarMimeType: null
                }
            };
        } catch (error) {
            await client.query("rollback").catch(() => {});
            throw error;
        } finally {
            client.release();
        }
    }

    async function signIn(input) {
        const email = normalizeEmail(input?.email);
        const password = input?.password;

        if (!email || !validatePassword(password)) {
            throw new AuthError("invalid_credentials", 401);
        }

        const client = await pool.connect();

        try {
            const result = await client.query(
                `select u.id, u.password_hash, u.disabled_at,
                        p.username, p.display_name, p.bio, p.avatar_mime_type
                 from users u
                 join profiles p on p.user_id = u.id
                 where u.email = $1`,
                [email]
            );

            const user = result.rows[0];
            if (!user || user.disabled_at) {
                throw new AuthError("invalid_credentials", 401);
            }

            const validPassword = await verifyPassword(user.password_hash, password);
            if (!validPassword) {
                throw new AuthError("invalid_credentials", 401);
            }

            await client.query("begin");

            try {
                const session = await createSession(client, user.id);
                await client.query("commit");

                return {
                    session,
                    profile: {
                        userId: user.id,
                        username: user.username,
                        displayName: user.display_name,
                        bio: user.bio,
                        avatarMimeType: user.avatar_mime_type
                    }
                };
            } catch (error) {
                await client.query("rollback").catch(() => {});
                throw error;
            }
        } finally {
            client.release();
        }
    }

    async function refresh(refreshToken) {
        if (typeof refreshToken !== "string" || refreshToken.length < 32) {
            throw new AuthError("invalid_refresh_token", 401);
        }

        const client = await pool.connect();

        try {
            await client.query("begin");

            const result = await client.query(
                `select s.id, s.user_id, s.expires_at, s.revoked_at,
                        u.disabled_at,
                        p.username, p.display_name, p.bio, p.avatar_mime_type
                 from sessions s
                 join users u on u.id = s.user_id
                 join profiles p on p.user_id = u.id
                 where s.refresh_token_hash = $1
                 for update`,
                [hashOpaqueToken(refreshToken)]
            );

            const session = result.rows[0];
            if (!session ||
                session.revoked_at ||
                new Date(session.expires_at).getTime() <= Date.now() ||
                session.disabled_at) {
                throw new AuthError("invalid_refresh_token", 401);
            }

            const newRefreshToken = issueRefreshToken();
            const newRefreshTokenHash = hashOpaqueToken(newRefreshToken);
            const expiresAt = new Date(Date.now() + REFRESH_TOKEN_TTL_SECONDS * 1000);

            await client.query(
                `update sessions
                 set refresh_token_hash = $1, expires_at = $2
                 where id = $3`,
                [newRefreshTokenHash, expiresAt, session.id]
            );

            const accessToken = await new SignJWT({ sid: session.id })
                .setProtectedHeader({ alg: "HS256" })
                .setSubject(session.user_id)
                .setIssuedAt()
                .setExpirationTime(`${ACCESS_TOKEN_TTL_SECONDS}s`)
                .sign(jwtKey);

            await client.query("commit");

            return {
                session: {
                    sessionId: session.id,
                    accessToken,
                    refreshToken: newRefreshToken,
                    expiresInSeconds: ACCESS_TOKEN_TTL_SECONDS
                },
                profile: {
                    userId: session.user_id,
                    username: session.username,
                    displayName: session.display_name,
                    bio: session.bio,
                    avatarMimeType: session.avatar_mime_type
                }
            };
        } catch (error) {
            await client.query("rollback").catch(() => {});
            throw error;
        } finally {
            client.release();
        }
    }

    async function authenticate(accessToken) {
        if (typeof accessToken !== "string" || accessToken.length < 20) {
            throw new AuthError("invalid_access_token", 401);
        }

        let verified;
        try {
            verified = await jwtVerify(accessToken, jwtKey, {
                algorithms: ["HS256"]
            });
        } catch {
            throw new AuthError("invalid_access_token", 401);
        }

        const sessionId = verified.payload.sid;
        const userId = verified.payload.sub;

        if (typeof sessionId !== "string" || typeof userId !== "string") {
            throw new AuthError("invalid_access_token", 401);
        }

        const result = await pool.query(
            `select s.id, s.user_id, s.expires_at, s.revoked_at,
                    u.disabled_at
             from sessions s
             join users u on u.id = s.user_id
             where s.id = $1 and s.user_id = $2`,
            [sessionId, userId]
        );

        const session = result.rows[0];
        if (!session ||
            session.revoked_at ||
            new Date(session.expires_at).getTime() <= Date.now() ||
            session.disabled_at) {
            throw new AuthError("invalid_session", 401);
        }

        return { sessionId, userId };
    }

    async function signOut(sessionId) {
        await pool.query(
            `update sessions
             set revoked_at = now()
             where id = $1`,
            [sessionId]
        );
    }

    async function getProfile(userId) {
        const result = await pool.query(
            `select user_id, username, display_name, bio, avatar_mime_type
             from profiles
             where user_id = $1`,
            [userId]
        );

        const row = result.rows[0];
        if (!row) {
            throw new AuthError("profile_not_found", 404);
        }

        return {
            userId: row.user_id,
            username: row.username,
            displayName: row.display_name,
            bio: row.bio,
            avatarMimeType: row.avatar_mime_type
        };
    }

    return {
        register,
        signIn,
        refresh,
        authenticate,
        signOut,
        getProfile
    };
}
