package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Toggle
import org.slf4j.LoggerFactory
import java.util.Timer
import java9.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer

class AutoPollingPolicy(
    override val unleashFetcher: UnleashFetcher,
    override val cache: ToggleCache,
    override val config: UnleashConfig,
    override var context: UnleashContext,
    autoPollingConfig: AutoPollingMode
) :
    RefreshPolicy(
        unleashFetcher = unleashFetcher,
        cache = cache,
        logger = LoggerFactory.getLogger("io.getunleash.polling.AutoPollingPolicy"),
        config = config,
        context = context
    ) {
    private val initialized = AtomicBoolean(false)
    private val initFuture = CompletableFuture<Unit>()
    private val timer: Timer

    init {
        autoPollingConfig.togglesUpdatedListener.let { listeners.add(it) }
        autoPollingConfig.erroredListener.let { errorListeners.add(it) }
        timer =
            timer(
                name = "unleash_toggles_fetcher",
                initialDelay = 0L,
                daemon = true,
                period = autoPollingConfig.pollRateDuration.toMillis()
            ) {
                updateToggles()
                if (!initialized.getAndSet(true)) {
                    initFuture.complete(null)
                }
            }
    }


    override fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>> {
        return if (this.initFuture.isDone) {
            CompletableFuture.completedFuture(super.readToggleCache())
        } else {
            this.initFuture.thenApplyAsync { super.readToggleCache() }
        }
    }

    private fun updateToggles() {
        try {
            val response = super.fetcher().getTogglesAsync(context).get()
            val cached = super.readToggleCache()
            if (response.isFetched() && cached != response.toggles) {
                super.writeToggleCache(response.toggles)
                this.broadcastTogglesUpdated()
            } else if (response.isFailed()) {

            }
        } catch (e: Exception) {
            this.broadcastTogglesErrored(e)
            logger.warn("Exception in AutoPollingCachePolicy", e)
        }
    }

    private fun broadcastTogglesErrored(e: Exception) {
        synchronized(errorListeners) {
            errorListeners.forEach {
                it.onError(e)
            }
        }
    }

    private fun broadcastTogglesUpdated() {
        synchronized(listeners) {
            listeners.forEach {
                it.onTogglesUpdated()
            }
        }
    }

    override fun close() {
        super.close()
        this.timer.cancel()
        this.listeners.clear()
    }
}