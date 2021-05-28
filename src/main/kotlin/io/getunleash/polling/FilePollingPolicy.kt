package io.getunleash.polling

import com.fasterxml.jackson.module.kotlin.readValue
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Parser
import io.getunleash.data.ProxyResponse
import io.getunleash.data.Toggle
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

/**
 * Allows loading a proxy response from file.
 * Does not poll, does not update toggles.
 * Is useful when you want to have a known state of all toggles to query against,
 * or if your app's internet connectivity is limited and you'd still like to have toggles.
 * Steps to use
 * 1. Download json file from proxy `curl -XGET -H "Authentication: your-proxy-key" YOURPROXYURL > toggles.json`
 * 2. Include json file in your package
 * 3. Use [io.getunleash.polling.PollingModes.fileMode]
 * 4. Be aware that you'll need to repackage and restart your app to have updates to toggles
 * @param filePollingConfig Configure where we're reading the toggles file from
 */
class FilePollingPolicy(
    override val unleashFetcher: UnleashFetcher,
    override val cache: ToggleCache,
    override val config: UnleashConfig,
    override var context: UnleashContext,
    filePollingConfig: FilePollingMode
) : RefreshPolicy(
    unleashFetcher = unleashFetcher,
    cache = cache,
    logger = LoggerFactory.getLogger(FilePollingPolicy::class.java),
    config = config,
    context = context
) {
    init {
        val togglesInFile: ProxyResponse = Parser.jackson.readValue(filePollingConfig.fileToLoadFrom)
        super.writeToggleCache(togglesInFile.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() })
    }
    override fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>> {
        return CompletableFuture.completedFuture(super.readToggleCache())
    }

}