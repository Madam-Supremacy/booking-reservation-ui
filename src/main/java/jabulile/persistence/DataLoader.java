package com.jabulile.booking.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DataLoader {

    private static final String DB_FILE = "test.db";

    private final Connection connection;

    public DataLoader() {
        this.connection = connect();
    }

    // Connect to SQLite
    private Connection connect() {
        try {
            String url = "jdbc:sqlite:" + DB_FILE;
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to DB", e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    // Load SQL from a file
    private String loadSQL(String fileName) {
        try {
            Path path = Path.of("src/main/resources/sql", fileName);
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read SQL file: " + fileName, e);
        }
    }

    // Run SQL statements
    private void executeSQL(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute SQL", e);
        }
    }

    // Initialize database (tables + sample data)
    public void initializeDatabase() {
        System.out.println("Initializing database...");

        // 1️⃣ Create tables
        executeSQL(loadSQL("create_resources.sql"));
        executeSQL(loadSQL("create_bookings.sql"));

        // 2️⃣ Insert sample resources
        executeSQL(loadSQL("insert_resources.sql"));

        System.out.println("Database initialized successfully!");
    }

    // Close connection (optional)
    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
