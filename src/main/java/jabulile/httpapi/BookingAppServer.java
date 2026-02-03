package jabulile.httpapi;

import jabulile.persistence.DataLoader;
import jabulile.rdbms.DatabaseManager;
import jabulile.web.WebServer;

public class BookingAppServer {
    public static void main(String[] args) {
        try {
            // Initialize database
            DataLoader dataLoader = new DataLoader();
            DatabaseManager dbManager = dataLoader.getDatabaseManager();
            
            // Create controllers
            ResourcesController resourcesController = new ResourcesController(dbManager);
            BookingsController bookingsController = new BookingsController(dbManager);
            AvailabilityController availabilityController = new AvailabilityController(dbManager);
            
            // Create and start web server
            int port = Integer.parseInt(
                System.getenv().getOrDefault("PORT", "8080")
            );

            WebServer webServer = new WebServer(port);


            webServer.addController("/api/resources", resourcesController);
            webServer.addController("/api/bookings", bookingsController);
            webServer.addController("/api/availability", availabilityController);
            
            System.out.println("Booking App Server started on port " + port);
            System.out.println("Access the application at: http://localhost:" + port);

            System.out.println("API endpoints:");
            System.out.println("  GET /api/resources");
            System.out.println("  POST /api/resources");
            System.out.println("  GET /api/bookings");
            System.out.println("  POST /api/bookings");
            System.out.println("  GET /api/availability");
            
            webServer.start();
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}