package io.github.danielveloso8.walletdashboard.importing;

import io.github.danielveloso8.walletdashboard.importing.csv.RowError;
import java.util.List;

public class ImportValidationException extends RuntimeException {
    private final List<RowError> errors;
    public ImportValidationException(List<RowError> errors) {
        super("Import file validation failed");
        this.errors = List.copyOf(errors);
    }
    public List<RowError> errors() { return errors; }
}
