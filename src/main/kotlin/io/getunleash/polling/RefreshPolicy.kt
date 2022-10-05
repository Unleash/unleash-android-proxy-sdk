package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Toggle
import org.slf4j.Logger
import java.io.Closeable
import java.math.BigInteger
import java.security.MessageDigest
import java9.util.concurrent.CompletableFuture

/**
 * Used to define how to Refresh and serve toggles
 * @param unleashFetcher How to fetch toggles
 * @param cache The toggle cache
 * @param logger Allowing for logging with correct classname
 * @param config Configuring unleash
 * @param context Configuring context
 */
abstract class RefreshPolicy(
    open val unleashFetcher: UnleashFetcher,
    open val cache: ToggleCache,
    val logger: Logger,
    open val config: UnleashConfig,
    open var context: UnleashContext
) : Closeable {
    internal val listeners: MutableList<TogglesUpdatedListener> = mutableListOf()
    internal val errorListeners: MutableList<TogglesErroredListener> = mutableListOf()
    private var inMemoryConfig: Map<String, Toggle> = emptyMap()
    private val cacheKey: String by lazy { sha256(cacheBase.format(this.config.clientKey)) }

    companion object {
        const val cacheBase = "android_${UnleashFetcher.TOGGLE_BACKUP_NAME}_%s"
        fun sha256(s: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(s.toByteArray(Charsets.UTF_8))
            val number = BigInteger(1, digest)
            return number.toString(16).padStart(32, '0')
        }
    }

    fun readToggleCache(): Map<String, Toggle> {
        return try {
            this.cache.read(cacheKey)
        } catch (e: Exception) {
            inMemoryConfig
        }
    }

    fun writeToggleCache(value: Map<String, Toggle>) {
        try {
            this.inMemoryConfig = value
            this.cache.write(cacheKey, value)
        } catch (e: Exception) {
        }
    }

    /**
     * Subclasses should override this to implement their way of updating the toggle cache
     */
    abstract fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>>

    /**
     * Through this getter, child classes can use our fetcher to get the latest toggles over HTTP
     *
     * @return the fetcher
     */
    fun fetcher(): UnleashFetcher {
        return this.unleashFetcher
    }

    fun refreshAsync(): CompletableFuture<Void> {
        return this.fetcher().getTogglesAsync(context).thenAcceptAsync { response ->
            if (response.isFetched()) {
                this.writeToggleCache(response.toggles)
            }
        }
    }

    /**
     * Subclasses should override this to implement their way of manually starting polling after context is updated.
     * Typical usage would be to use [PollingModes.manuallyStartPolling] or [PollingModes.manuallyStartedPollMs] to create/configure your polling mode,
     * and then call [startPolling()] on the Unleash client
     */
    abstract fun startPolling()

    /**
     * Subclasses should override this to signal to Unleash whether or not they're actively polling
     */
    abstract fun isPolling(): Boolean

    fun getLatestCachedValue(): Map<String, Toggle> = this.inMemoryConfig

    override fun close() {
        this.unleashFetcher.close()
    }

    fun addTogglesUpdatedListener(listener: TogglesUpdatedListener): Unit {
        listeners.add(listener)
    }

    fun addTogglesErroredListener(errorListener: TogglesErroredListener): Unit {
        errorListeners.add(errorListener)
    }
}
