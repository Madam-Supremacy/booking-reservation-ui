package com.jabulile.booking.rdbms;

import com.jabulile.booking.persistence.DataLoader;

import java.sql.Connection;

public class DatabaseManager {

    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() {
        DataLoader loader = new DataLoader();
        loader.initializeDatabase();  // Ensure tables exist and sample data is loaded
        this.connection = loader.getConnection();
    }

    // Singleton access
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    // Close DB (optional, on shutdown)
    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
