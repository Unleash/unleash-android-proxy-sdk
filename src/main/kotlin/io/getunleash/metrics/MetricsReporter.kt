package io.getunleash.metrics

import io.getunleash.UnleashConfig
import io.getunleash.data.Parser
import io.getunleash.data.Variant
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.IOException
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

interface MetricsReporter {
    /**
     * Log enabled state of feature name and return same state
     * @param featureName Name of feature toggle
     * @param enabled State of the feature toggle
     * @return enabled
     */
    fun log(featureName: String, enabled: Boolean): Boolean

    /**
     * Log which variant use got
     * @param featureName Name of feature toggle
     * @param variant Variant returned from getVariant call
     * @return variant
     */
    fun logVariant(featureName: String, variant: Variant): Variant

    /**
     * Send report to unleash proxy, also resets bucket
     */
    fun reportMetrics()

}

class NonReporter : MetricsReporter {
    override fun log(featureName: String, enabled: Boolean): Boolean {
        return enabled
    }

    override fun logVariant(featureName: String, variant: Variant): Variant {
        return variant
    }

    override fun reportMetrics() {
    }
}

data class EvaluationCount(var yes: Int, var no: Int, val variants: ConcurrentHashMap<String, Int> = ConcurrentHashMap())
data class Bucket(
    val start: Date,
    var stop: Date? = null,
    val toggles: ConcurrentHashMap<String, EvaluationCount> = ConcurrentHashMap()
)

data class Report(val appName: String, val environment: String, val instanceId: String, val bucket: Bucket)

class HttpMetricsReporter(val config: UnleashConfig, val started: Date = Date()) : MetricsReporter,
    Closeable {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(MetricsReporter::class.java)
    }

    val client = OkHttpClient.Builder().callTimeout(2, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
    val metricsUrl = config.proxyUrl.toHttpUrl().newBuilder().addPathSegment("client").addPathSegment("metrics").build()
    private var bucket: Bucket = Bucket(start = started)

    override fun reportMetrics() {
        val report = Report(
            appName = config.appName ?: "unknown",
            instanceId = config.instanceId ?: "not-set",
            environment = config.environment ?: "not-set",
            bucket = bucket
        )
        val request = Request.Builder().header("Authorization", config.clientKey).url(metricsUrl).post(
            Parser.jackson.writeValueAsString(report).toRequestBody("application/json".toMediaType())
        ).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logger.info("Failed to report metrics for interval")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body.use { //Need to consume body to ensure we don't keep connection open
                }
            }
        })
        bucket = Bucket(start = Date())
    }

    override fun log(featureName: String, enabled: Boolean): Boolean {
        val count = if (enabled) {
            EvaluationCount(1, 0)
        } else {
            EvaluationCount(0, 1)
        }
        bucket.toggles.merge(featureName, count) { old: EvaluationCount?, new: EvaluationCount ->
            old?.copy(yes = old.yes + new.yes, no = old.no + new.no) ?: new
        }
        return enabled
    }

    override fun logVariant(featureName: String, variant: Variant): Variant {
        bucket.toggles.compute(featureName) { _, count ->
            val evaluationCount = count ?: EvaluationCount(0, 0)
            evaluationCount.variants.merge(variant.name, 1) { old, value ->
                old + value
            }
            evaluationCount
        }
        return variant
    }

    override fun close() {
        client.connectionPool.evictAll()
        client.cache?.closeQuietly()
        client.dispatcher.executorService.shutdown()
    }
}