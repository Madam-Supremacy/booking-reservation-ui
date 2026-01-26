package jabulile.persistence;

import java.time.LocalDateTime;

public class BookingEntity {
    private int id;
    private int resourceId;
    private String userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    
    public BookingEntity() {
    }
    
    public BookingEntity(int resourceId, String userId, LocalDateTime startTime, LocalDateTime endTime) {
        this.resourceId = resourceId;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "CONFIRMED";
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getResourceId() {
        return resourceId;
    }
    
    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public LocalDateTime getEndTime() {
        return endTime;
    }
    
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "BookingEntity{" +
                "id=" + id +
                ", resourceId=" + resourceId +
                ", userId='" + userId + '\'' +
                ", startTime=" + (startTime != null ? startTime.toString() : "null") +
                ", endTime=" + (endTime != null ? endTime.toString() : "null") +
                ", status='" + status + '\'' +
                '}';
    }
}