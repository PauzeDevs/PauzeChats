import pg from "pg";

const { Pool } = pg;

let pool;

export function getPool() {
    if (!process.env.DATABASE_URL) {
        return null;
    }

    if (!pool) {
        pool = new Pool({
            connectionString: process.env.DATABASE_URL,
            max: Number.parseInt(process.env.DB_POOL_MAX ?? "10", 10)
        });
    }

    return pool;
}
