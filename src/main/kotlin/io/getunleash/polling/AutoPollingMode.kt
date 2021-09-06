package io.getunleash.polling

class AutoPollingMode(val pollRateDuration: Long, val togglesUpdatedListener: TogglesUpdatedListener = TogglesUpdatedListener {  }, val erroredListener: TogglesErroredListener = TogglesErroredListener {  }) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}