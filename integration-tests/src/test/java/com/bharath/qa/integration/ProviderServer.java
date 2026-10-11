package com.bharath.qa.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * In-process stub of the three provider services. Serves the happy-path
 * payloads from {@code demo/contract_demo.py} so contract tests run with
 * no external dependencies.
 */
final class ProviderServer {

    private static final Map<String, String> ROUTES = Map.of(
            "/orders/123",
            "{\"orderId\":123,\"status\":\"APPROVED\",\"customerTier\":\"GOLD\"}",
            "/underwriting/123",
            "{\"orderId\":123,\"decision\":\"APPROVED\",\"riskBand\":\"LOW\"}",
            "/notifications/123",
            "{\"orderId\":123,\"channel\":\"EMAIL\",\"deliveryState\":\"QUEUED\"}"
    );

    private final HttpServer server;
    private final int port;

    private ProviderServer(HttpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    static ProviderServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        for (Map.Entry<String, String> route : ROUTES.entrySet()) {
            byte[] body = route.getValue().getBytes(StandardCharsets.UTF_8);
            server.createContext(route.getKey(), exchange -> handle(exchange, body));
        }
        server.start();
        return new ProviderServer(server, server.getAddress().getPort());
    }

    int port() {
        return port;
    }

    void stop() {
        server.stop(0);
    }

    private static void handle(HttpExchange exchange, byte[] body) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        }
    }
}
