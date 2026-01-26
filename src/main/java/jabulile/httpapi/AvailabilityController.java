package jabulile.httpapi;

import jabulile.rdbms.DatabaseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AvailabilityController {
    private DatabaseManager dbManager;
    private ObjectMapper objectMapper;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public AvailabilityController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.objectMapper = new ObjectMapper();
    }
    
    public String handleRequest(String method, String path, String requestBody) {
        try {
            if ("GET".equals(method)) {
                if ("/api/availability".equals(path)) {
                    // Return all resources with current availability
                    return getAllAvailability();
                } else if (path.startsWith("/api/availability/")) {
                    // Check specific resource availability
                    String resourceId = path.substring("/api/availability/".length());
                    return getResourceAvailability(resourceId, requestBody);
                }
            } else if ("POST".equals(method) && "/api/availability".equals(path)) {
                // Check availability for a specific time period
                return checkAvailability(requestBody);
            }
            
            return "{\"error\": \"Not Found\"}";
            
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private String getAllAvailability() throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT r.*, " +
                 "(SELECT COUNT(*) FROM bookings b WHERE b.resource_id = r.id AND b.status = 'CONFIRMED' " +
                 "AND b.start_time <= CURRENT_TIMESTAMP AND b.end_time > CURRENT_TIMESTAMP) as current_bookings " +
                 "FROM resources r")) {
            
            List<Map<String, Object>> availabilityList = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> resourceAvailability = new HashMap<>();
                resourceAvailability.put("id", rs.getInt("id"));
                resourceAvailability.put("name", rs.getString("name"));
                resourceAvailability.put("type", rs.getString("type"));
                resourceAvailability.put("capacity", rs.getInt("capacity"));
                resourceAvailability.put("location", rs.getString("location"));
                
                int currentBookings = rs.getInt("current_bookings");
                boolean isAvailable = currentBookings == 0;
                int availableCapacity = rs.getInt("capacity") - currentBookings;
                
                resourceAvailability.put("isAvailable", isAvailable);
                resourceAvailability.put("currentBookings", currentBookings);
                resourceAvailability.put("availableCapacity", availableCapacity);
                
                availabilityList.add(resourceAvailability);
            }
            
            return objectMapper.writeValueAsString(availabilityList);
        }
    }
    
    private String getResourceAvailability(String resourceId, String requestBody) throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT r.*, " +
                 "(SELECT COUNT(*) FROM bookings b WHERE b.resource_id = r.id AND b.status = 'CONFIRMED' " +
                 "AND b.start_time <= CURRENT_TIMESTAMP AND b.end_time > CURRENT_TIMESTAMP) as current_bookings " +
                 "FROM resources r WHERE r.id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(resourceId));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> resourceAvailability = new HashMap<>();
                    resourceAvailability.put("id", rs.getInt("id"));
                    resourceAvailability.put("name", rs.getString("name"));
                    resourceAvailability.put("type", rs.getString("type"));
                    resourceAvailability.put("capacity", rs.getInt("capacity"));
                    resourceAvailability.put("location", rs.getString("location"));
                    
                    int currentBookings = rs.getInt("current_bookings");
                    boolean isAvailable = currentBookings == 0;
                    int availableCapacity = rs.getInt("capacity") - currentBookings;
                    
                    resourceAvailability.put("isAvailable", isAvailable);
                    resourceAvailability.put("currentBookings", currentBookings);
                    resourceAvailability.put("availableCapacity", availableCapacity);
                    
                    // Get upcoming bookings for this resource
                    List<Map<String, Object>> upcomingBookings = getUpcomingBookings(resourceId);
                    resourceAvailability.put("upcomingBookings", upcomingBookings);
                    
                    return objectMapper.writeValueAsString(resourceAvailability);
                } else {
                    return "{\"error\": \"Resource not found\"}";
                }
            }
        }
    }
    
    private String checkAvailability(String requestBody) throws SQLException, IOException {
        Map<String, Object> checkData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        
        Integer resourceId = (Integer) checkData.get("resourceId");
        LocalDateTime startTime = LocalDateTime.parse((String) checkData.get("startTime"), formatter);
        LocalDateTime endTime = LocalDateTime.parse((String) checkData.get("endTime"), formatter);
        
        try (Connection conn = dbManager.getConnection()) {
            List<Map<String, Object>> availableResources;
            
            if (resourceId != null) {
                // Check specific resource
                availableResources = checkSpecificResourceAvailability(conn, resourceId, startTime, endTime);
            } else {
                // Check all resources
                availableResources = checkAllResourcesAvailability(conn, startTime, endTime);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("availableResources", availableResources);
            response.put("startTime", startTime.format(formatter));
            response.put("endTime", endTime.format(formatter));
            
            return objectMapper.writeValueAsString(response);
        }
    }
    
    private List<Map<String, Object>> checkSpecificResourceAvailability(
        Connection conn, int resourceId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
    
    List<Map<String, Object>> result = new ArrayList<>();
    
    // Get resource details
    try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM resources WHERE id = ?")) {
        pstmt.setInt(1, resourceId);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                Map<String, Object> resourceInfo = new HashMap<>();
                resourceInfo.put("id", rs.getInt("id"));
                resourceInfo.put("name", rs.getString("name"));
                resourceInfo.put("type", rs.getString("type"));
                resourceInfo.put("capacity", rs.getInt("capacity"));
                resourceInfo.put("location", rs.getString("location"));
                
                // Check for conflicting bookings
                try (PreparedStatement conflictStmt = conn.prepareStatement(
                    "SELECT COUNT(*) as conflict_count FROM bookings " +
                    "WHERE resource_id = ? AND status = 'CONFIRMED' " +
                    "AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))")) {
                    
                    conflictStmt.setInt(1, resourceId);
                    conflictStmt.setTimestamp(2, Timestamp.valueOf(endTime.minusSeconds(1)));
                    conflictStmt.setTimestamp(3, Timestamp.valueOf(startTime));
                    conflictStmt.setTimestamp(4, Timestamp.valueOf(endTime));
                    conflictStmt.setTimestamp(5, Timestamp.valueOf(startTime.plusSeconds(1)));
                    
                    try (ResultSet conflictRs = conflictStmt.executeQuery()) {
                        if (conflictRs.next()) {
                            int conflictCount = conflictRs.getInt("conflict_count");
                            resourceInfo.put("isAvailable", conflictCount == 0);
                            resourceInfo.put("conflictCount", conflictCount);
                        }
                    }
                }
                
                result.add(resourceInfo);
            }
        }
    }
    
    return result;
}
    
    private List<Map<String, Object>> checkAllResourcesAvailability(
        Connection conn, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
    
    List<Map<String, Object>> result = new ArrayList<>();
    
    // Get all resources with availability status
    String sql = 
        "SELECT r.*, " +
        "(SELECT COUNT(*) FROM bookings b WHERE b.resource_id = r.id AND b.status = 'CONFIRMED' " +
        "AND ((b.start_time <= ? AND b.end_time > ?) OR (b.start_time < ? AND b.end_time >= ?))) as conflict_count " +
        "FROM resources r " +
        "ORDER BY r.name";
    
    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setTimestamp(1, Timestamp.valueOf(endTime.minusSeconds(1)));
        pstmt.setTimestamp(2, Timestamp.valueOf(startTime));
        pstmt.setTimestamp(3, Timestamp.valueOf(endTime));
        pstmt.setTimestamp(4, Timestamp.valueOf(startTime.plusSeconds(1)));
        
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> resourceInfo = new HashMap<>();
                resourceInfo.put("id", rs.getInt("id"));
                resourceInfo.put("name", rs.getString("name"));
                resourceInfo.put("type", rs.getString("type"));
                resourceInfo.put("capacity", rs.getInt("capacity"));
                resourceInfo.put("location", rs.getString("location"));
                
                int conflictCount = rs.getInt("conflict_count");
                resourceInfo.put("isAvailable", conflictCount == 0);
                resourceInfo.put("conflictCount", conflictCount);
                
                result.add(resourceInfo);
            }
        }
    }
    
    return result;
}
    
    private List<Map<String, Object>> getUpcomingBookings(String resourceId) throws SQLException {
        List<Map<String, Object>> upcomingBookings = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "SELECT b.*, r.name as resource_name FROM bookings b " +
                 "JOIN resources r ON b.resource_id = r.id " +
                 "WHERE b.resource_id = ? AND b.status = 'CONFIRMED' AND b.end_time > CURRENT_TIMESTAMP " +
                 "ORDER BY b.start_time")) {
            
            pstmt.setInt(1, Integer.parseInt(resourceId));
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> bookingInfo = new HashMap<>();
                    bookingInfo.put("id", rs.getInt("id"));
                    bookingInfo.put("userId", rs.getString("user_id"));
                    bookingInfo.put("resourceName", rs.getString("resource_name"));
                    
                    Timestamp startTimestamp = rs.getTimestamp("start_time");
                    Timestamp endTimestamp = rs.getTimestamp("end_time");
                    
                    bookingInfo.put("startTime", startTimestamp != null ? startTimestamp.toLocalDateTime().format(formatter) : null);
                    bookingInfo.put("endTime", endTimestamp != null ? endTimestamp.toLocalDateTime().format(formatter) : null);
                    bookingInfo.put("status", rs.getString("status"));
                    
                    upcomingBookings.add(bookingInfo);
                }
            }
        }
        
        return upcomingBookings;
    }
}