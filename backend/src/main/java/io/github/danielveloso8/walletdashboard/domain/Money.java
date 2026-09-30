package io.github.danielveloso8.walletdashboard.domain;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Amount and currency are required");
        }
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add amounts in different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }
}
