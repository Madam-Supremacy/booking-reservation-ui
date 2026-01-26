package com.jabulile.booking.httpapi;

import com.jabulile.booking.persistence.AvailabilityFinder;
import com.jabulile.booking.rdbms.DatabaseManager;

import java.sql.Connection;
import java.time.LocalDate;

import org.json.JSONObject;

public class AvailabilityController {

    private final AvailabilityFinder availabilityFinder;

    public AvailabilityController() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        this.availabilityFinder = new AvailabilityFinder(conn);
    }

    /**
     * Check availability of a resource given start and end dates.
     * Query string format: resourceId=1&start=YYYY-MM-DD&end=YYYY-MM-DD
     */
    public JSONObject getAvailability(String query) {
        JSONObject response = new JSONObject();

        try {
            // Parse query params
            String[] parts = query.split("&");
            int resourceId = -1;
            LocalDate startDate = null;
            LocalDate endDate = null;

            for (String part : parts) {
                String[] kv = part.split("=");
                switch (kv[0]) {
                    case "resourceId": resourceId = Integer.parseInt(kv[1]); break;
                    case "start": startDate = LocalDate.parse(kv[1]); break;
                    case "end": endDate = LocalDate.parse(kv[1]); break;
                }
            }

            if (resourceId == -1 || startDate == null || endDate == null) {
                response.put("status", 400);
                response.put("error", "Missing or invalid parameters");
                return response;
            }

            boolean available = availabilityFinder.isResourceAvailable(resourceId, startDate, endDate);
            response.put("status", 200);
            response.put("resourceId", resourceId);
            response.put("available", available);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", 500);
            response.put("error", "Failed to check availability");
        }

        return response;
    }
}
