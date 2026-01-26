package com.jabulile.booking.web;

import com.jabulile.booking.httpapi.BookingsController;
import com.jabulile.booking.httpapi.ResourcesController;
import com.jabulile.booking.httpapi.AvailabilityController;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetSocketAddress;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.io.InputStream;

import org.json.JSONObject;
import org.json.JSONArray;

public class WebServer {

    private final int port;
    private HttpServer server;

    private final ResourcesController resourcesController;
    private final BookingsController bookingsController;
    private final AvailabilityController availabilityController;

    public WebServer(int port) throws Exception {
        this.port = port;

        resourcesController = new ResourcesController();
        bookingsController = new BookingsController();
        availabilityController = new AvailabilityController();
    }

    public void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // ---------------------------
        // Resources endpoints
        // ---------------------------
        server.createContext("/api/resources", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                JSONArray json = resourcesController.getAllResources();
                sendJson(exchange, json.toString(), 200);
            } else if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject request = parseRequest(exchange);
                JSONObject response = resourcesController.createResource(request);
                sendJson(exchange, response.toString(), response.getInt("status"));
            } else {
                sendJson(exchange, "{}", 405);
            }
        });

        // ---------------------------
        // Availability endpoint
        // ---------------------------
        server.createContext("/api/availability", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                JSONObject response = availabilityController.getAvailability(exchange.getRequestURI().getQuery());
                sendJson(exchange, response.toString(), 200);
            } else {
                sendJson(exchange, "{}", 405);
            }
        });

        // ---------------------------
        // Bookings endpoints
        // ---------------------------
        server.createContext("/api/bookings", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                JSONObject request = parseRequest(exchange);
                JSONObject response = bookingsController.createBooking(request);
                sendJson(exchange, response.toString(), response.getInt("status"));
            } else if ("GET".equals(exchange.getRequestMethod())) {
                JSONArray history = bookingsController.getBookingHistory();
                sendJson(exchange, history.toString(), 200);
            } else {
                sendJson(exchange, "{}", 405);
            }
        });

        // ---------------------------
