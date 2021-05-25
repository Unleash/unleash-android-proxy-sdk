package io.getunleash

import io.getunleash.polling.AutoPollingMode
import io.getunleash.polling.PollingMode
import java.time.Duration

/**
 * Represents configuration for Unleash.
 * @property url HTTP(s) URL to the Unleash Proxy (Required).
 * @property clientKey the secret added as the Authorization header sent to the unleash-proxy (Required)
 * @property appName: name of the underlying application. Will be part of the unleash context if not overridden in the [io.getunleash.UnleashClient.updateContext] call (Required).
 * @property environment Part of unleash context if not overridden when using [io.getunleash.UnleashClient.updateContext] (Optional - Defaults to 'default')
 * @property pollingMode How to poll for features. Defaults to Automatic and once every 60 seconds
 * @property httpClientReadTimeout The number of seconds to wait for HTTP reads. (Optional - Defaults to 5)
 * @property httpClientConnectionTimeout The number of seconds to wait for HTTP connection. (Optional - Defaults to 2)
 * @property httpClientCacheSize Disk space (in bytes) set aside for http cache. (Optional - Defaults to 10MB)
 */
data class UnleashConfig(
    val proxyUrl: String,
    val clientSecret: String,
    val appName: String? = null,
    val environment: String? = null,
    val pollingMode: PollingMode = AutoPollingMode(Duration.ofSeconds(60)),
    val httpClientConnectionTimeout: Duration = Duration.ofSeconds(2),
    val httpClientReadTimeout: Duration = Duration.ofSeconds(5),
    val httpClientCacheSize: Long = 1024 * 1024 * 10
) {
    fun newBuilder(): Builder =
        Builder(
            proxyUrl = proxyUrl,
            clientSecret = clientSecret,
            appName = appName,
            environment = environment,
            pollingMode = pollingMode,
            httpClientConnectionTimeout = httpClientConnectionTimeout,
            httpClientReadTimeout = httpClientReadTimeout,
            httpClientCacheSize = httpClientCacheSize
        )

    companion object {
        fun newBuilder(): Builder = Builder()
    }

    data class Builder(
        var proxyUrl: String? = null,
        var clientSecret: String? = null,
        var appName: String? = null,
        var environment: String? = null,
        var pollingMode: PollingMode? = null,
        var httpClientConnectionTimeout: Duration? = null,
        var httpClientReadTimeout: Duration? = null,
        var httpClientCacheSize: Long? = null
    ) {
        fun proxyUrl(proxyUrl: String) = apply { this.proxyUrl = proxyUrl }
        fun clientSecret(secret: String) = apply { this.clientSecret = secret }
        fun appName(appName: String) = apply { this.appName = appName }
        fun environment(environment: String) = apply { this.environment = environment }
        fun pollingMode(pollingMode: PollingMode) = apply { this.pollingMode = pollingMode }
        fun httpClientConnectionTimeout(timeout: Duration) = apply { this.httpClientConnectionTimeout = timeout }
        fun httpClientConnectionTimeoutInSeconds(seconds: Long) = apply { this.httpClientConnectionTimeout = Duration.ofSeconds(seconds) }
        fun httpClientReadTimeout(timeout: Duration) = apply { this.httpClientReadTimeout = timeout }
        fun httpClientReadTimeoutInSeconds(seconds: Long) = apply { this.httpClientReadTimeout = Duration.ofSeconds(seconds) }
        fun httpClientCacheSize(cacheSize: Long) = apply { this.httpClientCacheSize = cacheSize }
        fun build(): UnleashConfig = UnleashConfig(
            proxyUrl = proxyUrl ?: throw IllegalStateException("You have to set proxy url in your UnleashConfig"),
            clientSecret = clientSecret
                ?: throw IllegalStateException("You have to set client secret in your UnleashConfig"),
            appName = appName,
            environment = environment,
            pollingMode = pollingMode ?: AutoPollingMode(Duration.ofSeconds(60)),
            httpClientConnectionTimeout = httpClientConnectionTimeout ?: Duration.ofSeconds(2),
            httpClientReadTimeout = httpClientReadTimeout ?: Duration.ofSeconds(5),
            httpClientCacheSize = httpClientCacheSize ?: 1024 * 1024 * 10
        )

    }
}