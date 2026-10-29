package dev.lizarazu.profileapi.sam;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sam")
public record SamProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout
) {
}