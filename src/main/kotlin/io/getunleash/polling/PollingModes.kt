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
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated.
     * However, this does not start polling until the user explicity calls [startPolling()]
     * @param autoPollIntervalSeconds - Sets how often (in seconds) this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun manuallyStartPolling(autoPollIntervalSeconds: Long, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(pollRateDuration = autoPollIntervalSeconds * 1000, togglesUpdatedListener = listener, pollImmediate = false)
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

    /**
     * Creates a configured auto polling config with a listener which receives updates when/if toggles get updated, but does not start polling automatically.
     * For that you'll need to call startPolling() on the Policy
     * @param intervalInMs - Sets intervalInMs for how often this policy should refresh the cache
     * @param listener - What should the poller call when toggles are updated?
     * @return the auto polling config
     */
    fun manuallyStartedPollMs(intervalInMs: Long, listener: TogglesUpdatedListener): PollingMode {
        return AutoPollingMode(pollRateDuration = intervalInMs, togglesUpdatedListener = listener, pollImmediate = false)
    }

    fun fileMode(toggleFile: File): PollingMode {
        return FilePollingMode(toggleFile)
    }


}
