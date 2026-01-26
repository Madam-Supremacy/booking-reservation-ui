package com.jabulile.booking.rdbms;

public class ResourcesTableValidator extends AbstractTableValidator {

    @Override
    public String tableName() {
        return "resources";
    }

    @Override
    public String validationQuery() {
        return "SELECT id, name, type FROM resources LIMIT 1;";
    }
}
