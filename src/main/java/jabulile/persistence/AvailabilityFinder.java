package jabulile.persistence;

import jabulile.rdbms.DatabaseManager;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvailabilityFinder {
    private DatabaseManager dbManager;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    public AvailabilityFinder(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    /**
     * Check if a specific resource is available for a given time period
     */
    public boolean isResourceAvailable(int resourceId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        String sql = "SELECT COUNT(*) as conflict_count FROM bookings " +
                    "WHERE resource_id = ? AND status = 'CONFIRMED' " +
                    "AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?))";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, resourceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(endTime.minusSeconds(1)));
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(startTime.plusSeconds(1)));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("conflict_count") == 0;
                }
            }
        }
        return false;
    }
    
    /**
     * Find all available resources for a given time period
     */
    public List<Map<String, Object>> findAvailableResources(LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        List<Map<String, Object>> availableResources = new ArrayList<>();
        
        String sql = 
            "SELECT r.*, " +
            "(SELECT COUNT(*) FROM bookings b WHERE b.resource_id = r.id AND b.status = 'CONFIRMED' " +
            "AND ((b.start_time <= ? AND b.end_time > ?) OR (b.start_time < ? AND b.end_time >= ?))) as conflict_count " +
            "FROM resources r " +
            "ORDER BY r.name";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
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
                    
                    availableResources.add(resourceInfo);
                }
            }
        }
        
        return availableResources;
    }
    
    /**
     * Find availability for a specific resource
     */
    public Map<String, Object> findResourceAvailability(int resourceId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        
        // Get resource details
        String resourceSql = "SELECT * FROM resources WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(resourceSql)) {
            
            pstmt.setInt(1, resourceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    result.put("id", rs.getInt("id"));
                    result.put("name", rs.getString("name"));
                    result.put("type", rs.getString("type"));
                    result.put("capacity", rs.getInt("capacity"));
                    result.put("location", rs.getString("location"));
                    
                    // Check availability
                    boolean isAvailable = isResourceAvailable(resourceId, startTime, endTime);
                    result.put("isAvailable", isAvailable);
                    
                    // Get conflicting bookings if any
                    if (!isAvailable) {
                        List<Map<String, Object>> conflicts = findConflictingBookings(resourceId, startTime, endTime);
                        result.put("conflictingBookings", conflicts);
                        result.put("conflictCount", conflicts.size());
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Find conflicting bookings for a resource and time period
     */
    public List<Map<String, Object>> findConflictingBookings(int resourceId, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        
        String sql = "SELECT * FROM bookings " +
                    "WHERE resource_id = ? AND status = 'CONFIRMED' " +
                    "AND ((start_time <= ? AND end_time > ?) OR (start_time < ? AND end_time >= ?)) " +
                    "ORDER BY start_time";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, resourceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(endTime.minusSeconds(1)));
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(startTime.plusSeconds(1)));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("id", rs.getInt("id"));
                    booking.put("userId", rs.getString("user_id"));
                    booking.put("startTime", rs.getTimestamp("start_time").toLocalDateTime());
                    booking.put("endTime", rs.getTimestamp("end_time").toLocalDateTime());
                    booking.put("status", rs.getString("status"));
                    conflicts.add(booking);
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Get upcoming bookings for a resource
     */
    public List<Map<String, Object>> getUpcomingBookings(int resourceId) throws SQLException {
        List<Map<String, Object>> upcomingBookings = new ArrayList<>();
        
        String sql = "SELECT b.*, r.name as resource_name FROM bookings b " +
                    "JOIN resources r ON b.resource_id = r.id " +
                    "WHERE b.resource_id = ? AND b.status = 'CONFIRMED' AND b.end_time > CURRENT_TIMESTAMP " +
                    "ORDER BY b.start_time";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, resourceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> booking = new HashMap<>();
                    booking.put("id", rs.getInt("id"));
                    booking.put("userId", rs.getString("user_id"));
                    booking.put("resourceName", rs.getString("resource_name"));
                    booking.put("startTime", rs.getTimestamp("start_time").toLocalDateTime());
                    booking.put("endTime", rs.getTimestamp("end_time").toLocalDateTime());
                    booking.put("status", rs.getString("status"));
                    upcomingBookings.add(booking);
                }
            }
        }
        
        return upcomingBookings;
    }
    
    /**
     * Get current availability status for all resources (now)
     */
    public List<Map<String, Object>> getCurrentAvailability() throws SQLException {
        List<Map<String, Object>> currentAvailability = new ArrayList<>();
        
        String sql = "SELECT r.*, " +
                    "(SELECT COUNT(*) FROM bookings b WHERE b.resource_id = r.id AND b.status = 'CONFIRMED' " +
                    "AND b.start_time <= CURRENT_TIMESTAMP AND b.end_time > CURRENT_TIMESTAMP) as current_bookings " +
                    "FROM resources r " +
                    "ORDER BY r.name";
        
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Map<String, Object> resourceInfo = new HashMap<>();
                resourceInfo.put("id", rs.getInt("id"));
                resourceInfo.put("name", rs.getString("name"));
                resourceInfo.put("type", rs.getString("type"));
                resourceInfo.put("capacity", rs.getInt("capacity"));
                resourceInfo.put("location", rs.getString("location"));
                
                int currentBookings = rs.getInt("current_bookings");
                boolean isAvailable = currentBookings == 0;
                int availableCapacity = rs.getInt("capacity") - currentBookings;
                
                resourceInfo.put("isAvailable", isAvailable);
                resourceInfo.put("currentBookings", currentBookings);
                resourceInfo.put("availableCapacity", availableCapacity);
                
                currentAvailability.add(resourceInfo);
            }
        }
        
        return currentAvailability;
    }
    
    /**
     * Find available time slots for a resource within a date range
     */
    public List<Map<String, Object>> findAvailableSlots(int resourceId, LocalDateTime date) throws SQLException {
        List<Map<String, Object>> availableSlots = new ArrayList<>();
        
        // Get bookings for the day
        LocalDateTime dayStart = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime dayEnd = date.withHour(23).withMinute(59).withSecond(59);
        
        String sql = "SELECT start_time, end_time FROM bookings " +
                    "WHERE resource_id = ? AND status = 'CONFIRMED' " +
                    "AND start_time >= ? AND end_time <= ? " +
                    "ORDER BY start_time";
        
        List<LocalDateTime[]> bookedSlots = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, resourceId);
            pstmt.setTimestamp(2, Timestamp.valueOf(dayStart));
            pstmt.setTimestamp(3, Timestamp.valueOf(dayEnd));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime slotStart = rs.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime slotEnd = rs.getTimestamp("end_time").toLocalDateTime();
                    bookedSlots.add(new LocalDateTime[]{slotStart, slotEnd});
                }
            }
        }
        
        // Generate available slots (assuming 1-hour slots from 8 AM to 6 PM)
        LocalDateTime currentSlot = dayStart.withHour(8).withMinute(0);
        LocalDateTime lastSlot = dayStart.withHour(18).withMinute(0);
        
        while (currentSlot.isBefore(lastSlot)) {
            LocalDateTime slotEnd = currentSlot.plusHours(1);
            boolean isAvailable = true;
            
            // Check if this slot conflicts with any booked slots
            for (LocalDateTime[] bookedSlot : bookedSlots) {
                if (isTimeOverlap(currentSlot, slotEnd, bookedSlot[0], bookedSlot[1])) {
                    isAvailable = false;
                    break;
                }
            }
            
            if (isAvailable) {
                Map<String, Object> slot = new HashMap<>();
                slot.put("startTime", currentSlot);
                slot.put("endTime", slotEnd);
                slot.put("duration", "1 hour");
                availableSlots.add(slot);
            }
            
            currentSlot = currentSlot.plusHours(1);
        }
        
        return availableSlots;
    }
    
    private boolean isTimeOverlap(LocalDateTime start1, LocalDateTime end1, 
                                  LocalDateTime start2, LocalDateTime end2) {
        return (start1.isBefore(end2) && end1.isAfter(start2));
    }
    
    /**
     * Parse date string to LocalDateTime
     */
    public LocalDateTime parseDateTime(String dateTimeStr) {
        return LocalDateTime.parse(dateTimeStr, formatter);
    }
    
    /**
     * Format LocalDateTime to string
     */
    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }
}