package dev.lizarazu.profileapi.usaspending;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class UsaSpendingConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(UsaSpendingConfiguration.class)
            .withPropertyValues(
                    "usaspending.base-url=https://usaspending.example",
                    "usaspending.connect-timeout=1s",
                    "usaspending.read-timeout=1s",
                    "usaspending.lookback-years=5",
                    "usaspending.result-limit=5"
            );

    @Test
    void doesNotCreateClientWhenDisabled() {
        runner.withPropertyValues("usaspending.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(UsaSpendingClient.class));
    }

    @Test
    void createsClientWhenEnabled() {
        runner.withPropertyValues("usaspending.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(UsaSpendingClient.class));
    }

    @Test
    void failsStartupWhenResultLimitIsOutOfRange() {
        runner.withPropertyValues(
                        "usaspending.enabled=true",
                        "usaspending.result-limit=500"
                )
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("result-limit must be between 1 and 100"));
    }
}