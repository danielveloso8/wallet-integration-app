package io.github.danielveloso8.walletdashboard.domain;

import java.util.Map;
import java.util.Optional;

public enum TransactionType {
    EXPENSE, INCOME;

    private static final Map<String, TransactionType> WALLET_TYPES =
            Map.of("Despesa", EXPENSE, "Receita", INCOME);

    public static Optional<TransactionType> fromWallet(String value) {
        return Optional.ofNullable(WALLET_TYPES.get(value));
    }
}
