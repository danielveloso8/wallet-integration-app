package io.github.danielveloso8.walletdashboard.importing.csv;

import java.util.List;

public record ParseResult(List<ParsedTransaction> rows, List<RowError> errors) {
    public ParseResult {
        rows = List.copyOf(rows);
        errors = List.copyOf(errors);
    }

    public boolean successful() { return errors.isEmpty(); }
}
