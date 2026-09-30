package io.github.danielveloso8.walletdashboard.importing.csv;

import java.util.List;
import java.util.regex.Pattern;

public final class WalletCsvSchema {
    public static final String DATE = "date", TYPE = "type", AMOUNT = "amount",
            REF_CURRENCY_AMOUNT = "ref_currency_amount", CURRENCY = "currency",
            ACCOUNT = "account", CATEGORY = "category", PAYMENT_TYPE = "payment_type",
            NOTE = "note", PAYEE = "payee", LABELS = "labels", TRANSFER = "transfer";
    public static final List<String> HEADERS = List.of(DATE, TYPE, AMOUNT, REF_CURRENCY_AMOUNT,
            CURRENCY, ACCOUNT, CATEGORY, PAYMENT_TYPE, NOTE, PAYEE, LABELS, TRANSFER);
    public static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    public static final Pattern CURRENCY_PATTERN = Pattern.compile("^[A-Z]{3}$");
    public static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z$");

    private WalletCsvSchema() {}
}
