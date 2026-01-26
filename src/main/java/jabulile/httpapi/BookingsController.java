package jabulile.httpapi;

import jabulile.rdbms.DatabaseManager;
import jabulile.persistence.BookingEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class BookingsController {
    private DatabaseManager dbManager;
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public BookingsController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.objectMapper = new ObjectMapper();
        // Remove this line: this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    public String handleRequest(String method, String path, String requestBody) {
        try {
            if ("GET".equals(method)) {
                if ("/api/bookings".equals(path)) {
                    return getAllBookings();
                } else if (path.startsWith("/api/bookings/")) {
                    String id = path.substring("/api/bookings/".length());
                    return getBookingById(id);
                } else if (path.contains("history")) {
                    // Handle booking history endpoint
                    return getBookingHistory(requestBody);
                }
            } else if ("POST".equals(method)) {
                if ("/api/bookings".equals(path)) {
                    return createBooking(requestBody);
                } else if (path.contains("cancel")) {
                    return cancelBooking(requestBody);
                }
            } else if ("PUT".equals(method) && path.startsWith("/api/bookings/")) {
                String id = path.substring("/api/bookings/".length());
                return updateBooking(id, requestBody);
            } else if ("DELETE".equals(method) && path.startsWith("/api/bookings/")) {
                String id = path.substring("/api/bookings/".length());
                return deleteBooking(id);
            }
            
            return "{\"error\": \"Not Found\"}";
            
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private String getAllBookings() throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM bookings ORDER BY start_time DESC")) {
            
            List<Map<String, Object>> bookings = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("id", rs.getInt("id"));
                booking.put("resourceId", rs.getInt("resource_id"));
                booking.put("userId", rs.getString("user_id"));
                
                Timestamp startTimestamp = rs.getTimestamp("start_time");
                Timestamp endTimestamp = rs.getTimestamp("end_time");
                
                // Format dates as strings for JSON
                booking.put("startTime", startTimestamp != null ? 
                    startTimestamp.toLocalDateTime().format(formatter) : null);
                booking.put("endTime", endTimestamp != null ? 
                    endTimestamp.toLocalDateTime().format(formatter) : null);
                booking.put("status", rs.getString("status"));
                
                bookings.add(booking);
            }
            
            return objectMapper.writeValueAsString(bookings);
        }
    }
    
    private String getBookingById(String id) throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM bookings WHERE id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("id", rs.getInt("id"));
                    booking.put("resourceId", rs.getInt("resource_id"));
                    booking.put("userId", rs.getString("user_id"));
                    
                    Timestamp startTimestamp = rs.getTimestamp("start_time");
                    Timestamp endTimestamp = rs.getTimestamp("end_time");
                    
                    // Format dates as strings for JSON
                    booking.put("startTime", startTimestamp != null ? 
                        startTimestamp.toLocalDateTime().format(formatter) : null);
                    booking.put("endTime", endTimestamp != null ? 
                        endTimestamp.toLocalDateTime().format(formatter) : null);
                    booking.put("status", rs.getString("status"));
                    
                    return objectMapper.writeValueAsString(booking);
                } else {
                    return "{\"error\": \"Booking not found\"}";
                }
            }
        }
    }
    
    private String createBooking(String requestBody) throws SQLException, IOException {
        Map<String, Object> bookingData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        
        // Check if resource is available
        if (!isResourceAvailable(bookingData)) {
            return "{\"error\": \"Resource not available for the requested time slot\"}";
        }
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO bookings (resource_id, user_id, start_time, end_time, status) VALUES (?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, ((Number) bookingData.get("resourceId")).intValue());
            pstmt.setString(2, (String) bookingData.get("userId"));
            
            LocalDateTime startTime = LocalDateTime.parse((String) bookingData.get("startTime"), formatter);
            LocalDateTime endTime = LocalDateTime.parse((String) bookingData.get("endTime"), formatter);
            
            // Convert LocalDateTime to Timestamp
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setString(5, "CONFIRMED");
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", generatedKeys.getInt(1));
                        response.put("message", "Booking created successfully");
                        response.put("status", "CONFIRMED");
                        return objectMapper.writeValueAsString(response);
                    }
                }
            }
            
            return "{\"error\": \"Failed to create booking\"}";
        }
    }
    
    private boolean isResourceAvailable(Map<String, Object> bookingData) throws SQLException {
        int resourceId = ((Number) bookingData.get("resourceId")).intValue();
        LocalDateTime startTime = LocalDateTime.parse((String) bookingData.get("startTime"), formatter);
        LocalDateTime endTime = LocalDateTime.parse((String) bookingData.get("endTime"), formatter);
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT COUNT(*) FROM bookings WHERE resource_id = ? AND status = 'CONFIRMED' " +
                 "AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?) OR (start_time >= ? AND end_time <= ?))")) {
            
            pstmt.setInt(1, resourceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(endTime.minusSeconds(1)));
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(startTime.plusSeconds(1)));
            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        
        return false;
    }
    
    private String updateBooking(String id, String requestBody) throws SQLException, IOException {
        Map<String, Object> bookingData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE bookings SET resource_id = ?, user_id = ?, start_time = ?, end_time = ?, status = ? WHERE id = ?")) {
            
            pstmt.setInt(1, ((Number) bookingData.get("resourceId")).intValue());
            pstmt.setString(2, (String) bookingData.get("userId"));
            
            LocalDateTime startTime = LocalDateTime.parse((String) bookingData.get("startTime"), formatter);
            LocalDateTime endTime = LocalDateTime.parse((String) bookingData.get("endTime"), formatter);
            
            // Convert LocalDateTime to Timestamp
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setString(5, (String) bookingData.get("status"));
            pstmt.setInt(6, Integer.parseInt(id));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return "{\"message\": \"Booking updated successfully\"}";
            } else {
                return "{\"error\": \"Booking not found\"}";
            }
        }
    }
    
    private String deleteBooking(String id) throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM bookings WHERE id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(id));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return "{\"message\": \"Booking deleted successfully\"}";
            } else {
                return "{\"error\": \"Booking not found\"}";
            }
        }
    }
    
    private String cancelBooking(String requestBody) throws SQLException, IOException {
        Map<String, Object> cancelData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        int bookingId = ((Number) cancelData.get("bookingId")).intValue();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("UPDATE bookings SET status = 'CANCELLED' WHERE id = ?")) {
            
            pstmt.setInt(1, bookingId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return "{\"message\": \"Booking cancelled successfully\"}";
            } else {
                return "{\"error\": \"Booking not found\"}";
            }
        }
    }
    
    private String getBookingHistory(String requestBody) throws SQLException, IOException {
        Map<String, Object> historyData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        String userId = (String) historyData.get("userId");
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT * FROM bookings WHERE user_id = ? ORDER BY start_time DESC")) {
            
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Map<String, Object>> bookings = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("id", rs.getInt("id"));
                    booking.put("resourceId", rs.getInt("resource_id"));
                    booking.put("userId", rs.getString("user_id"));
                    
                    Timestamp startTimestamp = rs.getTimestamp("start_time");
                    Timestamp endTimestamp = rs.getTimestamp("end_time");
                    
                    // Format dates as strings for JSON
                    booking.put("startTime", startTimestamp != null ? 
                        startTimestamp.toLocalDateTime().format(formatter) : null);
                    booking.put("endTime", endTimestamp != null ? 
                        endTimestamp.toLocalDateTime().format(formatter) : null);
                    booking.put("status", rs.getString("status"));
                    
                    bookings.add(booking);
                }
                
                return objectMapper.writeValueAsString(bookings);
            }
        }
    }
}