package io.github.danielveloso8.walletdashboard.importing;

import io.github.danielveloso8.walletdashboard.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record TxnView(String account, String category, String currency, BigDecimal amount,
        TransactionType type, String paymentType, String note, String payee, String labels,
        Instant occurredAt, boolean transfer, int sourceLine) {}
