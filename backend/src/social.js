import { randomUUID } from "node:crypto";
import {
    normalizeProfileInput,
    normalizeUsername
} from "./validation.js";

export class SocialError extends Error {
    constructor(code, status = 400) {
        super(code);
        this.code = code;
        this.status = status;
    }
}

function profileFromRow(row) {
    return {
        userId: row.user_id,
        username: row.username,
        displayName: row.display_name,
        bio: row.bio,
        avatarMimeType: row.avatar_mime_type ?? null
    };
}

export function createSocialService({ pool }) {
    if (!pool) {
        throw new Error("Database pool is required");
    }

    async function lookupUserByUsername(username) {
        const normalized = normalizeUsername(username);
        if (!normalized) {
            throw new SocialError("invalid_username", 400);
        }

        const result = await pool.query(
            `select p.user_id, p.username, p.display_name, p.bio, p.avatar_mime_type
             from profiles p
             join users u on u.id = p.user_id
             where p.username = $1 and u.disabled_at is null`,
            [normalized]
        );

        const row = result.rows[0];
        if (!row) {
            throw new SocialError("user_not_found", 404);
        }

        return profileFromRow(row);
    }

    async function updateProfile(userId, input) {
        const profile = normalizeProfileInput(input?.displayName, input?.bio);

        if (!profile) {
            throw new SocialError("invalid_profile", 400);
        }

        const result = await pool.query(
            `update profiles
             set display_name = $1,
                 bio = $2,
                 updated_at = now()
             where user_id = $3
             returning user_id, username, display_name, bio, avatar_mime_type`,
            [profile.displayName, profile.bio, userId]
        );

        const row = result.rows[0];
        if (!row) {
            throw new SocialError("profile_not_found", 404);
        }

        return profileFromRow(row);
    }

    async function createFriendRequest(fromUserId, username) {
        const normalized = normalizeUsername(username);
        if (!normalized) {
            throw new SocialError("invalid_username", 400);
        }

        const client = await pool.connect();

        try {
            await client.query("begin");

            const targetResult = await client.query(
                `select p.user_id, p.username, p.display_name, p.bio, p.avatar_mime_type
                 from profiles p
                 join users u on u.id = p.user_id
                 where p.username = $1 and u.disabled_at is null`,
                [normalized]
            );

            const target = targetResult.rows[0];
            if (!target) {
                throw new SocialError("user_not_found", 404);
            }

            if (target.user_id === fromUserId) {
                throw new SocialError("cannot_add_self", 400);
            }

            const existingResult = await client.query(
                `select id, from_user_id, to_user_id, status
                 from friend_requests
                 where (
                    (from_user_id = $1 and to_user_id = $2)
                    or
                    (from_user_id = $2 and to_user_id = $1)
                 )
                 and status in ('PENDING', 'ACCEPTED')
                 order by created_at desc
                 limit 1
                 for update`,
                [fromUserId, target.user_id]
            );

            const existing = existingResult.rows[0];
            if (existing?.status === "ACCEPTED") {
                throw new SocialError("already_friends", 409);
            }

            if (existing?.status === "PENDING") {
                if (existing.from_user_id === target.user_id) {
                    throw new SocialError("incoming_request_exists", 409);
                }
                throw new SocialError("friend_request_exists", 409);
            }

            const requestId = randomUUID();
            await client.query(
                `insert into friend_requests (
                    id, from_user_id, to_user_id, status
                 ) values ($1, $2, $3, 'PENDING')`,
                [requestId, fromUserId, target.user_id]
            );

            await client.query("commit");

            return {
                id: requestId,
                status: "PENDING",
                to: {
                    username: target.username,
                    displayName: target.display_name
                }
            };
        } catch (error) {
            await client.query("rollback").catch(() => {});
            if (error?.code === "23505") {
                throw new SocialError("friend_request_exists", 409);
            }
            throw error;
        } finally {
            client.release();
        }
    }

    async function listFriendRequests(userId, direction) {
        const column = direction === "incoming" ? "to_user_id" : "from_user_id";
        const otherColumn = direction === "incoming" ? "from_user_id" : "to_user_id";

        const result = await pool.query(
            `select f.id, f.status, f.created_at,
                    p.user_id, p.username, p.display_name, p.bio, p.avatar_mime_type
             from friend_requests f
             join profiles p on p.user_id = f.${otherColumn}
             where f.${column} = $1
               and f.status = 'PENDING'
             order by f.created_at desc`,
            [userId]
        );

        return result.rows.map((row) => ({
            id: row.id,
            status: row.status,
            createdAt: row.created_at,
            user: {
                userId: row.user_id,
                username: row.username,
                displayName: row.display_name,
                bio: row.bio,
                avatarMimeType: row.avatar_mime_type ?? null
            }
        }));
    }

    async function respondToFriendRequest(userId, requestId, action) {
        if (!["ACCEPTED", "DECLINED", "CANCELLED"].includes(action)) {
            throw new SocialError("invalid_friend_request_action", 400);
        }

        const ownerColumn = action === "CANCELLED" ? "from_user_id" : "to_user_id";

        const result = await pool.query(
            `update friend_requests
             set status = $1, updated_at = now()
             where id = $2
               and ${ownerColumn} = $3
               and status = 'PENDING'
             returning id, status`,
            [action, requestId, userId]
        );

        const row = result.rows[0];
        if (!row) {
            throw new SocialError("friend_request_not_found", 404);
        }

        return {
            id: row.id,
            status: row.status
        };
    }

    async function listFriends(userId) {
        const result = await pool.query(
            `select case
                        when f.from_user_id = $1 then f.to_user_id
                        else f.from_user_id
                    end as user_id,
                    p.username, p.display_name, p.bio, p.avatar_mime_type
             from friend_requests f
             join profiles p on p.user_id = case
                        when f.from_user_id = $1 then f.to_user_id
                        else f.from_user_id
                    end
             where (f.from_user_id = $1 or f.to_user_id = $1)
               and f.status = 'ACCEPTED'
             order by p.username asc`,
            [userId]
        );

        return result.rows.map(profileFromRow);
    }

    return {
        lookupUserByUsername,
        updateProfile,
        createFriendRequest,
        listFriendRequests,
        respondToFriendRequest,
        listFriends
    };
}
