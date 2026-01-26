package jabulile.rdbms;

public class AvailabilityValidator extends AbstractTableValidator {
    @Override
    public String validationQuery() {
        return "SELECT * FROM resources WHERE id NOT IN (SELECT resource_id FROM bookings);";
    }
}
