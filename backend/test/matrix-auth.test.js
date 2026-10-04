/*
 * Copyright © 2026 Aarav Singh (Pauze). All rights reserved.
 */

import assert from "node:assert/strict";
import test from "node:test";
import { jwtVerify } from "jose";
import { createMatrixTokenService } from "../src/matrix-auth.js";

const secret = "matrix-jwt-test-secret-0123456789abcdef";
const issuer = "pauzechats";
const audience = "pauzechats-matrix";

test("matrix token is short lived and audience bound", async () => {
    const service = createMatrixTokenService({
        matrixJwtSecret: secret,
        issuer,
        audience,
        ttlSeconds: 60
    });

    const token = await service.issueToken({
        username: "aarav",
        displayName: "Aarav Singh"
    });

    const verified = await jwtVerify(
        token,
        new TextEncoder().encode(secret),
        {
            algorithms: ["HS256"],
            issuer,
            audience
        }
    );

    assert.equal(verified.payload.sub, "aarav");
    assert.equal(verified.payload.display_name, "Aarav Singh");
    assert.equal(verified.payload.iss, issuer);
    assert.deepEqual(verified.payload.aud, [audience]);
    assert.ok(
        typeof verified.payload.exp === "number" &&
        typeof verified.payload.iat === "number"
    );
    assert.equal(verified.payload.exp - verified.payload.iat, 60);
});

test("matrix token service rejects invalid TTL configuration", () => {
    assert.throws(
        () => createMatrixTokenService({
            matrixJwtSecret: secret,
            issuer,
            audience,
            ttlSeconds: 301
        }),
        /between 30 and 300 seconds/
    );
});
