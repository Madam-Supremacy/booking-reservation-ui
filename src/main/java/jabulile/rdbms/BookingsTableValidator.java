package com.jabulile.booking.rdbms;

public class BookingsTableValidator extends AbstractTableValidator {

    @Override
    public String tableName() {
        return "bookings";
    }

    @Override
    public String validationQuery() {
        return "SELECT id, resource_id, start_date, end_date FROM bookings LIMIT 1;";
    }
}
