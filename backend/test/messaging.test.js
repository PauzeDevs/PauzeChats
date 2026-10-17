import test from "node:test";
import assert from "node:assert/strict";
import { createMessagingService, MessagingError } from "../src/messaging.js";

const userId = "11111111-1111-4111-8111-111111111111";
const conversationId = "22222222-2222-4222-8222-222222222222";
const clientMessageId = "33333333-3333-4333-8333-333333333333";

function errorWithCode(promise, code, status) {
    return assert.rejects(promise, (error) => {
        assert.ok(error instanceof MessagingError);
        assert.equal(error.code, code);
        assert.equal(error.status, status);
        return true;
    });
}

test("messaging service requires a database pool", () => {
    assert.throws(() => createMessagingService({ pool: null }), /Database pool is required/);
});

test("sendMessage rejects malformed conversation IDs before querying the database", async () => {
    let queried = false;
    const service = createMessagingService({
        pool: { query: async () => { queried = true; throw new Error("unexpected query"); } }
    });

    await errorWithCode(
        service.sendMessage(userId, "not-a-uuid", {
            ciphertext: "ciphertext",
            ciphertextVersion: 1,
            clientMessageId
        }),
        "invalid_conversation_id",
        400
    );
    assert.equal(queried, false);
});

test("sendMessage rejects missing, oversized, or invalid-version ciphertext payloads", async () => {
    const service = createMessagingService({
        pool: { query: async () => { throw new Error("payload validation should happen first"); } }
    });

    for (const payload of [
        {},
        { ciphertext: "", ciphertextVersion: 1, clientMessageId },
        { ciphertext: "ciphertext", ciphertextVersion: 0, clientMessageId },
        { ciphertext: "ciphertext", ciphertextVersion: 1.5, clientMessageId },
        { ciphertext: "ciphertext", ciphertextVersion: 1, clientMessageId: "bad-id" },
        { ciphertext: "x".repeat(65537), ciphertextVersion: 1, clientMessageId }
    ]) {
        await errorWithCode(
            service.sendMessage(userId, conversationId, payload),
            "invalid_encrypted_payload",
            400
        );
    }
});

test("sendMessage rejects non-members without inserting a message", async () => {
    const calls = [];
    const service = createMessagingService({
        pool: {
            query: async (sql, params) => {
                calls.push({ sql, params });
                return { rows: [] };
            }
        }
    });

    await errorWithCode(
        service.sendMessage(userId, conversationId, {
            ciphertext: "opaque-ciphertext",
            ciphertextVersion: 1,
            clientMessageId
        }),
        "conversation_not_found",
        404
    );

    assert.equal(calls.length, 1);
    assert.match(calls[0].sql, /from conversation_members/);
});
