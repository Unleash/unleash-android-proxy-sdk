package io.getunleash

import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Variant
import io.getunleash.data.disabledVariant
import io.getunleash.polling.AutoPollingMode
import io.getunleash.polling.AutoPollingPolicy
import io.getunleash.polling.RefreshPolicy
import io.getunleash.polling.UnleashFetcher
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.nio.file.Files
import java.security.InvalidParameterException
import java.time.Duration
import java.util.concurrent.CompletableFuture

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
    private val refreshPolicy: RefreshPolicy = when(unleashConfig.pollingMode) {
        is AutoPollingMode -> AutoPollingPolicy(unleashFetcher = fetcher, cache = cache, config = unleashConfig, context = unleashContext, unleashConfig.pollingMode)
        else -> throw InvalidParameterException("The polling mode parameter is invalid")
    }

    companion object {
        fun newBuilder(): Builder = Builder()
    }

    override fun isEnabled(toggleName: String): Boolean {
        return refreshPolicy.readToggleCache()[toggleName]?.enabled ?: false
    }

    override fun getVariant(toggleName: String): Variant {
        return refreshPolicy.readToggleCache()[toggleName]?.variant ?: disabledVariant
    }

    override fun updateContext(context: UnleashContext): CompletableFuture<Void> {
        refreshPolicy.context = context
        return refreshPolicy.refreshAsync()
    }

    override fun getContext(): UnleashContext {
        return unleashContext
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