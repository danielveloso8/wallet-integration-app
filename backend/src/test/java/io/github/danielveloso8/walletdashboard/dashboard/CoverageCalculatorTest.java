package io.github.danielveloso8.walletdashboard.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielveloso8.walletdashboard.domain.DateWindow;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CoverageCalculatorTest {
    @Test
    void mergesAdjacentCoverageAndReportsRemainingGaps() {
        var requested = new DateWindow(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-10"));
        var gaps = new CoverageCalculator().gaps(requested, Map.of("A", List.of(
                new DateWindow(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-03")),
                new DateWindow(LocalDate.parse("2026-09-04"), LocalDate.parse("2026-09-06")))));
        assertThat(gaps).containsExactly(new CoverageCalculator.CoverageGap("A",
                LocalDate.parse("2026-09-07"), LocalDate.parse("2026-09-10")));
    }

    @Test
    void accountWithoutAnyCommittedWindowIsUncovered() {
        var requested = new DateWindow(LocalDate.parse("2026-09-01"), LocalDate.parse("2026-09-02"));
        assertThat(new CoverageCalculator().gaps(requested, Map.of("A", List.of())))
                .containsExactly(new CoverageCalculator.CoverageGap("A", requested.start(), requested.end()));
    }
}
