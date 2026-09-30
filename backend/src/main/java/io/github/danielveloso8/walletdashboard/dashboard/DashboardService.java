package io.github.danielveloso8.walletdashboard.dashboard;

import io.github.danielveloso8.walletdashboard.domain.DateWindow;
import io.github.danielveloso8.walletdashboard.domain.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final JdbcTemplate jdbc;
    private final CoverageCalculator coverage = new CoverageCalculator();
    public DashboardService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public DashboardSummary summary(DateWindow window, Set<String> selectedAccounts) {
        List<String> known = jdbc.queryForList("SELECT DISTINCT account FROM import_batch_account a JOIN import_batch b ON b.id=a.batch_id WHERE b.status='COMMITTED' ORDER BY account", String.class);
        Set<String> accounts = selectedAccounts.isEmpty() ? new LinkedHashSet<>(known) : new LinkedHashSet<>(selectedAccounts);
        if (!known.containsAll(accounts)) throw new IllegalArgumentException("Unknown account selected");
        Map<String, Totals> current = totals(window, accounts);
        Map<String, Totals> previous = totals(window.previousComparable(), accounts);
        List<CoverageCalculator.CoverageGap> gaps = coverage.gaps(window, committedWindows(accounts));
        List<CoverageCalculator.CoverageGap> previousGaps = coverage.gaps(window.previousComparable(), committedWindows(accounts));
        List<CurrencyTotals> currencyTotals = new ArrayList<>();
        Set<String> currencies = new LinkedHashSet<>(current.keySet()); currencies.addAll(previous.keySet());
        for (String currency : currencies) {
            Totals now = current.getOrDefault(currency, new Totals());
            Totals before = previous.getOrDefault(currency, new Totals());
            currencyTotals.add(new CurrencyTotals(currency, now.income.toPlainString(), now.expense.toPlainString(),
                    now.income.subtract(now.expense).toPlainString(), now.transferIn.toPlainString(), now.transferOut.toPlainString(),
                    delta(now.income, before.income, previousGaps.isEmpty()),
                    delta(now.expense, before.expense, previousGaps.isEmpty()),
                    delta(now.income.subtract(now.expense), before.income.subtract(before.expense), previousGaps.isEmpty())));
        }
        List<CategoryTotals> categories = categoryTotals(window, accounts, window.previousComparable());
        List<AccountTransfer> transfers = transferTotals(window, accounts);
        return new DashboardSummary(window.start().toString(), window.end().toString(),
                window.previousComparable().start().toString(), window.previousComparable().end().toString(),
                currencyTotals, new Coverage(gaps.isEmpty(), mapGaps(gaps)),
                new Coverage(previousGaps.isEmpty(), mapGaps(previousGaps)), categories, transfers);
    }

    public List<String> knownAccounts() {
        return jdbc.queryForList("SELECT DISTINCT account FROM import_batch_account a JOIN import_batch b ON b.id=a.batch_id WHERE b.status='COMMITTED' ORDER BY account", String.class);
    }

    private Map<String, Totals> totals(DateWindow window, Set<String> accounts) {
        Map<String, Totals> result = new LinkedHashMap<>();
        if (accounts.isEmpty()) return result;
        String in = String.join(",", java.util.Collections.nCopies(accounts.size(), "?"));
        List<Object> args = new ArrayList<>(accounts); args.add(window.start()); args.add(window.end());
        jdbc.query("SELECT currency,type,transfer,COALESCE(SUM(amount),0) total FROM active_txn WHERE account IN (" + in + ") AND occurred_on BETWEEN ? AND ? GROUP BY currency,type,transfer",
                rs -> {
                    Totals total = result.computeIfAbsent(rs.getString("currency"), key -> new Totals());
                    BigDecimal amount = rs.getBigDecimal("total");
                    if (rs.getBoolean("transfer")) {
                        if (TransactionType.INCOME.name().equals(rs.getString("type"))) total.transferIn = amount;
                        else total.transferOut = amount;
                    } else if (TransactionType.INCOME.name().equals(rs.getString("type"))) total.income = amount;
                    else total.expense = amount;
                }, args.toArray());
        return result;
    }

    private Map<String, List<DateWindow>> committedWindows(Set<String> accounts) {
        Map<String, List<DateWindow>> result = new LinkedHashMap<>();
        accounts.forEach(account -> result.put(account, new ArrayList<>()));
        if (accounts.isEmpty()) return result;
        String in = String.join(",", java.util.Collections.nCopies(accounts.size(), "?"));
        jdbc.query("SELECT a.account,b.window_start,b.window_end FROM import_batch_account a JOIN import_batch b ON b.id=a.batch_id WHERE b.status='COMMITTED' AND a.account IN (" + in + ") ORDER BY b.window_start",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    result.get(rs.getString(1)).add(new DateWindow(rs.getDate(2).toLocalDate(), rs.getDate(3).toLocalDate()));
                },
                accounts.toArray());
        return result;
    }

    public DashboardTrend trend(YearMonth end, Set<String> accounts) {
        if (!knownAccounts().containsAll(accounts)) throw new IllegalArgumentException("Unknown account selected");
        List<YearMonth> months = DateWindow.monthsEndingAt(end, 12);
        List<TrendMonth> results = new ArrayList<>();
        for (YearMonth month : months) {
            DateWindow window = new DateWindow(month.atDay(1), month.atEndOfMonth());
            Map<String, Totals> monthly = totals(window, accounts);
            Map<String, String> sums = new LinkedHashMap<>();
            monthly.forEach((currency, total) -> sums.put(currency,
                    total.income.subtract(total.expense).toPlainString()));
            results.add(new TrendMonth(month.toString(), sums, coverage.gaps(window, committedWindows(accounts)).isEmpty()));
        }
        return new DashboardTrend(results);
    }

    private List<CategoryTotals> categoryTotals(DateWindow window, Set<String> accounts, DateWindow previous) {
        Map<String, CategoryAccumulator> totals = categoryWindow(window, accounts);
        Map<String, CategoryAccumulator> previousTotals = categoryWindow(previous, accounts);
        Set<String> keys = new LinkedHashSet<>(totals.keySet()); keys.addAll(previousTotals.keySet());
        return keys.stream().map(key -> {
            String[] parts = key.split("\u001f", -1);
            CategoryAccumulator current = totals.getOrDefault(key, new CategoryAccumulator());
            CategoryAccumulator before = previousTotals.getOrDefault(key, new CategoryAccumulator());
            return new CategoryTotals(parts[0], parts[1], current.expense.toPlainString(), current.income.toPlainString(),
                    before.expense.toPlainString(), before.income.toPlainString());
        }).toList();
    }

    private Map<String, CategoryAccumulator> categoryWindow(DateWindow window, Set<String> accounts) {
        Map<String, CategoryAccumulator> result = new LinkedHashMap<>();
        if (accounts.isEmpty()) return result;
        String in = String.join(",", java.util.Collections.nCopies(accounts.size(), "?"));
        List<Object> args = new ArrayList<>(accounts); args.add(window.start()); args.add(window.end());
        jdbc.query("SELECT currency,category,type,SUM(amount) total FROM active_txn WHERE transfer=FALSE AND account IN (" + in + ") AND occurred_on BETWEEN ? AND ? GROUP BY currency,category,type",
                rs -> {
                    String key = rs.getString("currency") + "\u001f" + rs.getString("category");
                    CategoryAccumulator accumulator = result.computeIfAbsent(key, ignored -> new CategoryAccumulator());
                    if (TransactionType.INCOME.name().equals(rs.getString("type"))) accumulator.income = rs.getBigDecimal("total");
                    else accumulator.expense = rs.getBigDecimal("total");
                }, args.toArray());
        return result;
    }

    private List<AccountTransfer> transferTotals(DateWindow window, Set<String> accounts) {
        if (accounts.isEmpty()) return List.of();
        String in = String.join(",", java.util.Collections.nCopies(accounts.size(), "?"));
        List<Object> args = new ArrayList<>(accounts); args.add(window.start()); args.add(window.end());
        Map<String, AccountTransferAccumulator> result = new LinkedHashMap<>();
        jdbc.query("SELECT account,currency,type,SUM(amount) total FROM active_txn WHERE transfer=TRUE AND account IN (" + in + ") AND occurred_on BETWEEN ? AND ? GROUP BY account,currency,type",
                rs -> {
                    String account = rs.getString("account"), currency = rs.getString("currency");
                    String key = account + "\u001f" + currency;
                    AccountTransferAccumulator accumulator = result.computeIfAbsent(key,
                            ignored -> new AccountTransferAccumulator(account, currency));
                    if (TransactionType.INCOME.name().equals(rs.getString("type"))) accumulator.in = rs.getBigDecimal("total");
                    else accumulator.out = rs.getBigDecimal("total");
                }, args.toArray());
        return result.values().stream().map(t -> new AccountTransfer(t.account, t.currency, t.in.toPlainString(), t.out.toPlainString())).toList();
    }

    private Delta delta(BigDecimal current, BigDecimal previous, boolean previousCovered) {
        BigDecimal absolute = current.subtract(previous);
        String percentage = previous.signum() == 0 || !previousCovered ? null
                : absolute.multiply(BigDecimal.valueOf(100)).divide(previous.abs(), 2, RoundingMode.HALF_UP).toPlainString();
        return new Delta(absolute.toPlainString(), percentage);
    }

    private static List<Gap> mapGaps(List<CoverageCalculator.CoverageGap> gaps) {
        return gaps.stream().map(g -> new Gap(g.account(), g.start().toString(), g.end().toString())).toList();
    }

    private static final class Totals {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO,
                transferIn = BigDecimal.ZERO, transferOut = BigDecimal.ZERO;
    }
    private static final class CategoryAccumulator {
        BigDecimal income = BigDecimal.ZERO, expense = BigDecimal.ZERO;
    }
    private static final class AccountTransferAccumulator {
        final String account, currency;
        BigDecimal in = BigDecimal.ZERO, out = BigDecimal.ZERO;
        AccountTransferAccumulator(String account, String currency) { this.account = account; this.currency = currency; }
    }
    public record DashboardSummary(String from, String to, String prevFrom, String prevTo,
            List<CurrencyTotals> currencies, Coverage coverage, Coverage previousCoverage,
            List<CategoryTotals> categories, List<AccountTransfer> transfers) {}
    public record CurrencyTotals(String currency, String income, String expense, String net, String transferIn, String transferOut,
            Delta incomeDelta, Delta expenseDelta, Delta netDelta) {}
    public record Delta(String abs, String pct) {}
    public record CategoryTotals(String currency, String category, String expense, String income, String prevExpense, String prevIncome) {}
    public record AccountTransfer(String account, String currency, String in, String out) {}
    public record Coverage(boolean complete, List<Gap> gaps) {}
    public record Gap(String account, String start, String end) {}
    public record DashboardTrend(List<TrendMonth> months) {}
    public record TrendMonth(String month, Map<String, String> netByCurrency, boolean covered) {}
}
