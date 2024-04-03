package io.getunleash

import okhttp3.OkHttpClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class UnleashConfigTest {

    @Test
    fun `can build config with builder`() {
        val config =
            UnleashConfig.newBuilder().appName("my-app").environment("default").proxyUrl("https://localhost:4242/proxy")
                .clientKey("some-key").build()


        assertThat(config.appName).isEqualTo("my-app")
        assertThat(config.environment).isEqualTo("default")
        assertThat(config.proxyUrl).isEqualTo("https://localhost:4242/proxy")
        assertThat(config.clientKey).isEqualTo("some-key")
    }

    @Test
    fun `can build context using normal class constructor`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        assertThat(config.appName).isEqualTo("my-app")
        assertThat(config.environment).isEqualTo("default")
        assertThat(config.proxyUrl).isEqualTo("https://localhost:4242/proxy")
        assertThat(config.clientKey).isEqualTo("some-key")
    }

    @Test
    fun `Can get a builder from existing context`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        val newConfig = config.newBuilder().appName("third").build()
        assertThat(config.appName).isEqualTo("my-app")
        assertThat(newConfig.appName).isEqualTo("third")
    }

    @Test
    fun `Failure to set proxy url or client secret fails the builder`() {
        assertThatThrownBy {
            UnleashConfig.newBuilder().appName("my-app").build()
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("You have to set proxy url in your UnleashConfig")
        assertThatThrownBy {
            UnleashConfig.newBuilder().proxyUrl("http://localhost:4242/proxy").build()
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessage("You have to set client key in your UnleashConfig")
    }

    @Test
    fun `Can set client timeouts in seconds with builder methods`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        val configInMs = config.newBuilder()
            .proxyUrl("http://localhost:4242")
            .httpClientConnectionTimeout(5000)
            .httpClientReadTimeout(4000)
            .build()
        val configInSec = config.newBuilder()
            .proxyUrl("http://localhost:4242")
            .httpClientConnectionTimeoutInSeconds(5)
            .httpClientReadTimeoutInSeconds(4)
            .build()
        assertThat(configInSec.httpClientConnectionTimeout).isEqualTo(5000)
        assertThat(configInSec.httpClientReadTimeout).isEqualTo(4000)
        assertThat(configInMs.httpClientConnectionTimeout).isEqualTo(configInSec.httpClientConnectionTimeout)
        assertThat(configInMs.httpClientReadTimeout).isEqualTo(configInSec.httpClientReadTimeout)
    }

    @Test
    fun `Can disable metrics with builder method`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        assertThat(config.reportMetrics).isNull()
        val withMetrics = config.newBuilder().disableMetrics().build()
        assertThat(withMetrics.reportMetrics).isNull()
    }

    @Test
    fun `Can tweak metrics report interval with builder methods`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )

        val configInMs = config.newBuilder().metricsInterval(5000).build()
        val configWithMetricsSetInSeconds = config.newBuilder().metricsIntervalInSeconds(5).build()
        assertThat(configInMs.reportMetrics!!.metricsInterval).isEqualTo(configWithMetricsSetInSeconds.reportMetrics!!.metricsInterval)
    }

    @Test
    fun `Default http client for metrics uses config`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        assertThat(config.reportMetrics).isNull()
        val withMetrics = config.newBuilder().build()
        assertThat(withMetrics.reportMetrics).isNotNull
        assertThat(withMetrics.reportMetrics!!.httpClient.connectTimeoutMillis).isEqualTo(config.httpClientConnectionTimeout)
        assertThat(withMetrics.reportMetrics!!.httpClient.readTimeoutMillis).isEqualTo(config.httpClientReadTimeout)
    }

    @Test
    fun `Can override http client for metrics`() {
        val config = UnleashConfig(
            proxyUrl = "https://localhost:4242/proxy",
            clientKey = "some-key",
            appName = "my-app",
            environment = "default"
        )
        assertThat(config.reportMetrics).isNull()
        val okHttpClient = OkHttpClient.Builder().build()
        val withMetrics = config.newBuilder().metricsHttpClient(okHttpClient).build()
        assertThat(withMetrics.reportMetrics).isNotNull
        assertEquals(okHttpClient, withMetrics.reportMetrics!!.httpClient)
    }
}
