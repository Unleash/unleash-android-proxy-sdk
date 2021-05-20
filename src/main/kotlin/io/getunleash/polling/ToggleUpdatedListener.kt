package io.getunleash.polling

/**
 * The interface which exposes the toggles updated event from [io.getunleash.polling.AutoPollingPolicy]
 */
interface ToggleUpdatedListener {
    /**
     * This method will be called when a toggles updated event is fired.
     */
    fun onTogglesUpdated(): Unit
}