import test from "node:test";
import assert from "node:assert/strict";
import { createSocialService, SocialError } from "../src/social.js";

function createPool(responses) {
    const calls = [];
    let index = 0;

    return {
        calls,
        async query(text, params) {
            calls.push({ text, params });
            return responses[index++] ?? { rows: [] };
        },
        async connect() {
            const client = {
                async query(text, params) {
                    calls.push({ text, params });
                    return responses[index++] ?? { rows: [] };
                },
                release() {}
            };
            return client;
        }
    };
}

test("username lookup normalizes the username", async () => {
    const pool = createPool([
        { rows: [] },
        {
            rows: [{
                user_id: "u-1",
                username: "friend_one",
                display_name: "Friend One",
                bio: "hello",
                avatar_mime_type: null
            }]
        }
    ]);

    const service = createSocialService({ pool });
    const profile = await service.lookupUserByUsername("Friend_One");

    assert.equal(profile.username, "friend_one");
    assert.equal(profile.displayName, "Friend One");
    assert.equal(pool.calls[0].params[0], "friend_one");
});

test("profile updates reject invalid data", async () => {
    const pool = createPool([]);
    const service = createSocialService({ pool });

    await assert.rejects(
        () => service.updateProfile("u-1", { displayName: "", bio: "x" }),
        (error) => error instanceof SocialError && error.code === "invalid_profile"
    );
});

test("friend requests reject self-adds", async () => {
    const pool = createPool([
        {
            rows: [{
                user_id: "u-1",
                username: "pauze",
                display_name: "Pauze",
                bio: "",
                avatar_mime_type: null
            }]
        }
    ]);

    const service = createSocialService({ pool });

    await assert.rejects(
        () => service.createFriendRequest("u-1", "pauze"),
        (error) => error instanceof SocialError && error.code === "cannot_add_self"
    );
});

test("friend request response returns the updated state", async () => {
    const pool = createPool([
        { rows: [{ id: "r-1", status: "ACCEPTED" }] }
    ]);

    const service = createSocialService({ pool });
    const result = await service.respondToFriendRequest("u-2", "r-1", "ACCEPTED");

    assert.deepEqual(result, { id: "r-1", status: "ACCEPTED" });
    assert.match(pool.calls[0].text, /status = \$1/);
});
