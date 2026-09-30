package io.github.danielveloso8.walletdashboard.importing;

import io.github.danielveloso8.walletdashboard.importing.dto.ImportBatchDto;
import io.github.danielveloso8.walletdashboard.importing.dto.ImportPreviewDto;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportStagingService staging;
    private final ImportCommitService commits;
    public ImportController(ImportStagingService staging, ImportCommitService commits) {
        this.staging = staging; this.commits = commits;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ImportPreviewDto stage(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "windowStart", required = false) LocalDate windowStart,
            @RequestParam(value = "windowEnd", required = false) LocalDate windowEnd) throws IOException {
        if ((windowStart == null) != (windowEnd == null)) throw new IllegalArgumentException("Both windowStart and windowEnd are required");
        return staging.stage(file, windowStart, windowEnd);
    }

    @PatchMapping("/{id}")
    public ImportPreviewDto rewindow(@PathVariable("id") long id, @RequestBody WindowRequest request) {
        return staging.rewindow(id, request.windowStart(), request.windowEnd());
    }

    @PostMapping("/{id}/commit")
    public ImportBatchDto commit(@PathVariable("id") long id, @RequestBody CommitRequest request) {
        return commits.commit(id, request.confirmRemovals());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discard(@PathVariable("id") long id) { staging.discard(id); }

    @PostMapping("/{id}/revert")
    public ImportBatchDto revert(@PathVariable("id") long id) { return commits.revert(id); }

    @GetMapping
    public List<ImportBatchDto> history() { return commits.history(); }

    public record WindowRequest(LocalDate windowStart, LocalDate windowEnd) {}
    public record CommitRequest(boolean confirmRemovals) {}
}
