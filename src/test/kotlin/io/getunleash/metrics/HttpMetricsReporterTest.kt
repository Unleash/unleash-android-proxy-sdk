package io.getunleash.metrics

import com.fasterxml.jackson.databind.ObjectMapper
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.data.Variant
import io.getunleash.polling.PollingModes
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class HttpMetricsReporterTest {

    @Test
    fun metricsUrlIsCorrect() {
        val okHttpClient = OkHttpClient.Builder().build()
        HttpMetricsReporter(UnleashConfig.newBuilder().proxyUrl("http://localhost:4242/proxy").clientKey("some-key").build(), okHttpClient).use { reporter ->
            assertThat(reporter.metricsUrl.toString()).isEqualTo("http://localhost:4242/proxy/client/metrics")
        }
    }

    @Test
    fun metricsFormatIsCorrect() {
        val testResponse = File(MetricsTest::class.java.classLoader.getResource("proxyresponse.json")!!.file)
        val server = MockWebServer()
        server.start()

        val config = UnleashConfig.newBuilder()
                .pollingMode(PollingModes.fileMode(testResponse))
                .proxyUrl(server.url("/proxy").toString())
                .clientKey("some-key")
                .appName("metrics-test")
                .instanceId("test-instance")
                .environment("test")
                .build()
        val okHttpClient = OkHttpClient.Builder().build()
        HttpMetricsReporter(config, okHttpClient).reportMetrics()
        // This line blocks until a request is received or the timeout expires
        val recordedRequest = server.takeRequest()
        val requestBody = recordedRequest.body.readUtf8()
        val objectMapper = ObjectMapper()
        val response = objectMapper.readTree(requestBody)
        assertThat(response.get("appName").asText()).isEqualTo("metrics-test")
        assertThat(response.get("environment").asText()).isEqualTo("test")
        assertThat(response.get("instanceId").asText()).isEqualTo("test-instance")

        val today = Date().toInstant().atZone(TimeZone.getTimeZone("UTC").toZoneId()).toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        // format today as yyyy-MM-dd
        assertThat(response.get("bucket").get("start").asText()).startsWith(today)
        assertThat(response.get("bucket").get("stop").asText()).startsWith(today)
        assertThat(response.get("bucket").get("toggles").toString()).isEqualTo("{}")
    }

}
