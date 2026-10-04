import test from "node:test";
import assert from "node:assert/strict";
import { hashPassword, verifyPassword } from "../src/auth.js";

test("password hashing uses Argon2id and verifies correctly", async () => {
    const password = "correctHorseBattery9";
    const passwordHash = await hashPassword(password);

    assert.match(passwordHash, /^\$argon2id\$/);
    assert.equal(await verifyPassword(passwordHash, password), true);
    assert.equal(await verifyPassword(passwordHash, "wrongPassword9"), false);
});
