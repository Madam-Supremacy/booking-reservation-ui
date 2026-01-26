package jabulile.persistence;

import jabulile.rdbms.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Tables {

    private final DatabaseManager dbManager;

    public Tables(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void createResourcesTable() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS resources (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "type TEXT NOT NULL" +
                    ");";
            stmt.execute(sql);
        }
    }

    public void createBookingsTable() throws SQLException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS bookings (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "resource_id INTEGER NOT NULL," +
                    "start_time TEXT NOT NULL," +
                    "end_time TEXT NOT NULL," +
                    "booked_by TEXT NOT NULL," +
                    "FOREIGN KEY(resource_id) REFERENCES resources(id)" +
                    ");";
            stmt.execute(sql);
        }
    }
}
