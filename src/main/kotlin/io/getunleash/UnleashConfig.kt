package io.getunleash

import io.getunleash.polling.AutoPollingMode
import io.getunleash.polling.PollingMode
import java.util.UUID


data class ReportMetrics(val metricsInterval: Long = 60000)
/**
 * Represents configuration for Unleash.
 * @property url HTTP(s) URL to the Unleash Proxy (Required).
 * @property clientSecret the secret added as the Authorization header sent to the unleash-proxy (Required)
 * @property appName: name of the underlying application. Will be used as default in the [io.getunleash.UnleashContext] call (Required).
 * @property environment which environment is the application running in. Will be used as default argument for the [io.getunleash.UnleashContext]. (Optional - Defaults to 'default')
 * @property instanceId instance id of your client
 * @property pollingMode How to poll for features. Defaults to [io.getunleash.polling.AutoPollingMode] with poll interval set to 60 seconds.
 * @property httpClientReadTimeout How long to wait for HTTP reads in milliseconds. (Optional - Defaults to 5000)
 * @property httpClientConnectionTimeout How long to wait for HTTP connection in milliseconds. (Optional - Defaults to 2000)
 * @property httpClientCacheSize Disk space (in bytes) set aside for http cache. (Optional - Defaults to 10MB)
 * @property reportMetrics Should the client collate and report metrics? The [io.getunleah.ReportMetrics] dataclass includes a metricsInterval field which defaults to 60 seconds. (Optional - defaults to null)
 */
data class UnleashConfig(
    val proxyUrl: String,
    val clientSecret: String,
    val appName: String? = null,
    val environment: String? = null,
    val instanceId: String? = UUID.randomUUID().toString(),
    val pollingMode: PollingMode = AutoPollingMode(60000),
    val httpClientConnectionTimeout: Long = 2000,
    val httpClientReadTimeout: Long = 5000,
    val httpClientCacheSize: Long = 1024 * 1024 * 10,
    val reportMetrics: ReportMetrics? = null
) {
    /**
     * Get a [io.getunleash.UnleashConfig.Builder] with all fields set to the value of
     * this instance of the class.
     */
    fun newBuilder(): Builder =
        Builder(
            proxyUrl = proxyUrl,
            clientSecret = clientSecret,
            appName = appName,
            environment = environment,
            pollingMode = pollingMode,
            httpClientConnectionTimeout = httpClientConnectionTimeout,
            httpClientReadTimeout = httpClientReadTimeout,
            httpClientCacheSize = httpClientCacheSize,
            enableMetrics = reportMetrics != null,
            metricsInterval = reportMetrics?.metricsInterval
        )

    companion object {
        /**
         * Get a [io.getunleash.UnleashConfig.Builder] with no fields set.
         */
        fun newBuilder(): Builder = Builder()
    }

    /**
     * Builder for [io.getunleash.UnleashConfig]
     */
    data class Builder(
        var proxyUrl: String? = null,
        var clientSecret: String? = null,
        var appName: String? = null,
        var environment: String? = null,
        var pollingMode: PollingMode? = null,
        var httpClientConnectionTimeout: Long? = null,
        var httpClientReadTimeout: Long? = null,
        var httpClientCacheSize: Long? = null,
        var enableMetrics: Boolean = false,
        var metricsInterval: Long? = null,
        var instanceId: String? = null,

    ) {
        fun proxyUrl(proxyUrl: String) = apply { this.proxyUrl = proxyUrl }
        fun clientSecret(secret: String) = apply { this.clientSecret = secret }
        fun appName(appName: String) = apply { this.appName = appName }
        fun environment(environment: String) = apply { this.environment = environment }
        fun pollingMode(pollingMode: PollingMode) = apply { this.pollingMode = pollingMode }
        fun httpClientConnectionTimeout(timeoutInMs: Long) = apply { this.httpClientConnectionTimeout = timeoutInMs }
        fun httpClientConnectionTimeoutInSeconds(seconds: Long) = apply { this.httpClientConnectionTimeout = seconds * 1000 }
        fun httpClientReadTimeout(timeoutInMs: Long) = apply { this.httpClientReadTimeout = timeoutInMs }
        fun httpClientReadTimeoutInSeconds(seconds: Long) = apply { this.httpClientReadTimeout = seconds * 1000 }
        fun httpClientCacheSize(cacheSize: Long) = apply { this.httpClientCacheSize = cacheSize }
        fun enableMetrics() = apply { this.enableMetrics = true }
        fun disableMetrics() = apply { this.enableMetrics = false }
        fun metricsInterval(intervalInMs: Long) = apply { this.metricsInterval = intervalInMs }
        fun metricsIntervalInSeconds(seconds: Long) = apply { this.metricsInterval = seconds * 1000 }
        fun instanceId(id: String) = apply { this.instanceId = id }
        fun build(): UnleashConfig = UnleashConfig(
            proxyUrl = proxyUrl ?: throw IllegalStateException("You have to set proxy url in your UnleashConfig"),
            clientSecret = clientSecret
                ?: throw IllegalStateException("You have to set client secret in your UnleashConfig"),
            appName = appName,
            environment = environment,
            pollingMode = pollingMode ?: AutoPollingMode(60000),
            httpClientConnectionTimeout = httpClientConnectionTimeout ?: 2000,
            httpClientReadTimeout = httpClientReadTimeout ?: 5000,
            httpClientCacheSize = httpClientCacheSize ?: (1024 * 1024 * 10),
            reportMetrics = if (enableMetrics) {
                ReportMetrics(metricsInterval ?: 60000)
            } else {
                null
            },
            instanceId = instanceId
        )

    }
}