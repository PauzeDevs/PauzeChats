import test from "node:test";
import assert from "node:assert/strict";
import { createServer } from "../src/server.js";

test("health endpoint returns service status", async (t) => {
    const server = createServer();
    await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
    t.after(() => server.close());

    const address = server.address();
    assert.ok(address && typeof address === "object");

    const response = await fetch(`http://127.0.0.1:${address.port}/healthz`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
        ok: true,
        service: "pauzechats-api"
    });
});
