package dev.lizarazu.profileapi.sam;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SamConfigurationTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(SamConfiguration.class)
                    .withPropertyValues(
                            "sam.base-url=https://sam.example",
                            "sam.connect-timeout=3s",
                            "sam.read-timeout=10s"
                    );

    @Test
    void doesNotCreateClientWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "sam.enabled=false",
                        "sam.api-key="
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .doesNotHaveBean(SamEntityClient.class);

                    SamProperties properties =
                            context.getBean(SamProperties.class);

                    assertThat(properties.enabled()).isFalse();
                    assertThat(properties.connectTimeout())
                            .isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.readTimeout())
                            .isEqualTo(Duration.ofSeconds(10));
                });
    }

    @Test
    void createsClientWhenEnabledWithKey() {
        contextRunner
                .withPropertyValues(
                        "sam.enabled=true",
                        "sam.api-key=test-api-key"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(SamEntityClient.class);

                    SamProperties properties =
                            context.getBean(SamProperties.class);

                    assertThat(properties.enabled()).isTrue();
                    assertThat(properties.baseUrl())
                            .isEqualTo("https://sam.example");
                });
    }

    @Test
    void rejectsEnabledIntegrationWithoutKey() {
        contextRunner
                .withPropertyValues(
                        "sam.enabled=true",
                        "sam.api-key="
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(
                                    IllegalStateException.class
                            )
                            .hasStackTraceContaining(
                                    "SAM_API_KEY must be set "
                                            + "when SAM integration is enabled"
                            );
                });
    }
}