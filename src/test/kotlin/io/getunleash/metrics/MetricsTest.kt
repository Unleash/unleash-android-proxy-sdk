package io.getunleash.metrics

import com.fasterxml.jackson.module.kotlin.readValue
import io.getunleash.UnleashClient
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.data.Parser
import io.getunleash.data.Variant
import io.getunleash.polling.PollingModes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Date

class MetricsTest {

    lateinit var server: MockWebServer
    lateinit var config: UnleashConfig
    lateinit var context: UnleashContext

    @BeforeEach
    fun setUp() {
        val testResponse = File(MetricsTest::class.java.classLoader.getResource("proxyresponse.json")!!.file)
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse())
        config = UnleashConfig.newBuilder()
            .pollingMode(PollingModes.fileMode(testResponse))
            .proxyUrl(server.url("/proxy").toString())
            .clientKey("some-key")
            .appName("metrics-test")
            .instanceId("test-instance")
            .environment("test")
            .build()
        context = UnleashContext.newBuilder().appName("unleash-android-proxy-sdk").userId("some-user-id").build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    class TestReporter : MetricsReporter {
        private var toggles: MutableMap<String, EvaluationCount> = mutableMapOf()
        override fun log(featureName: String, enabled: Boolean): Boolean {
            toggles.compute(featureName) { _, count ->
                val counter = count ?: EvaluationCount(0, 0)
                if (enabled) {
                    counter.yes++
                } else {
                    counter.no++
                }
                counter
            }
            return enabled
        }

        override fun reportMetrics() {
            toggles = mutableMapOf()
        }

        fun getToggles(): MutableMap<String, EvaluationCount> {
            return toggles
        }

        override fun logVariant(featureName: String, variant: Variant): Variant {
            toggles.compute(featureName) { _, count ->
                val evaluationCount = count ?: EvaluationCount(0, 0)
                evaluationCount.variants.compute(variant.name) { _, value ->
                    (value ?: 0) + 1
                }
                evaluationCount
            }
            return variant
        }
    }

    @Test
    fun `can report toggles`() {
        val reporter = TestReporter()
        val client = UnleashClient(config, context, metricsReporter = reporter)
        assertThat(client.isEnabled("some-non-existing-toggle")).isFalse
        val toggles = reporter.getToggles()
        assertThat(toggles).containsEntry("some-non-existing-toggle", EvaluationCount(0, 1))
    }

    @Test
    fun `accumulates over period`() {
        val reporter = TestReporter()
        val client = UnleashClient(config, context, metricsReporter = reporter)
        repeat(100) {
            assertThat(client.isEnabled("some-non-existing-toggle")).isFalse
            assertThat(client.isEnabled("unleash_android_sdk_demo")).isTrue
        }
        val toggles = reporter.getToggles()
        assertThat(toggles).containsEntry("some-non-existing-toggle", EvaluationCount(0, 100))
        assertThat(toggles).containsEntry("unleash_android_sdk_demo", EvaluationCount(100, 0))
    }


    @Test
    fun `getVariant calls also records yes and no`() {
        val reporter = TestReporter()
        val client = UnleashClient(config, context, metricsReporter = reporter)
        repeat(100) {
			// toggle doesn't exist
			client.getVariant("some-non-existing-toggle")
			// toggle with variants
			client.getVariant("asdasd")
			// toggle without variants
			client.getVariant("cache.buster")
        }
        val toggles = reporter.getToggles()

        assertThat(toggles).containsAllEntriesOf(mutableMapOf(
            "some-non-existing-toggle" to EvaluationCount(0, 100, mutableMapOf("disabled" to 100)),
            "asdasd" to EvaluationCount(100, 0, mutableMapOf("123" to 100)),
            "cache.buster" to EvaluationCount(100, 0, mutableMapOf("disabled" to 100)),
        ))
    }

    @Test
    fun `reporting resets period`() {
        val reporter = TestReporter()
        val client = UnleashClient(config, context, metricsReporter = reporter)
        0.until(100).forEach {
            assertThat(client.isEnabled("some-non-existing-toggle")).isFalse()
            assertThat(client.isEnabled("unleash_android_sdk_demo")).isTrue()
        }
        var toggles = reporter.getToggles()
        assertThat(toggles).containsEntry("some-non-existing-toggle", EvaluationCount(0, 100))
        assertThat(toggles).containsEntry("unleash_android_sdk_demo", EvaluationCount(100, 0))
        reporter.reportMetrics()
        0.until(50).forEach {
            assertThat(client.isEnabled("some-non-existing-toggle")).isFalse()
            assertThat(client.isEnabled("unleash_android_sdk_demo")).isTrue()
        }
        toggles = reporter.getToggles()
        assertThat(toggles).containsEntry("some-non-existing-toggle", EvaluationCount(0, 50))
        assertThat(toggles).containsEntry("unleash_android_sdk_demo", EvaluationCount(50, 0))
    }

    @Test
    fun `http reporter does actually report toggles to metrics endpoint`() {
        val reporter = HttpMetricsReporter(config)
        val client = UnleashClient(config, context, metricsReporter = reporter)
        repeat(100) {
            client.isEnabled("unleash-android-proxy-sdk")
            client.isEnabled("non-existing-toggle")
            client.isEnabled("Test_release")
        }
        repeat(100) {
            client.getVariant("demoApp.step4")
        }
        reporter.reportMetrics()
        var reported = server.takeRequest()
        var report : Report = Parser.jackson.readValue(reported.body.inputStream())
        assertThat(report.appName).isEqualTo(config.appName)
        assertThat(report.environment).isEqualTo(config.environment)
        assertThat(report.instanceId).isEqualTo(config.instanceId)
        assertThat(report.bucket.toggles).hasSize(4)
        assertThat(report.bucket.toggles).containsAllEntriesOf(mutableMapOf(
            "unleash-android-proxy-sdk" to EvaluationCount(0, 100),
            "non-existing-toggle" to EvaluationCount(0, 100),
            "Test_release" to EvaluationCount(100, 0),
            "demoApp.step4" to EvaluationCount(100, 0, mutableMapOf("red" to 100))
        ))
        server.enqueue(MockResponse())
        // No activity since last report, bucket should be empty
        reporter.reportMetrics()
        reported = server.takeRequest()
        report = Parser.jackson.readValue(reported.body.inputStream())
        assertThat(report.appName).isEqualTo(config.appName)
        assertThat(report.environment).isEqualTo(config.environment)
        assertThat(report.instanceId).isEqualTo(config.instanceId)
        assertThat(report.bucket.toggles).isEmpty()
    }

    @Test
    fun `bucket start and stop gets reported in ISO 8601 format`() {
        val output = Parser.jackson.writeValueAsString(Date.from(ZonedDateTime.of(2021, 6, 1, 15, 0, 0, 456000000, ZoneOffset.UTC).toInstant()))
        assertThat(output).isEqualTo("\"2021-06-01T15:00:00.456+00:00\"")
    }

}
