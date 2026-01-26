package jabulile.httpapi;

import jabulile.rdbms.DatabaseManager;
import jabulile.persistence.ResourceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class ResourcesController {
    private DatabaseManager dbManager;
    private ObjectMapper objectMapper;
    
    public ResourcesController(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.objectMapper = new ObjectMapper();
    }
    
    public String handleRequest(String method, String path, String requestBody) {
        try {
            if ("GET".equals(method)) {
                if ("/api/resources".equals(path)) {
                    return getAllResources();
                } else if (path.startsWith("/api/resources/")) {
                    String id = path.substring("/api/resources/".length());
                    return getResourceById(id);
                }
            } else if ("POST".equals(method) && "/api/resources".equals(path)) {
                return createResource(requestBody);
            } else if ("PUT".equals(method) && path.startsWith("/api/resources/")) {
                String id = path.substring("/api/resources/".length());
                return updateResource(id, requestBody);
            } else if ("DELETE".equals(method) && path.startsWith("/api/resources/")) {
                String id = path.substring("/api/resources/".length());
                return deleteResource(id);
            }
            
            return "{\"error\": \"Not Found\"}";
            
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
    
    private String getAllResources() throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM resources ORDER BY name")) {
            
            List<ResourceEntity> resources = new ArrayList<>();
            while (rs.next()) {
                ResourceEntity resource = new ResourceEntity();
                resource.setId(rs.getInt("id"));
                resource.setName(rs.getString("name"));
                resource.setType(rs.getString("type"));
                resource.setCapacity(rs.getInt("capacity"));
                resource.setLocation(rs.getString("location"));
                resources.add(resource);
            }
            
            return objectMapper.writeValueAsString(resources);
        }
    }
    
    private String getResourceById(String id) throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM resources WHERE id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(id));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ResourceEntity resource = new ResourceEntity();
                    resource.setId(rs.getInt("id"));
                    resource.setName(rs.getString("name"));
                    resource.setType(rs.getString("type"));
                    resource.setCapacity(rs.getInt("capacity"));
                    resource.setLocation(rs.getString("location"));
                    return objectMapper.writeValueAsString(resource);
                } else {
                    return "{\"error\": \"Resource not found\"}";
                }
            }
        }
    }
    
    private String createResource(String requestBody) throws SQLException, IOException {
        Map<String, Object> resourceData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "INSERT INTO resources (name, type, capacity, location) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, (String) resourceData.get("name"));
            pstmt.setString(2, (String) resourceData.get("type"));
            pstmt.setInt(3, ((Number) resourceData.get("capacity")).intValue());
            pstmt.setString(4, (String) resourceData.get("location"));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", generatedKeys.getInt(1));
                        response.put("message", "Resource created successfully");
                        return objectMapper.writeValueAsString(response);
                    }
                }
            }
            
            return "{\"error\": \"Failed to create resource\"}";
        }
    }
    
    private String updateResource(String id, String requestBody) throws SQLException, IOException {
        Map<String, Object> resourceData = objectMapper.readValue(requestBody, new TypeReference<Map<String, Object>>() {});
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE resources SET name = ?, type = ?, capacity = ?, location = ? WHERE id = ?")) {
            
            pstmt.setString(1, (String) resourceData.get("name"));
            pstmt.setString(2, (String) resourceData.get("type"));
            pstmt.setInt(3, ((Number) resourceData.get("capacity")).intValue());
            pstmt.setString(4, (String) resourceData.get("location"));
            pstmt.setInt(5, Integer.parseInt(id));
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return "{\"message\": \"Resource updated successfully\"}";
            } else {
                return "{\"error\": \"Resource not found\"}";
            }
        }
    }
    
    private String deleteResource(String id) throws SQLException, IOException {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM resources WHERE id = ?")) {
            
            pstmt.setInt(1, Integer.parseInt(id));
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                return "{\"message\": \"Resource deleted successfully\"}";
            } else {
                return "{\"error\": \"Resource not found\"}";
            }
        }
    }
}