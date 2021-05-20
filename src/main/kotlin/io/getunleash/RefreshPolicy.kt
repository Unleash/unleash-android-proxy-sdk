package io.getunleash

import io.getunleash.data.Toggle
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.Logger
import java.io.Closeable
import java.util.concurrent.CompletableFuture

abstract class RefreshPolicy(
    val unleashFetcher: UnleashFetcher,
    val cache: ToggleCache,
    val logger: Logger,
    val config: UnleashConfig,
    val context: UnleashContext
) : Closeable {
    var inMemoryConfig: Map<String, Toggle> = emptyMap()
    val cacheKey = String(DigestUtils.sha256Hex(cacheBase.format(config.clientSecret)).toByteArray())

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

    /**
     * Through this getter, child classes can use our fetcher to get the latest toggles over HTTP
     *
     * @return the fetcher
     */
    fun fetcher(): UnleashFetcher {
        return this.unleashFetcher
    }

    fun refreshAsync(): CompletableFuture<Void> {
        return this.fetcher().getResponseAsync(context).thenAcceptAsync { response ->
            if (response.isFetched()) {
                this.writeToggleCache(response.config!!.toggles.groupBy { it.name }.mapValues { it.value.first() })
            }
        }
    }

    fun getLatestCachedValue(): Map<String, Toggle> = this.inMemoryConfig

    override fun close() {
        this.unleashFetcher.close()
    }
}