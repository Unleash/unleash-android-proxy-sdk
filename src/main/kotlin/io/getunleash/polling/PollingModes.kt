package io.getunleash.polling

/**
 * Describes the polling modes
 */
object PollingModes {

    /**
     * Creates a configured auto polling config
     *
     * @param autoPollIntervalSeconds - Sets how often this policy should refresh the cache
     * @return the auto polling config
     */

    fun autoPoll(autoPollIntervalSeconds: Long): PollingMode {
        return AutoPollingMode(autoPollIntervalSeconds)
    }

    fun autoPoll(autoPollIntervalSeconds: Long, listener: ToggleUpdatedListener): PollingMode {
        return AutoPollingMode(autoPollIntervalSeconds, listOf(listener))
    }

    /**
     * Creates a configured lazy loading polling configuration.
     *
     * @param cacheRefreshIntervalInSeconds Sets how long the cache will store its value before fetching the latest from the network again.
     * @param asyncRefresh Sets whether the cache should refresh itself asynchronously or synchronously.
     * <p>If it's set to {@code true} reading from the policy will not wait for the refresh to be finished,
     * instead it returns immediately with the previous stored value.</p>
     * <p>If it's set to {@code false} the policy will wait until the expired
     * value is being refreshed with the latest configuration.</p>
     * @return the lazy loading polling configuration.
     */
    fun lazyLoad(cacheRefreshIntervalInSeconds: Long, asyncRefresh: Boolean = false): PollingMode {
        return LazyLoadingMode(cacheRefreshIntervalInSeconds, asyncRefresh)
    }
}