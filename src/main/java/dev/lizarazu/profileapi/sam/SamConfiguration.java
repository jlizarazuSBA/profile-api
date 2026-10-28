package dev.lizarazu.profileapi.sam;

import java.net.http.HttpClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SamProperties.class)
public class SamConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "sam",
            name = "enabled",
            havingValue = "true"
    )
    public SamEntityClient samEntityClient(SamProperties properties) {
        if (properties.apiKey() == null
                || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "SAM_API_KEY must be set when SAM integration is enabled"
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

        return new SamEntityClient(restClient, properties.apiKey());
    }
}