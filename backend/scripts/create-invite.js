import { createHash, randomBytes, randomUUID } from "node:crypto";
import pg from "pg";

const { Pool } = pg;

const usage = "Usage: node scripts/create-invite.js [maxUses] [expiresInHours]";
const maxUses = Number.parseInt(process.argv[2] ?? "1", 10);
const expiresInHours = Number.parseInt(process.argv[3] ?? "168", 10);

if (!Number.isInteger(maxUses) || maxUses < 1 || maxUses > 100) {
    throw new Error("maxUses must be an integer from 1 to 100");
}

if (!Number.isInteger(expiresInHours) || expiresInHours < 1 || expiresInHours > 8760) {
    throw new Error("expiresInHours must be an integer from 1 to 8760");
}

if (!process.env.DATABASE_URL) {
    throw new Error("DATABASE_URL is required");
}

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

try {
    const code = randomBytes(32).toString("base64url");
    const codeHash = createHash("sha256").update(code, "utf8").digest();
    const expiresAt = new Date(Date.now() + expiresInHours * 60 * 60 * 1000);

    await pool.query(
        `insert into invites (
            id, code_hash, max_uses, expires_at
        ) values ($1, $2, $3, $4)`,
        [randomUUID(), codeHash, maxUses, expiresAt]
    );

    console.log(`Invite: pauzechats://invite/${code}`);
    console.log(`Max uses: ${maxUses}`);
    console.log(`Expires: ${expiresAt.toISOString()}`);
} finally {
    await pool.end();
}
