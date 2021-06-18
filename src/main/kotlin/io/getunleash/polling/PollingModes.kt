package io.getunleash.polling

import java.io.File
import java.time.Duration

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
        return AutoPollingMode(Duration.ofSeconds(autoPollIntervalSeconds))
    }

    /**
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated
     * @param autoPollIntervalSeconds - Sets how often (in seconds) this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun autoPoll(autoPollIntervalSeconds: Long, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(Duration.ofSeconds(autoPollIntervalSeconds), listener)
    }

    /**
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated
     * @param duration - Sets how often as a duration this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun autoPoll(duration: Duration, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(duration, listener)
    }

    fun fileMode(toggleFile: File): PollingMode {
        return FilePollingMode(toggleFile)
    }


}