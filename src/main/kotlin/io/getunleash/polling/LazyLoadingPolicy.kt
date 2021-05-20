package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Toggle
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.Date
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class LazyLoadingPolicy(
    override val unleashFetcher: UnleashFetcher,
    override val cache: ToggleCache,
    override val config: UnleashConfig,
    override val context: UnleashContext = UnleashContext(),
    private val lazyLoadingMode: LazyLoadingMode,
) :
    RefreshPolicy(
        unleashFetcher = unleashFetcher,
        cache = cache,
        logger = LoggerFactory.getLogger(LazyLoadingPolicy::class.java),
        config = config,
        context = context
    ) {
    private var lastRefreshedTime: Instant = Date(0).toInstant()
    private var init = CompletableFuture<Void>()
    private var isFetching = AtomicBoolean(false)
    private var initialized = AtomicBoolean(false)
    private var fetchingFuture = CompletableFuture<Map<String, Toggle>>()
    var clock: Clock = Clock.systemUTC()

    override fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>> {
        val now = Instant.now(clock)
        if (now.isAfter(lastRefreshedTime.plusSeconds(this.lazyLoadingMode.refreshIntervalInSeconds))) {
            val isInitialized = this.init.isDone
            if (isInitialized && !this.isFetching.compareAndSet(false, true)) {
                return if(lazyLoadingMode.asyncRefresh && this.initialized.get()) {
                    CompletableFuture.completedFuture(super.readToggleCache())
                } else {
                    this.fetchingFuture
                }
            }
            if (isInitialized) {
                this.fetchingFuture = this.fetch()
                return if (this.lazyLoadingMode.asyncRefresh) {
                    CompletableFuture.completedFuture(super.readToggleCache())
                } else {
                    return fetch()
                }

            } else {
                if (this.isFetching.compareAndSet(false, true)) {
                    this.fetchingFuture = this.fetch()
                }
                return this.init.thenApplyAsync { super.readToggleCache() }
            }
        }
        return CompletableFuture.completedFuture(super.readToggleCache())
    }

    private fun fetch(): CompletableFuture<Map<String, Toggle>> {
        return super.fetcher().getTogglesAsync(context).thenApplyAsync { response ->
            val cached = super.readToggleCache()
            if (response.isFetched() && response.toggles != cached) {
                super.writeToggleCache(response.toggles)
            }

            if (!response.isFailed()) {
                this.lastRefreshedTime = Instant.now(clock)
            }

            if (this.initialized.compareAndSet(false, true)) {
                this.init.complete(null)
            }
            this.isFetching.set(false)

            if (response.isFetched()) {
                response.toggles
            } else {
                cached
            }
        }
    }

}