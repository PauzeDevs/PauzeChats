import test from "node:test";
import assert from "node:assert/strict";
import { RateLimiter } from "../src/rate-limit.js";

test("allows requests up to the limit", () => {
    let now = 1_000;
    const limiter = new RateLimiter({ now: () => now });

    assert.deepEqual(
        limiter.consume("ip:1", 2, 10_000),
        { allowed: true, remaining: 1, retryAfterSeconds: 0 }
    );
    assert.deepEqual(
        limiter.consume("ip:1", 2, 10_000),
        { allowed: true, remaining: 0, retryAfterSeconds: 0 }
    );
    assert.deepEqual(
        limiter.consume("ip:1", 2, 10_000),
        { allowed: false, remaining: 0, retryAfterSeconds: 10 }
    );
});

test("resets a bucket after expiry", () => {
    let now = 2_000;
    const limiter = new RateLimiter({ now: () => now });

    limiter.consume("ip:2", 1, 5_000);
    now += 5_001;

    assert.deepEqual(
        limiter.consume("ip:2", 1, 5_000),
        { allowed: true, remaining: 0, retryAfterSeconds: 0 }
    );
});

test("keeps stored buckets bounded", () => {
    const limiter = new RateLimiter({ maxEntries: 2, now: () => 3_000 });

    limiter.consume("a", 1, 10_000);
    limiter.consume("b", 1, 10_000);
    limiter.consume("c", 1, 10_000);

    assert.ok(limiter.entries.size <= 2);
});
