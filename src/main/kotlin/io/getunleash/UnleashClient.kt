package io.getunleash

import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Variant
import io.getunleash.polling.AutoPollingMode
import io.getunleash.polling.AutoPollingPolicy
import io.getunleash.polling.PollingMode
import io.getunleash.polling.RefreshPolicy
import io.getunleash.polling.UnleashFetcher
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.nio.file.Files
import java.security.InvalidParameterException
import java.sql.Ref
import java.time.Duration

class UnleashClient(
    val unleashConfig: UnleashConfig,
    var unleashContext: UnleashContext = UnleashContext(),
    val httpClient: OkHttpClient = OkHttpClient.Builder().readTimeout(Duration.ofSeconds(2)).cache(
        Cache(
            directory = Files.createTempDirectory("unleash_toggles").toFile(),
            maxSize = 10L * 1024L * 1024L // Use 10 MB as max
        )
    ).build(),
    val cache: ToggleCache = InMemoryToggleCache(),
) : UnleashClientSpec {
    private val fetcher: UnleashFetcher = UnleashFetcher(unleashConfig = unleashConfig, httpClient = httpClient)
    private val refreshPolicy: RefreshPolicy
    init {
        refreshPolicy = when(unleashConfig.pollingMode) {
            is AutoPollingMode -> AutoPollingPolicy(unleashFetcher = fetcher, cache = cache, config = unleashConfig, context = unleashContext, unleashConfig.pollingMode)
            else -> throw InvalidParameterException("The polling mode parameter is invalid")
        }
    }
    companion object {
        fun newBuilder(): Builder = Builder()
    }

    override fun isEnabled(toggleName: String): Boolean {
        TODO("Not implemented yet, need to figure out where to get toggles from")
    }

    override fun getVariant(toggleName: String): Variant {
        TODO("Not yet implemented")
    }

    override fun updateContext(context: UnleashContext) {
        TODO("Not yet implemented")
    }

    override fun getContext(): UnleashContext {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
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
            httpClient = this.httpClient ?: OkHttpClient.Builder().readTimeout(Duration.ofSeconds(2)).cache(
                Cache(
                    directory = Files.createTempDirectory("unleash_toggles").toFile(),
                    maxSize = 10L * 1024L * 1024L // Use 10 MB as max
                )
            ).build(),
            unleashConfig = this.unleashConfig ?: throw IllegalStateException("You must set an UnleashConfig for your UnleashClient"),
            unleashContext = this.unleashContext ?: UnleashContext(),
            cache = this.cache ?: InMemoryToggleCache(),
        )
    }
}