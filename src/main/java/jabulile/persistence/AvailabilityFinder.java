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

    /**
     * Check if a specific resource is available between startDate and endDate.
     *
     * @param resourceId The resource to check
     * @param startDate  Start date (inclusive)
     * @param endDate    End date (exclusive)
     * @return true if available, false if overlapping booking exists
     */
    public boolean isResourceAvailable(int resourceId, LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT COUNT(*) AS overlap_count " +
                     "FROM bookings " +
                     "WHERE resource_id = ? " +
                     "AND NOT (? >= end_date OR ? <= start_date)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, resourceId);
            stmt.setString(2, startDate.toString());
            stmt.setString(3, endDate.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("overlap_count");
                    return count == 0; // Available if no overlapping bookings
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false; // In case of error, consider resource unavailable
    }
}
