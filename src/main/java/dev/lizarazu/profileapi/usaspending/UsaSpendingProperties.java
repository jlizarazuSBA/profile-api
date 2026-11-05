package dev.lizarazu.profileapi.usaspending;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "usaspending")
public record UsaSpendingProperties(
        boolean enabled,
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        int lookbackYears,
        int resultLimit
) {
}