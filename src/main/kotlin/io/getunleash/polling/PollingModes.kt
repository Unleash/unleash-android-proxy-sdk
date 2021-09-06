package io.getunleash.polling

import java.io.File

/**
 * Describes the polling modes
 */
object PollingModes {

    /**
     * Creates a configured auto polling config
     *
     * @param autoPollIntervalSeconds - Sets how often (in seconds) this policy should refresh the cache
     * @return the auto polling config
     */

    fun autoPoll(autoPollIntervalSeconds: Long): PollingMode {
        return AutoPollingMode(autoPollIntervalSeconds * 1000)
    }

    /**
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated
     * @param autoPollIntervalSeconds - Sets how often (in seconds) this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun autoPoll(autoPollIntervalSeconds: Long, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(autoPollIntervalSeconds * 1000, listener)
    }

    /**
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated
     * @param intervalInMs - Sets intervalInMs for how often this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun autoPollMs(intervalInMs: Long, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(intervalInMs, listener)
    }

    fun fileMode(toggleFile: File): PollingMode {
        return FilePollingMode(toggleFile)
    }


}