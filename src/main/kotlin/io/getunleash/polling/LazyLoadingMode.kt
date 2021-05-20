package io.getunleash.polling

class LazyLoadingMode(val refreshIntervalInSeconds:  Long, val asyncRefresh: Boolean) : PollingMode {
    override fun pollingIdentifier(): String = "lazy"
}