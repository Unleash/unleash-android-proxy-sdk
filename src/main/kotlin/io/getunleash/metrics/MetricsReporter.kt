package io.getunleash.metrics

import io.getunleash.UnleashConfig
import io.getunleash.data.Parser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Duration
import java.time.Instant

interface MetricsReporter {
    /**
     * Log enabled state of feature name and return same state
     * @param featureName Name of feature toggle
     * @param enabled State of the feature toggle
     * @return enabled
     */
    fun log(featureName: String, enabled: Boolean): Boolean

    /**
     * Send report to unleash proxy, also resets bucket
     */
    fun reportMetrics()

}

class NonReporter : MetricsReporter {
    override fun log(featureName: String, enabled: Boolean): Boolean {
        return enabled
    }

    override fun reportMetrics() {
    }
}
data class EvaluationCount(var yes: Int, var no: Int)
data class Bucket(val start: Instant, var stop: Instant? = null, val toggles: MutableMap<String, EvaluationCount> = mutableMapOf())
data class Report(val appName: String, val environment: String, val instanceId: String, val bucket: Bucket)

class HttpMetricsReporter(val config: UnleashConfig, val started: Instant = Instant.now()) : MetricsReporter {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(MetricsReporter::class.java)
    }
    val client = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(2)).readTimeout(Duration.ofSeconds(5)).build()
    val metricsUrl = config.proxyUrl.toHttpUrl().newBuilder().addPathSegment("/client/metrics").build()
    private var bucket: Bucket = Bucket(start = started)

    override fun reportMetrics() {
        val report = Report(appName = config.appName ?: "unknown", instanceId = config.instanceId ?: "not-set", environment = config.environment ?: "not-set", bucket = bucket)
        val request = Request.Builder().url(metricsUrl).post(
            Parser.jackson.writeValueAsString(report).toRequestBody("application/json".toMediaType())
        ).build()
        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.info("Failed to report metrics for interval")
            }

            override fun onResponse(call: Call, response: Response) {
                logger.info("Metrics reported for interval")
            }
        })
        bucket = Bucket(start = Instant.now())
    }

    override fun log(featureName: String, enabled: Boolean): Boolean {
        val count = bucket.toggles.getOrDefault(featureName, EvaluationCount(0, 0))
        if (enabled) {
            count.yes++
        } else {
            count.no++
        }
        bucket.toggles[featureName] = count
        return enabled
    }
}