import { randomUUID } from "node:crypto";

export class MessagingError extends Error {
    constructor(code, status = 400) {
        super(code);
        this.code = code;
        this.status = status;
    }
}

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

function validUuid(value) {
    return typeof value === "string" && UUID.test(value);
}

function normalizeCiphertext(input) {
    if (!input || typeof input !== "object") return null;
    const ciphertext = input.ciphertext;
    const version = input.ciphertextVersion;
    const clientMessageId = input.clientMessageId;
    if (typeof ciphertext !== "string" || ciphertext.length < 1 || ciphertext.length > 65536) return null;
    if (!Number.isInteger(version) || version < 1 || version > 32767) return null;
    if (!validUuid(clientMessageId)) return null;
    return { ciphertext, ciphertextVersion: version, clientMessageId };
}

export function createMessagingService({ pool }) {
    if (!pool) throw new Error("Database pool is required");

    async function createDirectConversation(userId, otherUserId) {
        if (!validUuid(otherUserId)) throw new MessagingError("invalid_user_id", 400);
        if (userId === otherUserId) throw new MessagingError("cannot_chat_with_self", 400);

        const client = await pool.connect();
        try {
            await client.query("begin");
            const friendship = await client.query(
                `select 1 from friend_requests
                 where status = 'ACCEPTED'
                   and ((from_user_id = $1 and to_user_id = $2)
                     or (from_user_id = $2 and to_user_id = $1))
                 limit 1`,
                [userId, otherUserId]
            );
            if (!friendship.rows[0]) throw new MessagingError("users_are_not_friends", 403);

            const existing = await client.query(
                `select c.id
                 from conversations c
                 join conversation_members a on a.conversation_id = c.id and a.user_id = $1 and a.left_at is null
                 join conversation_members b on b.conversation_id = c.id and b.user_id = $2 and b.left_at is null
                 where c.kind = 'DIRECT'
                   and (select count(*) from conversation_members m
                        where m.conversation_id = c.id and m.left_at is null) = 2
                 limit 1`,
                [userId, otherUserId]
            );
            if (existing.rows[0]) {
                await client.query("commit");
                return { id: existing.rows[0].id, kind: "DIRECT", created: false };
            }

            const id = randomUUID();
            await client.query(
                "insert into conversations (id, kind, created_by) values ($1, 'DIRECT', $2)",
                [id, userId]
            );
            await client.query(
                "insert into conversation_members (conversation_id, user_id) values ($1, $2), ($1, $3)",
                [id, userId, otherUserId]
            );
            await client.query("commit");
            return { id, kind: "DIRECT", created: true };
        } catch (error) {
            await client.query("rollback").catch(() => {});
            throw error;
        } finally {
            client.release();
        }
    }

    async function listConversations(userId) {
        const result = await pool.query(
            `select c.id, c.kind, c.title, c.created_at,
                    (select count(*) from conversation_members m
                     where m.conversation_id = c.id and m.left_at is null) as member_count,
                    (select max(msg.created_at) from encrypted_messages msg
                     where msg.conversation_id = c.id and msg.deleted_at is null) as last_message_at
             from conversations c
             join conversation_members mine
               on mine.conversation_id = c.id and mine.user_id = $1 and mine.left_at is null
             order by coalesce(
                 (select max(msg.created_at) from encrypted_messages msg
                  where msg.conversation_id = c.id and msg.deleted_at is null),
                 c.created_at
             ) desc
             limit 100`,
            [userId]
        );
        return result.rows.map((row) => ({
            id: row.id,
            kind: row.kind,
            title: row.title,
            createdAt: row.created_at,
            memberCount: Number(row.member_count),
            lastMessageAt: row.last_message_at
        }));
    }

    async function listMessages(userId, conversationId, before, limit = 50) {
        if (!validUuid(conversationId)) throw new MessagingError("invalid_conversation_id", 400);
        const pageSize = Number.isInteger(limit) ? Math.max(1, Math.min(limit, 100)) : 50;
        if (before && !Number.isFinite(Date.parse(before))) throw new MessagingError("invalid_cursor", 400);

        const member = await pool.query(
            `select 1 from conversation_members
             where conversation_id = $1 and user_id = $2 and left_at is null`,
            [conversationId, userId]
        );
        if (!member.rows[0]) throw new MessagingError("conversation_not_found", 404);

        const result = await pool.query(
            `select id, sender_user_id, ciphertext, ciphertext_version,
                    client_message_id, created_at, edited_at, deleted_at
             from encrypted_messages
             where conversation_id = $1
               and ($2::timestamptz is null or created_at < $2::timestamptz)
             order by created_at desc, id desc
             limit $3`,
            [conversationId, before ?? null, pageSize]
        );
        return result.rows.reverse().map((row) => ({
            id: row.id,
            senderUserId: row.sender_user_id,
            ciphertext: row.deleted_at ? null : row.ciphertext,
            ciphertextVersion: row.ciphertext_version,
            clientMessageId: row.client_message_id,
            createdAt: row.created_at,
            editedAt: row.edited_at,
            deleted: Boolean(row.deleted_at)
        }));
    }

    async function sendMessage(userId, conversationId, input) {
        if (!validUuid(conversationId)) throw new MessagingError("invalid_conversation_id", 400);
        const message = normalizeCiphertext(input);
        if (!message) throw new MessagingError("invalid_encrypted_payload", 400);

        const member = await pool.query(
            `select 1 from conversation_members
             where conversation_id = $1 and user_id = $2 and left_at is null`,
            [conversationId, userId]
        );
        if (!member.rows[0]) throw new MessagingError("conversation_not_found", 404);

        const result = await pool.query(
            `insert into encrypted_messages
                (id, conversation_id, sender_user_id, ciphertext, ciphertext_version, client_message_id)
             values ($1, $2, $3, $4, $5, $6)
             on conflict (sender_user_id, client_message_id) do nothing
             returning id, sender_user_id, ciphertext, ciphertext_version, client_message_id, created_at`,
            [randomUUID(), conversationId, userId, message.ciphertext, message.ciphertextVersion, message.clientMessageId]
        );
        let row = result.rows[0];
        if (!row) {
            const prior = await pool.query(
                `select id, conversation_id, sender_user_id, ciphertext, ciphertext_version,
                        client_message_id, created_at
                 from encrypted_messages where sender_user_id = $1 and client_message_id = $2`,
                [userId, message.clientMessageId]
            );
            row = prior.rows[0];
            if (!row || row.conversation_id !== conversationId ||
                row.ciphertext !== message.ciphertext ||
                row.ciphertext_version !== message.ciphertextVersion) {
                throw new MessagingError("client_message_id_conflict", 409);
            }
        }
        return {
            id: row.id,
            senderUserId: row.sender_user_id,
            ciphertext: row.ciphertext,
            ciphertextVersion: row.ciphertext_version,
            clientMessageId: row.client_message_id,
            createdAt: row.created_at
        };
    }

    async function deleteOwnMessage(userId, messageId) {
        if (!validUuid(messageId)) throw new MessagingError("invalid_message_id", 400);
        const result = await pool.query(
            `update encrypted_messages msg
             set deleted_at = now(), ciphertext = '[deleted]'
             where msg.id = $1 and msg.sender_user_id = $2 and msg.deleted_at is null
               and exists (
                   select 1 from conversation_members member
                   where member.conversation_id = msg.conversation_id
                     and member.user_id = $2 and member.left_at is null
               )
             returning msg.id, msg.deleted_at`,
            [messageId, userId]
        );
        const row = result.rows[0];
        if (!row) throw new MessagingError("message_not_found", 404);
        return { id: row.id, deleted: true, deletedAt: row.deleted_at };
    }

    return { createDirectConversation, listConversations, listMessages, sendMessage, deleteOwnMessage };
}
