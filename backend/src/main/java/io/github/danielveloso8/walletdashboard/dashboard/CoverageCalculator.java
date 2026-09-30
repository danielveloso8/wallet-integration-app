package io.github.danielveloso8.walletdashboard.dashboard;

import io.github.danielveloso8.walletdashboard.domain.DateWindow;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CoverageCalculator {
    public List<CoverageGap> gaps(DateWindow requested, Map<String, List<DateWindow>> committedByAccount) {
        List<CoverageGap> gaps = new ArrayList<>();
        committedByAccount.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            List<DateWindow> windows = entry.getValue().stream()
                    .filter(window -> !window.end().isBefore(requested.start()) && !window.start().isAfter(requested.end()))
                    .sorted(Comparator.comparing(DateWindow::start)).toList();
            LocalDate cursor = requested.start();
            for (DateWindow window : windows) {
                LocalDate coveredStart = window.start().isBefore(requested.start()) ? requested.start() : window.start();
                LocalDate coveredEnd = window.end().isAfter(requested.end()) ? requested.end() : window.end();
                if (coveredStart.isAfter(cursor)) gaps.add(new CoverageGap(entry.getKey(), cursor, coveredStart.minusDays(1)));
                if (!coveredEnd.isBefore(cursor)) cursor = coveredEnd.plusDays(1);
                if (cursor.isAfter(requested.end())) break;
            }
            if (!cursor.isAfter(requested.end())) gaps.add(new CoverageGap(entry.getKey(), cursor, requested.end()));
        });
        return List.copyOf(gaps);
    }

    public record CoverageGap(String account, LocalDate start, LocalDate end) {}
}
