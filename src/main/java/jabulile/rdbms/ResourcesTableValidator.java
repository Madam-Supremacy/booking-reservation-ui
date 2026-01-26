package jabulile.rdbms;

public class ResourcesTableValidator extends AbstractTableValidator {
    @Override
    public String validationQuery() {
        return "SELECT * FROM resources;";
    }
}
