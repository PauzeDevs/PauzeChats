import http from "node:http";

const port = Number.parseInt(process.env.PORT ?? "8080", 10);

if (!Number.isInteger(port) || port < 1 || port > 65535) {
    throw new Error("PORT must be a valid TCP port");
}

export function createServer() {
    return http.createServer((request, response) => {
        if (request.method === "GET" && request.url === "/healthz") {
            response.writeHead(200, { "content-type": "application/json; charset=utf-8" });
            response.end(JSON.stringify({ ok: true, service: "pauzechats-api" }));
            return;
        }

        response.writeHead(404, { "content-type": "application/json; charset=utf-8" });
        response.end(JSON.stringify({ error: "not_found" }));
    });
}

if (import.meta.url === `file://${process.argv[1]}`) {
    const server = createServer();
    server.listen(port, "0.0.0.0", () => {
        console.log(`PauzeChats API listening on port ${port}`);
    });
}
