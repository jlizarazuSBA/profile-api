package dev.lizarazu.profileapi.usaspending;

import java.net.http.HttpClient;
import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UsaSpendingProperties.class)
public class UsaSpendingConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "usaspending",
            name = "enabled",
            havingValue = "true"
    )
    public UsaSpendingClient usaSpendingClient(UsaSpendingProperties properties) {
        if (properties.resultLimit() < 1 || properties.resultLimit() > 100) {
            throw new IllegalStateException(
                    "usaspending.result-limit must be between 1 and 100"
            );
        }
        if (properties.lookbackYears() < 1) {
            throw new IllegalStateException(
                    "usaspending.lookback-years must be at least 1"
            );
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();

        return new UsaSpendingClient(
            restClient,
            Clock.systemDefaultZone(),
            properties.lookbackYears(),
            properties.resultLimit()
        );
    }
}