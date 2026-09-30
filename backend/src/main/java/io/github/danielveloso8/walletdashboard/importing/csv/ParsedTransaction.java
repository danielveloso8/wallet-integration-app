package io.github.danielveloso8.walletdashboard.importing.csv;

import io.github.danielveloso8.walletdashboard.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ParsedTransaction(int sourceLine, String rawLine, String account, String category,
        String currency, BigDecimal amount, String amountRaw, BigDecimal refCurrencyAmount,
        TransactionType type, String typeRaw, String paymentType, String note, String payee,
        String labels, Instant occurredAt, LocalDate occurredOn, boolean transfer) {}
