package io.getunleash

import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Variant
import io.getunleash.data.disabledVariant
import io.getunleash.metrics.HttpMetricsReporter
import io.getunleash.metrics.MetricsReporter
import io.getunleash.metrics.NonReporter
import io.getunleash.polling.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.InvalidParameterException
import java9.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timer

/**
 * A client for interacting with the Unleash Proxy.
 * Enables checking whether a feature toggle is enabled or not, and working with variants.
 * Evaluation happens in the proxy, so part of the setup is to configure how you'd like to
 * poll/update your local copy of the evaluated feature toggles. This can be configured in [io.getunleash.UnleashConfig.pollingMode]
 * @param unleashConfig
 * @param unleashContext
 * @param httpClient - You should not need to set this, the default is a client with a [io.getunleash.UnleashConfig.httpClientCacheSize] cache, readTimeout set to [io.getunleash.UnleashConfig.httpClientReadTimeout] and connection timeout set to [io.getunleash.UnleashConfig.httpClientConnectionTimeout]
 * @param cache - In addition to the http cache for our httpclient, we also have toggles stored in a cache, the default is an [io.getunleash.cache.InMemoryToggleCache] which utilizes a ConcurrentHashMap as the backing store
 * */
class UnleashClient(
    val unleashConfig: UnleashConfig,
    var unleashContext: UnleashContext = UnleashContext(),
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(unleashConfig.httpClientReadTimeout, TimeUnit.MILLISECONDS)
        .connectTimeout(unleashConfig.httpClientConnectionTimeout, TimeUnit.MILLISECONDS)
        .cache(
            Cache(
                directory = CacheDirectoryProvider().getCacheDirectory(),
                maxSize = unleashConfig.httpClientCacheSize
            )
        ).build(),
    val cache: ToggleCache = InMemoryToggleCache(),
    val metricsReporter: MetricsReporter = unleashConfig.reportMetrics?.let { HttpMetricsReporter(unleashConfig) } ?: NonReporter()
) : UnleashClientSpec {
    private val fetcher: UnleashFetcher = UnleashFetcher(unleashConfig = unleashConfig, httpClient = httpClient)
    private val refreshPolicy: RefreshPolicy = when (unleashConfig.pollingMode) {
        is AutoPollingMode -> AutoPollingPolicy(
            unleashFetcher = fetcher,
            cache = cache,
            config = unleashConfig,
            context = unleashContext,
            unleashConfig.pollingMode
        )
        is FilePollingMode -> FilePollingPolicy(
            unleashFetcher = fetcher,
            cache = cache,
            config = unleashConfig,
            context = unleashContext,
            unleashConfig.pollingMode
        )
        else -> throw InvalidParameterException("The polling mode parameter is invalid")
    }

    init {
        if (unleashConfig.reportMetrics != null) {
            timer(
                name = "unleash_report_metrics",
                daemon = true,
                initialDelay = unleashConfig.reportMetrics.metricsInterval,
                period = unleashConfig.reportMetrics.metricsInterval
            ) {
                metricsReporter.reportMetrics()
            }
        }
    }

    companion object {
        /**
         * Get a builder for setting up a client
         */
        fun newBuilder(): Builder = Builder()
        val logger: Logger = LoggerFactory.getLogger(UnleashClient::class.java)
    }

    override fun isEnabled(toggleName: String, defaultValue: Boolean): Boolean {
        val enabled = refreshPolicy.readToggleCache()[toggleName]?.enabled
        return metricsReporter.log(toggleName,  enabled ?: defaultValue)
    }

    override fun getVariant(toggleName: String): Variant {
        val variant = refreshPolicy.readToggleCache()[toggleName]?.variant ?: disabledVariant
        return metricsReporter.logVariant(toggleName, variant)
    }

    override fun updateContext(context: UnleashContext): CompletableFuture<Void> {
        refreshPolicy.context = context
        return refreshPolicy.refreshAsync()
    }

    override fun getContext(): UnleashContext {
        return unleashContext
    }

    override fun close() {
        this.refreshPolicy.close()
    }

    data class Builder(
        var httpClient: OkHttpClient? = null,
        var unleashConfig: UnleashConfig? = null,
        var unleashContext: UnleashContext? = null,
        var cache: ToggleCache? = null,
    ) {
        fun httpClient(okHttpClient: OkHttpClient) = apply { this.httpClient = okHttpClient }
        fun unleashConfig(unleashConfig: UnleashConfig) = apply { this.unleashConfig = unleashConfig }
        fun unleashContext(unleashContext: UnleashContext) = apply { this.unleashContext = unleashContext }
        fun cache(cache: ToggleCache) = apply { this.cache = cache }
        fun build(): UnleashClient = UnleashClient(
            unleashConfig = this.unleashConfig
                ?: throw IllegalStateException("You must set an UnleashConfig for your UnleashClient"),
            unleashContext = this.unleashContext ?: UnleashContext(),
            httpClient = this.httpClient ?: OkHttpClient.Builder()
                    .readTimeout(unleashConfig!!.httpClientReadTimeout, TimeUnit.MILLISECONDS)
                    .connectTimeout(unleashConfig!!.httpClientConnectionTimeout, TimeUnit.MILLISECONDS)
                    .cache(
                        Cache(
                            directory = CacheDirectoryProvider().getCacheDirectory(),
                            maxSize = unleashConfig!!.httpClientCacheSize
                )
            ).build(),
            cache = this.cache ?: InMemoryToggleCache(),
        )
    }

    /**
     * Adds a TogglesUpdatedListener to our [io.getunleash.polling.RefreshPolicy]
     * @param listener The listener to add
     * @since 0.2
     */
    override fun addTogglesUpdatedListener(listener: TogglesUpdatedListener) {
        refreshPolicy.addTogglesUpdatedListener(listener)
    }

    /**
     * Adds a TogglesErroredListener to our [io.getunleash.polling.RefreshPolicy]
     * @param listener the listener to add
     * @since 0.2
     */
    override fun addTogglesErroredListener(listener: TogglesErroredListener) {
        refreshPolicy.addTogglesErroredListener(listener)
    }
}
