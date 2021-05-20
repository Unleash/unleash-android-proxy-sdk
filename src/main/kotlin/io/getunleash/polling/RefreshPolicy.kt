package io.getunleash.polling

import io.getunleash.cache.ToggleCache
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.data.Toggle
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.Logger
import java.io.Closeable
import java.util.concurrent.CompletableFuture

abstract class RefreshPolicy(
    open val unleashFetcher: UnleashFetcher,
    open val cache: ToggleCache,
    val logger: Logger,
    open val config: UnleashConfig,
    open val context: UnleashContext
) : Closeable {
    private var inMemoryConfig: Map<String, Toggle> = emptyMap()
    private val cacheKey: String by lazy { String(DigestUtils.sha256Hex(cacheBase.format(this.config.clientSecret)).toByteArray()) }

    companion object {
        val cacheBase = "android_${UnleashFetcher.TOGGLE_BACKUP_NAME}_%s"
    }

    fun readToggleCache(): Map<String, Toggle> {
        return try {
            this.cache.read(cacheKey)
        } catch (e: Exception) {
            logger.warn("An error occurred when reading toggle cache", e)
            inMemoryConfig
        }
    }

    fun writeToggleCache(value: Map<String, Toggle>) {
        try {
            this.inMemoryConfig = value
            this.cache.write(cacheKey, value)
        } catch (e: Exception) {
            logger.warn("An error occurred when writing the cache", e)
        }
    }


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

    fun getLatestCachedValue(): Map<String, Toggle> = this.inMemoryConfig

    override fun close() {
        this.unleashFetcher.close()
    }
}