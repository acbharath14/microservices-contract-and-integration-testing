package com.bharath.qa.integration;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class ProviderServer {

    private final HttpServer server;
    private final int port;

    private ProviderServer(HttpServer server, int port) {
        this.server = server;
        this.port = port;
    }

    static ProviderServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/orders/123", ProviderServer::handleOrderLookup);
        server.start();
        return new ProviderServer(server, server.getAddress().getPort());
    }

    int port() {
        return port;
    }

    void stop() {
        server.stop(0);
    }

    private static void handleOrderLookup(HttpExchange exchange) throws IOException {
        byte[] response = "{\"orderId\":123,\"status\":\"APPROVED\",\"customerTier\":\"GOLD\"}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
        }
    }
}
