package io.github.danielveloso8.walletdashboard.importing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public final class ImportDiffCalculator {
    public ImportDiff diff(List<TxnView> active, List<TxnView> staged) {
        Map<String, List<TxnView>> current = grouped(active, this::semanticKey);
        Map<String, List<TxnView>> incoming = grouped(staged, this::semanticKey);
        List<TxnView> removed = new ArrayList<>(), added = new ArrayList<>();
        int unchanged = 0;
        for (String key : union(current, incoming)) {
            List<TxnView> oldRows = current.getOrDefault(key, new ArrayList<>());
            List<TxnView> newRows = incoming.getOrDefault(key, new ArrayList<>());
            int common = Math.min(oldRows.size(), newRows.size());
            unchanged += common;
            removed.addAll(oldRows.subList(common, oldRows.size()));
            added.addAll(newRows.subList(common, newRows.size()));
        }
        Comparator<TxnView> order = Comparator.comparingInt(TxnView::sourceLine);
        removed.sort(order); added.sort(order);
        Map<String, List<TxnView>> oldCandidates = grouped(removed, this::changeKey);
        Map<String, List<TxnView>> newCandidates = grouped(added, this::changeKey);
        List<ImportDiff.ChangedTxn> changed = new ArrayList<>();
        for (String key : union(oldCandidates, newCandidates)) {
            List<TxnView> oldRows = oldCandidates.getOrDefault(key, new ArrayList<>());
            List<TxnView> newRows = newCandidates.getOrDefault(key, new ArrayList<>());
            int count = Math.min(oldRows.size(), newRows.size());
            for (int i = 0; i < count; i++) {
                TxnView oldRow = oldRows.get(i), newRow = newRows.get(i);
                changed.add(new ImportDiff.ChangedTxn(oldRow, newRow, changedFields(oldRow, newRow)));
            }
        }
        removed.removeIf(row -> changed.stream().anyMatch(change -> change.oldRow() == row));
        added.removeIf(row -> changed.stream().anyMatch(change -> change.newRow() == row));
        return new ImportDiff(unchanged, added, removed, changed);
    }

    private String semanticKey(TxnView row) {
        return String.join("\u001f", row.account(), row.category(), row.currency(), normalized(row.amount()),
                row.type().name(), row.paymentType(), row.note(), row.payee(), row.labels(),
                row.occurredAt().toString(), Boolean.toString(row.transfer()));
    }
    private String changeKey(TxnView row) {
        return String.join("\u001f", row.account(), row.occurredAt().toString(), normalized(row.amount()),
                row.currency(), row.type().name(), row.note());
    }
    private static List<String> changedFields(TxnView a, TxnView b) {
        List<String> fields = new ArrayList<>();
        if (!a.category().equals(b.category())) fields.add("category");
        if (!a.paymentType().equals(b.paymentType())) fields.add("payment_type");
        if (!a.payee().equals(b.payee())) fields.add("payee");
        if (!a.labels().equals(b.labels())) fields.add("labels");
        if (a.transfer() != b.transfer()) fields.add("transfer");
        return fields;
    }
    private static String normalized(java.math.BigDecimal amount) { return amount.stripTrailingZeros().toPlainString(); }
    private static Map<String, List<TxnView>> grouped(List<TxnView> rows, Function<TxnView, String> key) {
        return rows.stream().collect(Collectors.groupingBy(key, HashMap::new, Collectors.toCollection(ArrayList::new)));
    }
    private static java.util.Set<String> union(Map<String, ?> a, Map<String, ?> b) {
        java.util.Set<String> keys = new java.util.LinkedHashSet<>(a.keySet()); keys.addAll(b.keySet()); return keys;
    }
}
