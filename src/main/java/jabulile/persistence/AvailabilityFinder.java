package com.jabulile.booking.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

public class AvailabilityFinder {

    private final Connection connection;

    public AvailabilityFinder(Connection connection) {
        this.connection = connection;
    }

    public boolean isResourceAvailable(int resourceId, LocalDate start, LocalDate end) {
        String sql = """
            SELECT 1 FROM bookings
            WHERE resource_id = ?
            AND NOT (
                ? >= end_date
                OR ? <= start_date
            );
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, resourceId);
            stmt.setString(2, start.toString());
            stmt.setString(3, end.toString());

            ResultSet rs = stmt.executeQuery();
            return !rs.next(); // available if no overlap found
        } catch (Exception e) {
            throw new RuntimeException("Availability check failed", e);
        }
    }
}
