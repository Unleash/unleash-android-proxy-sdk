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
        return AutoPollingMode(autoPollIntervalSeconds, listener)
    }
}