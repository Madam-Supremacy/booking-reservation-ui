package com.jabulile.booking.httpapi;

import com.jabulile.booking.persistence.DataLoader;
import com.jabulile.booking.web.WebServer;

public class BookingAppServer {

    public static void main(String[] args) {
        //Initialize DB
        DataLoader loader = new DataLoader();
        loader.initializeDatabase();

        //Start Web Server
        WebServer server = new WebServer(8080);
        server.start();

        System.out.println("Booking & Reservation System running on port 8080");
    }
}
