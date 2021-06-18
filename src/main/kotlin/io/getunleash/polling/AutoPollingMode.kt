package io.getunleash.polling

import java.time.Duration

class AutoPollingMode(val pollRateDuration: Duration, val togglesUpdatedListener: TogglesUpdatedListener = TogglesUpdatedListener {  }, val erroredListener: TogglesErroredListener = TogglesErroredListener {  }) : PollingMode {
    override fun pollingIdentifier(): String = "auto"

}