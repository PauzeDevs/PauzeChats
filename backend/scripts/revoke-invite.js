import { createHash } from "node:crypto";
import pg from "pg";

const { Pool } = pg;

const code = process.argv[2];

if (!code) {
    throw new Error("Usage: node scripts/revoke-invite.js <inviteCode>");
}

if (!process.env.DATABASE_URL) {
    throw new Error("DATABASE_URL is required");
}

const pool = new Pool({ connectionString: process.env.DATABASE_URL });

try {
    const result = await pool.query(
        `update invites
         set revoked_at = now()
         where code_hash = $1
           and revoked_at is null`,
        [createHash("sha256").update(code, "utf8").digest()]
    );

    if (result.rowCount !== 1) {
        throw new Error("Invite not found or already revoked");
    }

    console.log("Invite revoked.");
} finally {
    await pool.end();
}
