package com.jabulile.booking.web;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class WebServer {

    private final int port;

    public WebServer(int port) {
        this.port = port;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", exchange -> {
                String response = "Booking & Reservation System";
                exchange.sendResponseHeaders(200, response.length());
                exchange.getResponseBody().write(response.getBytes());
                exchange.close();
            });
            server.start();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start web server", e);
        }
    }
}
