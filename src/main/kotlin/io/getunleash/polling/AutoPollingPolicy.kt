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
    val autoPollingConfig: AutoPollingMode,
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
    private var timer: Timer? = null
    init {
        autoPollingConfig.togglesUpdatedListener?.let { listeners.add(it) }
        autoPollingConfig.togglesCheckedListener?.let { checkListeners.add(it) }
        autoPollingConfig.erroredListener?.let { errorListeners.add(it) }
        autoPollingConfig.readyListener?.let { readyListeners.add(it) }
        if (autoPollingConfig.pollImmediate) {
            if (autoPollingConfig.pollRateDuration > 0) {
                timer =
                        timer(
                                name = "unleash_toggles_fetcher",
                                initialDelay = 0L,
                                daemon = true,
                                period = autoPollingConfig.pollRateDuration
                        ) {
                            updateToggles()
                            if (!initialized.getAndSet(true)) {
                                super.broadcastReady()
                                initFuture.complete(null)
                            }
                        }
            } else {
                updateToggles()
                if (!initialized.getAndSet(true)) {
                    super.broadcastReady()
                    initFuture.complete(null)
                }
            }
        }
    }

    override val isReady: AtomicBoolean
        get() = initialized

    override fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>> {
        return if (this.initFuture.isDone) {
            CompletableFuture.completedFuture(super.readToggleCache())
        } else {
            this.initFuture.thenApplyAsync { super.readToggleCache() }
        }
    }

    override fun startPolling() {
        if (autoPollingConfig.pollRateDuration > 0) {
            this.timer?.cancel()
            this.timer = timer(
                    name = "unleash_toggles_fetcher",
                    initialDelay = 0L,
                    daemon = true,
                    period = autoPollingConfig.pollRateDuration
            ) {
                updateToggles()
                if (!initialized.getAndSet(true)) {
                    initFuture.complete(null)
                }
            }
        } else {
            updateToggles()
            if (!initialized.getAndSet(true)) {
                initFuture.complete(null)
            }
        }
    }

    override fun isPolling(): Boolean {
        return this.timer != null
    }

    private fun updateToggles() {
        try {
            val response = super.fetcher().getTogglesAsync(context).get()
            val cached = super.readToggleCache()
            if (response.isFetched() && cached != response.toggles) {
                logger.trace("Content was not equal")
                super.writeToggleCache(response.toggles) // This will also broadcast updates
            } else if (response.isFailed()) {
                response?.error?.let { e -> super.broadcastTogglesErrored(e) }
            }
        } catch (e: Exception) {
            super.broadcastTogglesErrored(e)
            logger.warn("Exception in AutoPollingCachePolicy", e)
        }
        logger.info("Done checking. Broadcasting check result")
        super.broadcastTogglesChecked()
    }
    override fun close() {
        super.close()
        this.timer?.cancel()
        this.listeners.clear()
        this.errorListeners.clear()
        this.checkListeners.clear()
        this.timer = null
    }
}
