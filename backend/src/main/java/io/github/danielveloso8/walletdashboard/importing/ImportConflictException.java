package io.github.danielveloso8.walletdashboard.importing;

public class ImportConflictException extends RuntimeException {
    private final String code;
    public ImportConflictException(String code, String message) { super(message); this.code = code; }
    public String code() { return code; }
}
