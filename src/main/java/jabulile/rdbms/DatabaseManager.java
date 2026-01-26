package jabulile.rdbms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String URL = "jdbc:h2:mem:bookingdb;DB_CLOSE_DELAY=-1;INIT=RUNSCRIPT FROM 'classpath:sql/create_resources.sql'\\;RUNSCRIPT FROM 'classpath:sql/create_bookings.sql'";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    
    // Don't use static initializer, load driver when needed
    private static boolean driverLoaded = false;
    
    private static void loadDriver() {
        if (!driverLoaded) {
            try {
                Class.forName("org.h2.Driver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                System.err.println("H2 Driver not found. Please ensure H2 dependency is added to pom.xml");
                System.err.println("You can add it with: <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><version>2.1.214</version></dependency>");
                // Don't throw exception, let it fail gracefully
            }
        }
    }
    
    public Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    
    public void initializeDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection established successfully");
            // Initialize data
            initializeData(conn);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            // Continue without database - for demo purposes
        }
    }
    
    private void initializeData(Connection conn) throws SQLException {
        // Create initial resources if table is empty
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM resources")) {
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("Loading initial data...");
                // Insert sample resources
                String[] insertSQL = {
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Conference Room A', 'Room', 20, 'Building 1, Floor 3')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Conference Room B', 'Room', 15, 'Building 1, Floor 3')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Meeting Room 101', 'Room', 8, 'Building 2, Floor 1')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Training Room', 'Room', 30, 'Building 2, Floor 2')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Projector 1', 'Equipment', 1, 'Building 1, IT Department')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Laptop 1', 'Equipment', 1, 'Building 2, IT Department')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Auditorium', 'Venue', 100, 'Main Building')",
                    "INSERT INTO resources (name, type, capacity, location) VALUES ('Cafeteria', 'Venue', 50, 'Ground Floor')"
                };
                
                for (String sql : insertSQL) {
                    stmt.execute(sql);
                }
                System.out.println("Initial data loaded successfully");
            }
        }
    }
}