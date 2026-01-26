package com.jabulile.booking.httpapi;

import com.jabulile.booking.web.WebServer;

public class BookingAppServer {

    public static void main(String[] args) {
        WebServer server = new WebServer(8080);
        server.start();

        System.out.println("Booking & Reservation System running on port 8080");
    }
}
