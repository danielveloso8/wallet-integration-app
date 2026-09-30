package io.github.danielveloso8.walletdashboard.importing.csv;

public record RowError(int line, String column, String value, String message) {
    public RowError {
        value = value == null ? "" : value.substring(0, Math.min(value.length(), 100));
    }
}
