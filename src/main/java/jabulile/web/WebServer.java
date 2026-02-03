package jabulile.web;

import jabulile.httpapi.ResourcesController;
import jabulile.httpapi.BookingsController;
import jabulile.httpapi.AvailabilityController;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WebServer {
    private HttpServer server;
    private int port;

    public WebServer(int port) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
    }

    public void addController(String path, Object controller) {
        server.createContext(path, new ApiHandler(controller));
    }

    public void start() {
        // Serve static files
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("WebServer running on port " + port);
    }

    public void stop() {
        server.stop(0);
    }

    // =========================
    // API HANDLER
    // =========================
    private static class ApiHandler implements HttpHandler {
        private Object controller;

        public ApiHandler(Object controller) {
            this.controller = controller;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {

            // ---- CORS HEADERS (for Netlify) ----
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // ---- Handle preflight requests ----
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();

                // Read request body (if any)
                String requestBody = "";
                if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                    try (var reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(exchange.getRequestBody()))) {
                        StringBuilder body = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            body.append(line);
                        }
                        requestBody = body.toString();
                    }
                }

                // Route to controller
                String response = "";

                if (controller instanceof ResourcesController) {
                    response = ((ResourcesController) controller)
                            .handleRequest(method, path, requestBody);

                } else if (controller instanceof BookingsController) {
                    response = ((BookingsController) controller)
                            .handleRequest(method, path, requestBody);

                } else if (controller instanceof AvailabilityController) {
                    response = ((AvailabilityController) controller)
                            .handleRequest(method, path, requestBody);
                }

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

            } catch (Exception e) {
                String errorResponse = "{\"error\": \"" + e.getMessage() + "\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(500, errorResponse.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(errorResponse.getBytes());
                }
            }
        }
    }

    // =========================
    // STATIC FILE HANDLER
    // =========================
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path)) {
                path = "/index.html";
            }

            try {
                // Try classpath (Render / JAR)
                Path filePath = Paths.get("target/classes/html" + path);

                // Fallback for local dev
                if (!Files.exists(filePath)) {
                    filePath = Paths.get("src/main/resources/html" + path);
                }

                if (Files.exists(filePath)) {
                    byte[] fileBytes = Files.readAllBytes(filePath);

                    String contentType = "text/html";
                    if (path.endsWith(".css")) contentType = "text/css";
                    else if (path.endsWith(".js")) contentType = "application/javascript";
                    else if (path.endsWith(".png")) contentType = "image/png";
                    else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) contentType = "image/jpeg";

                    exchange.getResponseHeaders().set("Content-Type", contentType);
                    exchange.sendResponseHeaders(200, fileBytes.length);

                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(fileBytes);
                    }
                } else {
                    String notFound = "<html><body><h1>404 Not Found</h1></body></html>";
                    exchange.sendResponseHeaders(404, notFound.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound.getBytes());
                    }
                }

            } catch (Exception e) {
                String error = "<html><body><h1>500 Internal Server Error</h1><p>"
                        + e.getMessage() + "</p></body></html>";
                exchange.sendResponseHeaders(500, error.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes());
                }
            }
        }
    }
}
