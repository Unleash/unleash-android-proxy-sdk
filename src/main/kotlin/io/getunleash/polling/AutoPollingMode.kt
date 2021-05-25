package io.getunleash.polling

class AutoPollingMode(val pollRateInSeconds: Long, val togglesUpdatedListener: ToggleUpdatedListener? = null) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}