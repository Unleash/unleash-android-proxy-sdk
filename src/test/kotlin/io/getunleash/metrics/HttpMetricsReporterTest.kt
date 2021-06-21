package io.getunleash.metrics

import io.getunleash.UnleashConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HttpMetricsReporterTest {

    @Test
    fun metricsUrlIsCorrect() {
        HttpMetricsReporter(UnleashConfig.newBuilder().proxyUrl("http://localhost:4242/proxy").clientSecret("some-secret").build()).use { reporter ->
            assertThat(reporter.metricsUrl.toString()).isEqualTo("http://localhost:4242/proxy/client/metrics")
        }
    }
}