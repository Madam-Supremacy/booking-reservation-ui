package jabulile.rdbms;

/**
 * Abstract class for table validators.
 * Every table validator should implement validationQuery() to return a SQL query for validation.
 */
public abstract class AbstractTableValidator {

    /**
     * Each subclass must provide a SQL query to validate its table.
     * Example: SELECT * FROM bookings;
     *
     * @return SQL query string
     */
    public abstract String validationQuery();

}
