package io.github.danielveloso8.walletdashboard.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties("app")
public record AppProperties(ZoneId timezone, @Name("import") Import importSettings, Security security) {
    public AppProperties {
        if (timezone == null) timezone = ZoneId.of("Europe/Lisbon");
        if (importSettings == null) importSettings = new Import(5 * 1024 * 1024, 50_000, 500, Duration.ofMinutes(60));
        if (security == null) security = new Security(null, null);
    }

    public record Import(long maxFileBytes, int maxRows, int maxErrorsReported, Duration stagedTtl) {
        public Import {
            if (maxFileBytes <= 0 || maxRows <= 0 || maxErrorsReported <= 0 || stagedTtl == null) {
                throw new IllegalArgumentException("Invalid import configuration");
            }
        }
    }

    public record Security(java.util.List<String> allowedHosts, java.util.List<String> allowedOrigins) {}
}
