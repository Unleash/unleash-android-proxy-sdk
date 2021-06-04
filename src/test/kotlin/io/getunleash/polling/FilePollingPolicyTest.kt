package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.io.File

class FilePollingPolicyTest {

    @Test
    fun `can read toggles from file`() {
        val uri = FilePollingPolicy::class.java.classLoader.getResource("proxyresponse.json")!!.toURI()
        val file = File(uri)
        val pollingMode = PollingModes.fileMode(file)
        val filePollingPolicy = FilePollingPolicy(
            unleashFetcher = mock(),
            cache = InMemoryToggleCache(),
            config = UnleashConfig("doesn't matter", clientSecret = ""),
            context = UnleashContext(),
            filePollingConfig = pollingMode as FilePollingMode
        )
        val toggles = filePollingPolicy.getConfigurationAsync().get()
        assertThat(toggles).isNotEmpty()
        assertThat(toggles).containsKey("unleash_android_sdk_demo")
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
            config = UnleashConfig("doesn't matter", clientSecret = ""),
            context = UnleashContext(),
            filePollingConfig = pollingMode as FilePollingMode
        )
        val toggles = filePollingPolicy.getConfigurationAsync().get()
        assertThat(toggles).isNotEmpty
    }
}