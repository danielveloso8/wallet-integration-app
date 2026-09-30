package io.github.danielveloso8.walletdashboard.importing;

import io.github.danielveloso8.walletdashboard.importing.dto.ImportBatchDto;
import io.github.danielveloso8.walletdashboard.importing.dto.ImportPreviewDto;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportCommitService {
    private final JdbcTemplate jdbc;
    private final AppStateRepository state;
    private final ImportStagingService staging;

    public ImportCommitService(JdbcTemplate jdbc, AppStateRepository state, ImportStagingService staging) {
        this.jdbc = jdbc; this.state = state; this.staging = staging;
    }

    @Transactional
    public ImportBatchDto commit(long id, boolean confirmRemovals) {
        var batch = staged(id);
        if (expired(batch)) throw new ImportConflictException("STAGED_EXPIRED", "Staged import has expired");
        List<String> accounts = jdbc.queryForList("SELECT account FROM import_batch_account WHERE batch_id=?", String.class, id);
        var scope = placeholders(accounts.size());
        ImportPreviewDto preview = staging.preview(id);
        if (preview.requiresRemovalConfirmation() && !confirmRemovals) {
            throw new ImportConflictException("REMOVAL_CONFIRMATION_REQUIRED", "Confirm rows that will be superseded");
        }
        long expected = ((Number) batch.get("expected_version")).longValue();
        if (!state.compareAndIncrement(expected)) {
            throw new ImportConflictException("STALE_PREVIEW", "Data changed since preview; re-preview");
        }
        jdbc.update("UPDATE txn SET superseded_by_batch=? WHERE superseded_by_batch IS NULL AND batch_id IN (SELECT t.batch_id FROM txn t JOIN import_batch b ON b.id=t.batch_id JOIN import_batch_account a ON a.batch_id=b.id WHERE b.status='COMMITTED' AND a.account IN (" + scope + ") AND t.occurred_on BETWEEN ? AND ?)",
                concat(List.of(id), accounts, batch.get("window_start"), batch.get("window_end")));
        jdbc.update("UPDATE import_batch SET status='COMMITTED', committed_at=? WHERE id=?",
                OffsetDateTime.now(ZoneOffset.UTC), id);
        return dto(id);
    }

    @Transactional
    public ImportBatchDto revert(long id) {
        Long latest = jdbc.query("SELECT id FROM import_batch WHERE status='COMMITTED' ORDER BY committed_at DESC, id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (latest == null || latest != id) throw new ImportConflictException("ONLY_LATEST_REVERTIBLE", "Only the latest committed batch can be reverted");
        long version = state.currentVersion();
        if (!state.compareAndIncrement(version)) throw new ImportConflictException("STALE_PREVIEW", "Data changed during revert");
        jdbc.update("UPDATE txn SET superseded_by_batch=NULL WHERE superseded_by_batch=?", id);
        jdbc.update("UPDATE import_batch SET status='REVERTED', reverted_at=? WHERE id=?", OffsetDateTime.now(ZoneOffset.UTC), id);
        return dto(id);
    }

    public List<ImportBatchDto> history() {
        List<Long> ids = jdbc.queryForList("SELECT id FROM import_batch ORDER BY created_at DESC,id DESC", Long.class);
        return ids.stream().map(this::dto).toList();
    }

    private java.util.Map<String, Object> staged(long id) {
        var rows = jdbc.queryForList("SELECT * FROM import_batch WHERE id=? AND status='STAGED'", id);
        if (rows.isEmpty()) throw new ImportConflictException("NOT_STAGED", "Import is not staged");
        return rows.getFirst();
    }

    private static boolean expired(java.util.Map<String, Object> batch) {
        Object value = batch.get("staged_expires_at");
        if (value instanceof OffsetDateTime dateTime) return dateTime.isBefore(OffsetDateTime.now(ZoneOffset.UTC));
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().isBefore(java.time.Instant.now());
        throw new IllegalStateException("Unexpected staged expiration type");
    }

    private ImportBatchDto dto(long id) {
        var row = jdbc.queryForMap("SELECT * FROM import_batch WHERE id=?", id);
        List<String> accounts = jdbc.queryForList("SELECT account FROM import_batch_account WHERE batch_id=? ORDER BY account", String.class, id);
        Long latest = jdbc.query("SELECT id FROM import_batch WHERE status='COMMITTED' ORDER BY committed_at DESC, id DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null);
        return new ImportBatchDto(id, row.get("status").toString(), row.get("file_name").toString(),
                row.get("window_start").toString(), row.get("window_end").toString(), accounts,
                ((Number) row.get("row_count")).intValue(), toOffset(row.get("created_at")),
                toOffset(row.get("committed_at")), toOffset(row.get("reverted_at")), latest != null && latest == id);
    }

    private static OffsetDateTime toOffset(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime dateTime) return dateTime;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        throw new IllegalStateException("Unexpected database timestamp type: " + value.getClass().getName());
    }

    private static String placeholders(int count) {
        if (count == 0) throw new ImportConflictException("EMPTY_SCOPE", "An import must contain at least one account");
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }
    private static Object[] concat(List<?> accounts, Object start, Object end) {
        var values = new java.util.ArrayList<Object>(accounts); values.add(start); values.add(end); return values.toArray();
    }
    private static Object[] concat(List<?> prefix, List<?> accounts, Object start, Object end) {
        var values = new java.util.ArrayList<Object>(prefix); values.addAll(accounts); values.add(start); values.add(end); return values.toArray();
    }
}
