package jabulile.rdbms;

public class BookingsTableValidator extends AbstractTableValidator {
    @Override
    public String validationQuery() {
        return "SELECT * FROM bookings;";
    }
}
