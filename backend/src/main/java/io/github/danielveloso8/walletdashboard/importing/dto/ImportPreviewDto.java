package io.github.danielveloso8.walletdashboard.importing.dto;

import java.util.List;

public record ImportPreviewDto(long id, String windowStart, String windowEnd, List<String> accounts,
        int unchangedCount, int addedCount, int removedCount, int changedCount,
        List<String> added, List<String> removed, List<String> changed,
        boolean requiresRemovalConfirmation, Long identicalToBatchId) {}
