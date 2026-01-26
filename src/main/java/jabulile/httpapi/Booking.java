package jabulile.httpapi;

import java.time.LocalDate;

public class Booking {
    private int id;
    private int resourceId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String bookedBy;

    public Booking(int id, int resourceId, LocalDate startDate, LocalDate endDate, String bookedBy) {
        this.id = id;
        this.resourceId = resourceId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.bookedBy = bookedBy;
    }

    public int getId() {
        return id;
    }

    public int getResourceId() {
        return resourceId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getBookedBy() {
        return bookedBy;
    }
}
