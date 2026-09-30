package io.github.danielveloso8.walletdashboard.importing;

import io.github.danielveloso8.walletdashboard.config.AppProperties;
import io.github.danielveloso8.walletdashboard.domain.DateWindow;
import io.github.danielveloso8.walletdashboard.importing.csv.ParseResult;
import io.github.danielveloso8.walletdashboard.importing.csv.ParsedTransaction;
import io.github.danielveloso8.walletdashboard.importing.csv.RowError;
import io.github.danielveloso8.walletdashboard.importing.csv.WalletCsvParser;
import io.github.danielveloso8.walletdashboard.importing.dto.ImportPreviewDto;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Types;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImportStagingService {
    private final WalletCsvParser parser;
    private final AppProperties properties;
    private final AppStateRepository state;
    private final JdbcTemplate jdbc;
    private final ImportDiffCalculator diffCalculator;

    public ImportStagingService(WalletCsvParser parser, AppProperties properties, AppStateRepository state,
            JdbcTemplate jdbc, ImportDiffCalculator diffCalculator) {
        this.parser = parser; this.properties = properties; this.state = state; this.jdbc = jdbc;
        this.diffCalculator = diffCalculator;
    }

    @Transactional
    public ImportPreviewDto stage(MultipartFile file, java.time.LocalDate windowStart, java.time.LocalDate windowEnd) throws java.io.IOException {
        ParseResult result = parser.parse(file.getBytes());
        if (!result.successful()) throw new ImportValidationException(result.errors());
        if (result.rows().isEmpty()) throw new ImportValidationException(List.of(new RowError(1, "file", "", "CSV contains no data rows")));
        var rows = result.rows();
        var start = windowStart == null ? rows.stream().map(ParsedTransaction::occurredOn).min(java.time.LocalDate::compareTo).orElseThrow() : windowStart;
        var end = windowEnd == null ? rows.stream().map(ParsedTransaction::occurredOn).max(java.time.LocalDate::compareTo).orElseThrow() : windowEnd;
        DateWindow window;
        try { window = new DateWindow(start, end); }
        catch (IllegalArgumentException e) { throw new ImportValidationException(List.of(new RowError(1, "window", "", e.getMessage()))); }
        List<RowError> outside = rows.stream().filter(row -> !window.contains(row.occurredOn()))
                .map(row -> new RowError(row.sourceLine(), "date", row.occurredOn().toString(), "Row is outside the selected window")).toList();
        if (!outside.isEmpty()) throw new ImportValidationException(outside);
        jdbc.update("DELETE FROM txn WHERE batch_id IN (SELECT id FROM import_batch WHERE status='STAGED')");
        jdbc.update("DELETE FROM import_batch_account WHERE batch_id IN (SELECT id FROM import_batch WHERE status='STAGED')");
        jdbc.update("DELETE FROM import_batch WHERE status='STAGED'");
        byte[] bytes = file.getBytes();
        var key = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO import_batch(status,file_name,file_sha256,file_size,row_count,window_start,window_end,
                      expected_version,staged_expires_at,created_at)
                    VALUES ('STAGED',?,?,?,?,?,?,?,?,?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, file.getOriginalFilename() == null ? "wallet.csv" : file.getOriginalFilename());
            statement.setString(2, sha256(bytes)); statement.setLong(3, bytes.length); statement.setInt(4, rows.size());
            statement.setObject(5, start); statement.setObject(6, end); statement.setLong(7, state.currentVersion());
            statement.setObject(8, OffsetDateTime.now(ZoneOffset.UTC).plus(properties.importSettings().stagedTtl()));
            statement.setObject(9, OffsetDateTime.now(ZoneOffset.UTC));
            return statement;
        }, key);
        long id = key.getKey().longValue();
        Set<String> accounts = new LinkedHashSet<>();
        rows.forEach(row -> accounts.add(row.account()));
        jdbc.batchUpdate("INSERT INTO txn(batch_id,source_line,raw_line,account,category,currency,amount,amount_raw,ref_currency_amount,type,type_raw,payment_type,note,payee,labels,occurred_at,occurred_on,transfer) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                rows, 1_000, (statement, row) -> {
                    statement.setLong(1, id);
                    statement.setInt(2, row.sourceLine());
                    statement.setString(3, row.rawLine());
                    statement.setString(4, row.account());
                    statement.setString(5, row.category());
                    statement.setString(6, row.currency());
                    statement.setBigDecimal(7, row.amount());
                    statement.setString(8, row.amountRaw());
                    if (row.refCurrencyAmount() == null) statement.setNull(9, Types.DECIMAL);
                    else statement.setBigDecimal(9, row.refCurrencyAmount());
                    statement.setString(10, row.type().name());
                    statement.setString(11, row.typeRaw());
                    statement.setString(12, row.paymentType());
                    statement.setString(13, row.note());
                    statement.setString(14, row.payee());
                    statement.setString(15, row.labels());
                    statement.setObject(16, OffsetDateTime.ofInstant(row.occurredAt(), ZoneOffset.UTC));
                    statement.setObject(17, row.occurredOn());
                    statement.setBoolean(18, row.transfer());
                });
        jdbc.batchUpdate("INSERT INTO import_batch_account(batch_id,account) VALUES (?,?)",
                accounts.stream().map(account -> new Object[]{id, account}).toList());
        return preview(id, rows, accounts, start, end);
    }

    public ImportPreviewDto preview(long id) {
        var batch = jdbc.queryForMap("SELECT * FROM import_batch WHERE id=? AND status='STAGED'", id);
        Set<String> accounts = new LinkedHashSet<>(jdbc.queryForList("SELECT account FROM import_batch_account WHERE batch_id=? ORDER BY account", String.class, id));
        var window = new DateWindow(java.time.LocalDate.parse(batch.get("window_start").toString()),
                java.time.LocalDate.parse(batch.get("window_end").toString()));
        var diff = diffCalculator.diff(activeRows(accounts, window), stagedRows(id));
        List<String> added = diff.added().stream().limit(200).map(row -> row.account() + " · " + row.category() + " · " + row.amount()).toList();
        List<String> removed = diff.removed().stream().limit(200).map(row -> row.account() + " · " + row.category() + " · " + row.amount()).toList();
        List<String> changed = diff.changed().stream().limit(200).map(row -> row.oldRow().account() + " · " + String.join(", ", row.fields())).toList();
        Long identicalTo = jdbc.query("SELECT id FROM import_batch WHERE status='COMMITTED' AND file_sha256=? AND window_start=? AND window_end=? ORDER BY committed_at DESC,id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, batch.get("file_sha256"), window.start(), window.end());
        return new ImportPreviewDto(id, batch.get("window_start").toString(), batch.get("window_end").toString(),
                List.copyOf(accounts), diff.unchangedCount(), diff.added().size(), diff.removed().size(),
                diff.changed().size(), added, removed, changed,
                !diff.removed().isEmpty() || !diff.changed().isEmpty(), identicalTo);
    }

    @Transactional
    public ImportPreviewDto rewindow(long id, java.time.LocalDate start, java.time.LocalDate end) {
        DateWindow window = new DateWindow(start, end);
        var batch = jdbc.queryForList("SELECT id FROM import_batch WHERE id=? AND status='STAGED' AND staged_expires_at>CURRENT_TIMESTAMP", id);
        if (batch.isEmpty()) throw new ImportConflictException("STAGED_EXPIRED", "Staged import is missing or expired");
        List<RowError> errors = jdbc.query("SELECT source_line, occurred_on FROM txn WHERE batch_id=? AND (occurred_on<? OR occurred_on>?)",
                (rs, row) -> new RowError(rs.getInt(1), "date", rs.getDate(2).toString(), "Row is outside the selected window"), id, start, end);
        if (!errors.isEmpty()) throw new ImportValidationException(errors);
        jdbc.update("UPDATE import_batch SET window_start=?,window_end=?,expected_version=? WHERE id=?", start, end, state.currentVersion(), id);
        return preview(id);
    }

    @Transactional
    public void discard(long id) {
        if (jdbc.queryForList("SELECT id FROM import_batch WHERE id=? AND status='STAGED'", Long.class, id).isEmpty()) {
            throw new ImportConflictException("NOT_STAGED", "Only a staged import can be discarded");
        }
        jdbc.update("DELETE FROM txn WHERE batch_id=?", id);
        jdbc.update("DELETE FROM import_batch_account WHERE batch_id=?", id);
        jdbc.update("DELETE FROM import_batch WHERE id=?", id);
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void purgeExpired() {
        List<Long> expired = jdbc.queryForList("SELECT id FROM import_batch WHERE status='STAGED' AND staged_expires_at<CURRENT_TIMESTAMP", Long.class);
        for (Long id : expired) {
            jdbc.update("DELETE FROM txn WHERE batch_id=?", id);
            jdbc.update("DELETE FROM import_batch_account WHERE batch_id=?", id);
            jdbc.update("DELETE FROM import_batch WHERE id=?", id);
        }
    }

    private ImportPreviewDto preview(long id, List<ParsedTransaction> rows, Set<String> accounts,
            java.time.LocalDate start, java.time.LocalDate end) {
        return preview(id);
    }

    private List<TxnView> stagedRows(long id) {
        return jdbc.query("SELECT * FROM txn WHERE batch_id=? ORDER BY source_line", (rs, row) ->
                new TxnView(rs.getString("account"), rs.getString("category"), rs.getString("currency"),
                        rs.getBigDecimal("amount"), io.github.danielveloso8.walletdashboard.domain.TransactionType.valueOf(rs.getString("type")),
                        rs.getString("payment_type"), rs.getString("note"), rs.getString("payee"), rs.getString("labels"),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(), rs.getBoolean("transfer"), rs.getInt("source_line")), id);
    }

    private List<TxnView> activeRows(Set<String> accounts, DateWindow window) {
        return activeRows(List.copyOf(accounts), window);
    }

    private List<TxnView> activeRows(List<String> accounts, DateWindow window) {
        if (accounts.isEmpty()) return List.of();
        String in = String.join(",", java.util.Collections.nCopies(accounts.size(), "?"));
        List<Object> args = new ArrayList<>(accounts); args.add(window.start()); args.add(window.end());
        return jdbc.query("SELECT * FROM active_txn WHERE account IN (" + in + ") AND occurred_on BETWEEN ? AND ? ORDER BY source_line",
                (rs, row) -> new TxnView(rs.getString("account"), rs.getString("category"), rs.getString("currency"),
                        rs.getBigDecimal("amount"), io.github.danielveloso8.walletdashboard.domain.TransactionType.valueOf(rs.getString("type")),
                        rs.getString("payment_type"), rs.getString("note"), rs.getString("payee"), rs.getString("labels"),
                        rs.getObject("occurred_at", OffsetDateTime.class).toInstant(), rs.getBoolean("transfer"), rs.getInt("source_line")),
                args.toArray());
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable", e); }
    }
}
