package io.github.danielveloso8.walletdashboard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class DateWindowTest {
    @Test
    void comparesFullMonthToPreviousCalendarMonth() {
        assertThat(new DateWindow(LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 31))
                .previousComparable())
                .isEqualTo(new DateWindow(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)));
    }

    @Test
    void comparesCustomWindowToPrecedingEqualLengthWindow() {
        assertThat(new DateWindow(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 26))
                .previousComparable())
                .isEqualTo(new DateWindow(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 9, 9)));
    }

    @Test
    void returnsTwelveMonthsAcrossYearBoundary() {
        var months = DateWindow.monthsEndingAt(YearMonth.of(2026, 2), 12);
        assertThat(months).hasSize(12);
        assertThat(months.getFirst()).isEqualTo(YearMonth.of(2025, 3));
        assertThat(months.getLast()).isEqualTo(YearMonth.of(2026, 2));
    }
}
