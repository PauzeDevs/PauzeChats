export class RateLimiter {
    constructor({ maxEntries = 10_000, now = () => Date.now() } = {}) {
        this.maxEntries = maxEntries;
        this.now = now;
        this.entries = new Map();
    }

    consume(key, limit, windowMs) {
        if (
            !key ||
            !Number.isInteger(limit) ||
            limit < 1 ||
            !Number.isInteger(windowMs) ||
            windowMs < 1
        ) {
            throw new Error("Invalid rate limiter configuration");
        }

        const now = this.now();
        const current = this.entries.get(key);

        if (!current || current.expiresAt <= now) {
            this.entries.set(key, { count: 1, expiresAt: now + windowMs });
            this.trim(now);
            return {
                allowed: true,
                remaining: Math.max(0, limit - 1),
                retryAfterSeconds: 0
            };
        }

        current.count += 1;

        if (current.count > limit) {
            return {
                allowed: false,
                remaining: 0,
                retryAfterSeconds: Math.max(
                    1,
                    Math.ceil((current.expiresAt - now) / 1000)
                )
            };
        }

        return {
            allowed: true,
            remaining: Math.max(0, limit - current.count),
            retryAfterSeconds: 0
        };
    }

    trim(now = this.now()) {
        if (this.entries.size <= this.maxEntries) return;

        for (const [key, entry] of this.entries) {
            if (entry.expiresAt <= now) this.entries.delete(key);
        }

        while (this.entries.size > this.maxEntries) {
            const oldestKey = this.entries.keys().next().value;
            if (oldestKey === undefined) break;
            this.entries.delete(oldestKey);
        }
    }
}
