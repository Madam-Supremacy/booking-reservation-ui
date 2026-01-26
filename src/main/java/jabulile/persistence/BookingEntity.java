package com.jabulile.booking.persistence;

import java.time.LocalDate;

public class BookingEntity {
    private final int id;
    private final int resourceId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String bookedBy;

    public BookingEntity(int id, int resourceId, LocalDate startDate, LocalDate endDate, String bookedBy) {
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
