package io.github.danielveloso8.walletdashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void addsOnlyMatchingCurrencies() {
        assertThat(new Money(new BigDecimal("1.20"), "EUR")
                .add(new Money(new BigDecimal("2.30"), "EUR")).amount())
                .isEqualByComparingTo("3.50");
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "EUR")
                .add(new Money(BigDecimal.ONE, "USD")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
