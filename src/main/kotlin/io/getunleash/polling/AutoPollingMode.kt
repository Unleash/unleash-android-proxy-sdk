package io.getunleash.polling

class AutoPollingMode(val pollRateDuration: Long, val togglesUpdatedListener: TogglesUpdatedListener = TogglesUpdatedListener {  }, val erroredListener: TogglesErroredListener = TogglesErroredListener {  }, val pollImmediate: Boolean = true) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}
