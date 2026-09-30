package io.github.danielveloso8.walletdashboard.importing.csv;

import io.github.danielveloso8.walletdashboard.config.AppProperties;
import io.github.danielveloso8.walletdashboard.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;

public final class WalletRowMapper {
    private final AppProperties properties;

    public WalletRowMapper(AppProperties properties) { this.properties = properties; }

    public MappingResult map(CSVRecord row, int line) {
        List<RowError> errors = new ArrayList<>();
        String rawDate = row.get(WalletCsvSchema.DATE);
        String rawType = row.get(WalletCsvSchema.TYPE);
        String rawAmount = row.get(WalletCsvSchema.AMOUNT);
        String rawRefAmount = row.get(WalletCsvSchema.REF_CURRENCY_AMOUNT);
        String currency = row.get(WalletCsvSchema.CURRENCY);
        TransactionType type = TransactionType.fromWallet(rawType).orElse(null);
        Instant instant = null;
        BigDecimal amount = decimal(rawAmount, WalletCsvSchema.AMOUNT, line, errors, false);
        BigDecimal refAmount = decimal(rawRefAmount, WalletCsvSchema.REF_CURRENCY_AMOUNT, line, errors, true);

        if (!WalletCsvSchema.DATE_PATTERN.matcher(rawDate).matches()) {
            errors.add(new RowError(line, WalletCsvSchema.DATE, rawDate, "Expected UTC date with milliseconds"));
        } else {
            try { instant = Instant.parse(rawDate); }
            catch (DateTimeParseException e) { errors.add(new RowError(line, WalletCsvSchema.DATE, rawDate, "Invalid date")); }
        }
        if (type == null) errors.add(new RowError(line, WalletCsvSchema.TYPE, rawType, "Expected Despesa or Receita"));
        if (!WalletCsvSchema.CURRENCY_PATTERN.matcher(currency).matches()) {
            errors.add(new RowError(line, WalletCsvSchema.CURRENCY, currency, "Expected three uppercase letters"));
        }
        for (String column : List.of(WalletCsvSchema.ACCOUNT, WalletCsvSchema.CATEGORY, WalletCsvSchema.PAYMENT_TYPE)) {
            if (row.get(column).isBlank()) errors.add(new RowError(line, column, row.get(column), "Must not be blank"));
        }
        String rawTransfer = row.get(WalletCsvSchema.TRANSFER);
        if (!rawTransfer.equals("true") && !rawTransfer.equals("false")) {
            errors.add(new RowError(line, WalletCsvSchema.TRANSFER, rawTransfer, "Expected true or false"));
        }
        if (!errors.isEmpty()) return new MappingResult(null, errors);
        LocalDate occurredOn = LocalDate.ofInstant(instant, properties.timezone());
        ParsedTransaction parsed = new ParsedTransaction(line, rawLine(row), row.get(WalletCsvSchema.ACCOUNT),
                row.get(WalletCsvSchema.CATEGORY), currency, amount, rawAmount, refAmount, type, rawType,
                row.get(WalletCsvSchema.PAYMENT_TYPE), row.get(WalletCsvSchema.NOTE),
                row.get(WalletCsvSchema.PAYEE), row.get(WalletCsvSchema.LABELS), instant, occurredOn,
                Boolean.parseBoolean(rawTransfer));
        return new MappingResult(parsed, List.of());
    }

    private static BigDecimal decimal(String value, String column, int line, List<RowError> errors, boolean emptyAllowed) {
        if (emptyAllowed && value.isEmpty()) return null;
        if (!WalletCsvSchema.AMOUNT_PATTERN.matcher(value).matches()) {
            errors.add(new RowError(line, column, value, "Expected a non-negative decimal with at most four fractional digits"));
            return null;
        }
        BigDecimal decimal = new BigDecimal(value);
        if (decimal.scale() > 4) errors.add(new RowError(line, column, value, "Maximum scale is four"));
        return decimal.scale() > 4 ? null : decimal;
    }

    private static String rawLine(CSVRecord row) {
        return String.join(";", row.toMap().entrySet().stream().map(Map.Entry::getValue).toList());
    }

    public record MappingResult(ParsedTransaction transaction, List<RowError> errors) {
        public MappingResult { errors = List.copyOf(errors); }
    }
}
