package jabulile.persistence;

import jabulile.rdbms.DatabaseManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class DataLoader {
    private DatabaseManager dbManager;
    
    public DataLoader() {
        this.dbManager = new DatabaseManager();
        initializeDatabase();
    }
    
    public DatabaseManager getDatabaseManager() {
        return dbManager;
    }
    
    private void initializeDatabase() {
        try {
            // Create tables
            executeSQLScript("create_resources.sql");
            executeSQLScript("create_bookings.sql");
            
            // Load initial data
            executeSQLScript("insert_resources.sql");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
        }
    }
    
    private void executeSQLScript(String scriptName) {
        try (Connection conn = dbManager.getConnection();
             InputStream is = getClass().getClassLoader().getResourceAsStream("sql/" + scriptName)) {
            
            if (is == null) {
                System.err.println("Script not found: " + scriptName);
                return;
            }
            
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                scanner.useDelimiter(";");
                
                try (Statement stmt = conn.createStatement()) {
                    while (scanner.hasNext()) {
                        String sql = scanner.next().trim();
                        if (!sql.isEmpty() && !sql.startsWith("--")) {
                            stmt.execute(sql);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error executing script " + scriptName + ": " + e.getMessage());
        }
    }
}