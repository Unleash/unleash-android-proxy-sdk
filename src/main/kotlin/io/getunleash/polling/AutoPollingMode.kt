package io.getunleash.polling

/**
 * @param pollRateDuration - How long (in seconds) between each poll
 * @param togglesUpdatedListener - A listener that will be notified each time a poll actually updates the evaluation result
 * @param erroredListener - A listener that will be notified each time a poll fails. The notification will include the Exception
 * @param togglesCheckedListener - A listener that will be notified each time a poll completed. Will be called regardless of the check succeeded or failed.
 * @param readyListener - A listener that will be notified after the poller is done instantiating, i.e. has an evaluation result in its cache. Each ready listener will receive only one notification
 * @param pollImmediate - Set to true, the poller will immediately poll for configuration and then call the ready listener. Set to false, you will need to call [startPolling()) to actually talk to proxy/Edge
 */
class AutoPollingMode(val pollRateDuration: Long,
                      val togglesUpdatedListener: TogglesUpdatedListener? = null,
                      val erroredListener: TogglesErroredListener? = null,
                      val togglesCheckedListener: TogglesCheckedListener? = null,
                      val readyListener: ReadyListener? = null,
                      val pollImmediate: Boolean = true) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}
