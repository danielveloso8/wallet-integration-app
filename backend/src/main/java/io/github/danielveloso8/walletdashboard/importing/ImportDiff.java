package io.github.danielveloso8.walletdashboard.importing;

import java.util.List;

public record ImportDiff(int unchangedCount, List<TxnView> added, List<TxnView> removed, List<ChangedTxn> changed) {
    public ImportDiff { added = List.copyOf(added); removed = List.copyOf(removed); changed = List.copyOf(changed); }
    public record ChangedTxn(TxnView oldRow, TxnView newRow, List<String> fields) {
        public ChangedTxn { fields = List.copyOf(fields); }
    }
}
