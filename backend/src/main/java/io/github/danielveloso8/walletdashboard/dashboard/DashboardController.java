package io.github.danielveloso8.walletdashboard.dashboard;

import io.github.danielveloso8.walletdashboard.domain.DateWindow;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {
    private final DashboardService dashboard;
    public DashboardController(DashboardService dashboard) { this.dashboard = dashboard; }

    @GetMapping("/dashboard/summary")
    public DashboardService.DashboardSummary summary(@RequestParam("from") LocalDate from, @RequestParam("to") LocalDate to,
            @RequestParam(value = "accounts", required = false) List<String> accounts) {
        DateWindow window = new DateWindow(from, to);
        if (window.lengthDays() > 3660) throw new IllegalArgumentException("Date range cannot exceed 3,660 days");
        return dashboard.summary(window, accounts == null ? Set.of() : new LinkedHashSet<>(accounts));
    }

    @GetMapping("/dashboard/trend")
    public DashboardService.DashboardTrend trend(@RequestParam("endMonth") YearMonth endMonth,
            @RequestParam(value = "accounts", required = false) List<String> accounts) {
        return dashboard.trend(endMonth, accounts == null ? Set.of() : new LinkedHashSet<>(accounts));
    }

    @GetMapping("/accounts")
    public List<String> accounts() { return dashboard.knownAccounts(); }
}
