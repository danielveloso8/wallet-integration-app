package io.github.danielveloso8.walletdashboard.importing.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ImportBatchDto(long id, String status, String fileName, String windowStart, String windowEnd,
        List<String> accounts, int rowCount, OffsetDateTime createdAt, OffsetDateTime committedAt,
        OffsetDateTime revertedAt, boolean revertible) {}
