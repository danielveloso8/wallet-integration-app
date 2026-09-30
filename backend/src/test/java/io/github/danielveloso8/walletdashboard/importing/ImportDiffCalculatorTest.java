package io.github.danielveloso8.walletdashboard.importing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielveloso8.walletdashboard.domain.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportDiffCalculatorTest {
    private final ImportDiffCalculator calculator = new ImportDiffCalculator();
    @Test
    void comparesDuplicatesAsAMultisetAndNormalizesAmounts() {
        TxnView a = row("450", "Food");
        TxnView b = row("450.0000", "Food");
        ImportDiff result = calculator.diff(List.of(a, a), List.of(b, b, b));
        assertThat(result.unchangedCount()).isEqualTo(2);
        assertThat(result.added()).hasSize(1);
        assertThat(result.removed()).isEmpty();
    }

    @Test
    void presentsRecategorizedRowAsChanged() {
        ImportDiff result = calculator.diff(List.of(row("10", "Food")), List.of(row("10", "Bills")));
        assertThat(result.changed()).hasSize(1);
        assertThat(result.changed().getFirst().fields()).containsExactly("category");
    }

    private static TxnView row(String amount, String category) {
        return new TxnView("Checking", category, "EUR", new BigDecimal(amount), TransactionType.EXPENSE,
                "Card", "note", "payee", "", Instant.parse("2026-09-01T10:00:00Z"), false, 2);
    }
}
