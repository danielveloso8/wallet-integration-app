package io.github.danielveloso8.walletdashboard.importing.csv;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.danielveloso8.walletdashboard.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class WalletCsvParserTest {
    private static final String HEADER = String.join(";", WalletCsvSchema.HEADERS);
    private final WalletCsvParser parser = new WalletCsvParser(new AppProperties(null,
            new AppProperties.Import(5_000_000, 50_000, 500, Duration.ofHours(1)), null));

    @Test
    void preservesValuesAndParsesQuotedDelimitersWithOrWithoutBom() {
        String line = "2026-09-29T23:06:51.825Z;Despesa;1.20;;EUR;Main;Food;Card;\"a; \"\"note\"\" \";Payee;tag;false";
        for (String prefix : new String[]{"", "\uFEFF"}) {
            ParseResult result = parser.parse((prefix + HEADER + "\n" + line).getBytes(StandardCharsets.UTF_8));
            assertThat(result.errors()).isEmpty();
            assertThat(result.rows()).hasSize(1);
            assertThat(result.rows().getFirst().note()).isEqualTo("a; \"note\" ");
            assertThat(result.rows().getFirst().occurredOn().toString()).isEqualTo("2026-09-30");
        }
    }

    @Test
    void rejectsMalformedValuesWithoutReturningPartialRows() {
        String valid = "2026-09-29T23:06:51.825Z;Despesa;1.20;;EUR;Main;Food;Card;note;Payee;tag;false";
        String invalid = valid.replace("Despesa", "Expense").replace("1.20", "1.23456");
        ParseResult result = parser.parse((HEADER + "\n" + valid + "\n" + invalid).getBytes(StandardCharsets.UTF_8));
        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).extracting(RowError::column).contains("type", "amount");
    }
}
