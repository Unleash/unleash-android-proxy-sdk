package io.getunleash.polling

abstract class PollingMode {
    abstract fun pollingIdentifier(): String
}