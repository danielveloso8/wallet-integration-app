package io.github.danielveloso8.walletdashboard;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.datasource.url=jdbc:h2:mem:wallet-test;DB_CLOSE_DELAY=-1")
class FlywayMigrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test
    void migrationCreatesTheActiveTransactionViewAndVersionState() {
        assertThat(jdbc.queryForObject("SELECT version FROM app_state WHERE id=1", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM active_txn", Long.class)).isZero();
    }
}
