package io.github.danielveloso8.walletdashboard.importing;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AppStateRepository {
    private final JdbcClient jdbc;
    public AppStateRepository(JdbcClient jdbc) { this.jdbc = jdbc; }
    public long currentVersion() {
        return jdbc.sql("SELECT version FROM app_state WHERE id=1").query(Long.class).single();
    }
    public boolean compareAndIncrement(long expected) {
        return jdbc.sql("UPDATE app_state SET version=version+1 WHERE id=1 AND version=:version")
                .param("version", expected).update() == 1;
    }
}
