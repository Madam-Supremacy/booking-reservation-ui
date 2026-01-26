package com.jabulile.booking.httpapi;

import com.jabulile.booking.persistence.AvailabilityFinder;
import com.jabulile.booking.rdbms.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class BookingsController {

    private final Connection connection;
    private final AvailabilityFinder availabilityFinder;

    public BookingsController() {
        this.connection = DatabaseManager.getInstance().getConnection();
        this.availabilityFinder = new AvailabilityFinder(connection);
    }

    // -----------------------
    // Create a booking
    // -----------------------
    public JSONObject createBooking(JSONObject requestBody) {
        JSONObject response = new JSONObject();

        try {
            int resourceId = requestBody.getInt("resourceId");
            String bookedBy = requestBody.getString("bookedBy");
            LocalDate startDate = LocalDate.parse(requestBody.getString("startDate"));
            LocalDate endDate = LocalDate.parse(requestBody.getString("endDate"));

            // 1️⃣ Validate dates
            if (!startDate.isBefore(endDate)) {
                response.put("status", 400);
                response.put("error", "Start date must be before end date");
                return response;
            }

            // 2️⃣ Check availability
            boolean available = availabilityFinder.isResourceAvailable(resourceId, startDate, endDate);
            if (!available) {
                response.put("status", 409);
                response.put("error", "Resource is not available for the selected dates");
                return response;
            }

            // 3️⃣ Insert booking
            String sql = "INSERT INTO bookings (resource_id, start_date, end_date, booked_by) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setInt(1, resourceId);
                stmt.setString(2, startDate.toString());
                stmt.setString(3, endDate.toString());
                stmt.setString(4, bookedBy);
                stmt.executeUpdate();
            }

            response.put("status", 201);
            response.put("message", "Booking created successfully");
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 500);
            response.put("error", "Internal server error");
            return response;
        }
    }

    // -----------------------
    // Get booking history
    // -----------------------
    public JSONArray getBookingHistory() {
        JSONArray history = new JSONArray();

        try {
            String sql = "SELECT b.id AS booking_id, r.name AS resource_name, r.type AS resource_type, " +
                    "b.start_date, b.end_date, b.booked_by, b.created_at " +
                    "FROM bookings b " +
                    "JOIN resources r ON b.resource_id = r.id " +
                    "ORDER BY b.start_date DESC";

            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    JSONObject booking = new JSONObject();
                    booking.put("bookingId", rs.getInt("booking_id"));
                    booking.put("resourceName", rs.getString("resource_name"));
                    booking.put("resourceType", rs.getString("resource_type"));
                    booking.put("startDate", rs.getString("start_date"));
                    booking.put("endDate", rs.getString("end_date"));
                    booking.put("bookedBy", rs.getString("booked_by"));
                    booking.put("createdAt", rs.getString("created_at"));

                    history.put(booking);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return history;
    }
}
