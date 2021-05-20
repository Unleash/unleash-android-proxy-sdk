package io.getunleash.polling

class AutoPollingMode(val pollRateInSeconds: Long, val togglesUpdatedListeners: List<ToggleUpdatedListener> = emptyList()) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}