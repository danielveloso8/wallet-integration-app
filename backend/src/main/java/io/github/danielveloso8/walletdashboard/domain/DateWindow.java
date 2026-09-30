package io.github.danielveloso8.walletdashboard.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.IntStream;

public record DateWindow(LocalDate start, LocalDate end) {
    public DateWindow {
        if (start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("Window start must be on or before end");
        }
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    public long lengthDays() {
        return end.toEpochDay() - start.toEpochDay() + 1;
    }

    public boolean isFullCalendarMonth() {
        YearMonth month = YearMonth.from(start);
        return start.getDayOfMonth() == 1 && end.equals(month.atEndOfMonth());
    }

    public DateWindow previousComparable() {
        if (isFullCalendarMonth()) {
            YearMonth previous = YearMonth.from(start).minusMonths(1);
            return new DateWindow(previous.atDay(1), previous.atEndOfMonth());
        }
        LocalDate previousEnd = start.minusDays(1);
        return new DateWindow(previousEnd.minusDays(lengthDays() - 1), previousEnd);
    }

    public static List<YearMonth> monthsEndingAt(YearMonth end, int count) {
        if (count < 1) throw new IllegalArgumentException("Month count must be positive");
        return IntStream.range(0, count).mapToObj(i -> end.minusMonths(count - 1L - i)).toList();
    }
}
