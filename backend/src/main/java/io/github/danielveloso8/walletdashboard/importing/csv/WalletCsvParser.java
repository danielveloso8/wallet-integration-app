package io.github.danielveloso8.walletdashboard.importing.csv;

import io.github.danielveloso8.walletdashboard.config.AppProperties;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public final class WalletCsvParser {
    private final AppProperties properties;
    private final WalletRowMapper mapper;

    public WalletCsvParser(AppProperties properties) {
        this.properties = properties;
        this.mapper = new WalletRowMapper(properties);
    }

    public ParseResult parse(byte[] bytes) {
        if (bytes.length > properties.importSettings().maxFileBytes()) {
            return failure("file", "File exceeds maximum size");
        }
        String content;
        try {
            content = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return failure("file", "Malformed UTF-8 at byte offset " + malformedOffset(bytes));
        }
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        List<ParsedTransaction> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        CSVFormat format = CSVFormat.Builder.create().setDelimiter(';').setQuote('"').setTrim(false)
                .setIgnoreSurroundingSpaces(false).setIgnoreEmptyLines(false).setHeader()
                .setSkipHeaderRecord(true).setAllowMissingColumnNames(false)
                .setDuplicateHeaderMode(org.apache.commons.csv.DuplicateHeaderMode.DISALLOW).get();
        try (CSVParser parser = format.parse(new StringReader(content))) {
            Set<String> headers = new LinkedHashSet<>(parser.getHeaderNames());
            Set<String> expected = new LinkedHashSet<>(WalletCsvSchema.HEADERS);
            if (!headers.equals(expected)) {
                Set<String> missing = new LinkedHashSet<>(expected); missing.removeAll(headers);
                Set<String> unexpected = new LinkedHashSet<>(headers); unexpected.removeAll(expected);
                if (!missing.isEmpty()) errors.add(new RowError(1, "header", String.join(", ", missing), "Missing headers"));
                if (!unexpected.isEmpty()) errors.add(new RowError(1, "header", String.join(", ", unexpected), "Unexpected headers"));
                if (!errors.isEmpty()) return new ParseResult(List.of(), cap(errors));
            }
            for (CSVRecord record : parser) {
                if (record.size() != WalletCsvSchema.HEADERS.size()) {
                    errors.add(new RowError((int) record.getRecordNumber() + 1, "record", "", "Expected exactly 12 fields"));
                } else {
                    var result = mapper.map(record, (int) record.getRecordNumber() + 1);
                    if (result.errors().isEmpty()) rows.add(result.transaction());
                    else errors.addAll(result.errors());
                }
                if (record.getRecordNumber() > properties.importSettings().maxRows()) {
                    errors.add(new RowError((int) record.getRecordNumber() + 1, "file", "", "Maximum row count exceeded"));
                    break;
                }
                if (errors.size() >= properties.importSettings().maxErrorsReported()) break;
            }
            if (parser.getRecordNumber() == 0) errors.add(new RowError(1, "file", "", "CSV contains no data rows"));
        } catch (IOException | IllegalArgumentException e) {
            errors.add(new RowError(1, "file", "", "Invalid CSV structure"));
        }
        return errors.isEmpty() ? new ParseResult(rows, List.of()) : new ParseResult(List.of(), cap(errors));
    }

    private ParseResult failure(String column, String message) {
        return new ParseResult(List.of(), List.of(new RowError(1, column, "", message)));
    }

    private List<RowError> cap(List<RowError> errors) {
        return List.copyOf(errors.subList(0, Math.min(errors.size(), properties.importSettings().maxErrorsReported())));
    }

    private static int malformedOffset(byte[] bytes) {
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer input = ByteBuffer.wrap(bytes);
        try { decoder.decode(input); } catch (CharacterCodingException ignored) { return input.position(); }
        return 0;
    }
}
