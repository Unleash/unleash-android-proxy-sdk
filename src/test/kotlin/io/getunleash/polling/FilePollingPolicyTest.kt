package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.InMemoryToggleCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class FilePollingPolicyTest {

    @Test
    fun `can read toggles from file`() {
        val uri = FilePollingPolicy::class.java.classLoader.getResource("proxyresponse.json")!!.toURI()
        val file = File(uri)
        val pollingMode = PollingModes.fileMode(file)
        val filePollingPolicy = FilePollingPolicy(
            unleashFetcher = mock(),
            cache = InMemoryToggleCache(),
            config = UnleashConfig("doesn't matter", clientKey = ""),
            context = UnleashContext(),
            filePollingConfig = pollingMode as FilePollingMode
        )
        val toggles = filePollingPolicy.getConfigurationAsync().get()
        assertThat(toggles).isNotEmpty()
        assertThat(toggles).containsKey("unleash_android_sdk_demo")
    }

    @Test
    fun `broadcasts the ready event once it has read from file`() {

        val uri = FilePollingPolicy::class.java.classLoader.getResource("proxyresponse.json")!!.toURI()
        val file = File(uri)
        val ready = AtomicBoolean(false)
        val pollingMode = PollingModes.fileMode(file) { ready.set(true) }
        val filePollingPolicy = FilePollingPolicy(
                unleashFetcher = mock(),
                cache = InMemoryToggleCache(),
                config = UnleashConfig("doesn't matter", clientKey = ""),
                context = UnleashContext(),
                filePollingConfig = pollingMode as FilePollingMode
        )
        val toggles = filePollingPolicy.getConfigurationAsync().get()
        assertThat(toggles).isNotEmpty()
        assertThat(toggles).containsKey("unleash_android_sdk_demo")
        assertThat(ready.get()).isTrue
    }

    @Test
    fun `yields correct identifier`() {
        val pollMode = PollingModes.fileMode(File(""))
        assertThat(pollMode.pollingIdentifier()).isEqualTo("file")
    }

    @Test
    fun `it does not poll and does not refresh`() {
        val uri = FilePollingPolicy::class.java.classLoader.getResource("proxyresponse.json")!!.toURI()
        val file = File(uri)
        val pollingMode = PollingModes.fileMode(file)
        val fetcher: UnleashFetcher = mock()
        val filePollingPolicy = FilePollingPolicy(
            unleashFetcher = fetcher,
            cache = InMemoryToggleCache(),
            config = UnleashConfig("doesn't matter", clientKey = ""),
            context = UnleashContext(),
            filePollingConfig = pollingMode as FilePollingMode
        )
        val toggles = filePollingPolicy.getConfigurationAsync().get()
        assertThat(toggles).isNotEmpty
    }
}
