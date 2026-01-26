package com.jabulile.booking.httpapi;

import com.jabulile.booking.persistence.ResourceEntity;
import com.jabulile.booking.rdbms.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ResourcesController {

    private final Connection connection;

    public ResourcesController() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    // ------------------------
    // Get all resources
    // ------------------------
    public JSONArray getAllResources() {
        JSONArray resources = new JSONArray();
        try {
            String sql = "SELECT id, name, type, description FROM resources ORDER BY name";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    JSONObject res = new JSONObject();
                    res.put("id", rs.getInt("id"));
                    res.put("name", rs.getString("name"));
                    res.put("type", rs.getString("type"));
                    res.put("description", rs.getString("description"));
                    resources.put(res);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resources;
    }

    // ------------------------
    // Create a new resource
    // ------------------------
    public JSONObject createResource(JSONObject requestBody) {
        JSONObject response = new JSONObject();
        try {
            String name = requestBody.getString("name");
            String type = requestBody.getString("type");
            String description = requestBody.optString("description", "");

            String sql = "INSERT INTO resources (name, type, description) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, name);
                stmt.setString(2, type);
                stmt.setString(3, description);
                stmt.executeUpdate();
            }

            response.put("status", 201);
            response.put("message", "Resource created successfully");
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 500);
            response.put("error", "Failed to create resource");
        }
        return response;
    }
}
