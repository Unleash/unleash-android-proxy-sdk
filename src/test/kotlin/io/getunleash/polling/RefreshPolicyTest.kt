package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.data.Toggle
import java9.util.concurrent.CompletableFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.LongAdder


class RefreshPolicyTest {

    @Test
    fun `can sha256 a string`() {
        val s = RefreshPolicy.sha256("expected")
        assertThat(s).isEqualTo("cea23dd4b87e8b00d19fb9ccaaef93e97353c7353e2070f3baf05aeb3995dff4")
    }

    @Test
    fun `Can add and remove an update listener`() {
        val refreshPolicy = object : RefreshPolicy(
            unleashFetcher = mock(),
            cache = mock(),
            logger = mock(),
            config = UnleashConfig("", ""),
            context = UnleashContext(),
        ) {
            override val isReady: AtomicBoolean = AtomicBoolean(true)

            override fun getConfigurationAsync(): CompletableFuture<Map<String, Toggle>> {
                error("Not applicable")
            }

            override fun startPolling() {}
            override fun isPolling(): Boolean = false
        }
        val checks = LongAdder()
        val togglesUpdatedListener = TogglesUpdatedListener { checks.add(1) }
        refreshPolicy.addTogglesUpdatedListener(togglesUpdatedListener)
        refreshPolicy.broadcastTogglesUpdated()
        refreshPolicy.removeTogglesUpdatedListener(togglesUpdatedListener)
        refreshPolicy.broadcastTogglesUpdated()
        assertThat(checks.sum()).isEqualTo(1)
    }
}