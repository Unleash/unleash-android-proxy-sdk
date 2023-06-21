package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import io.getunleash.data.Status
import io.getunleash.data.Toggle
import io.getunleash.data.ToggleResponse
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.time.Duration
import java9.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.LongAdder

class AutoPollingPolicyTest {


    @Test
    fun `if cache is empty tries to fetch`() {
        val result = mapOf("variantToggle" to Toggle("variantToggle", enabled = false))
        val unleashFetcher = mock<UnleashFetcher> {
            on { getTogglesAsync(any<UnleashContext>()) } doReturn CompletableFuture.completedFuture(
                ToggleResponse(
                    Status.FETCHED,
                    result
                )
            )
        }
        val toggleCache = mock<ToggleCache> {
            on { read(anyString()) } doThrow IllegalStateException()
            on { write(anyString(), any()) } doThrow IllegalStateException()
        }

        val policy = AutoPollingPolicy(
            unleashFetcher = unleashFetcher,
            cache = toggleCache,
            config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientKey = "some-key"),
            context = UnleashContext(),
            autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
    }

    @Test
    fun `same response does not update cache`() {
        val result = mapOf("variantToggle" to Toggle("variantToggle", enabled = false))

        val unleashFetcher = mock<UnleashFetcher> {
            on { getTogglesAsync(any()) } doReturn CompletableFuture.completedFuture(
                ToggleResponse(
                    Status.FETCHED,
                    result
                )
            )
        }
        val toggleCache = mock<ToggleCache> {
            on { read(anyString()) } doReturn result
        }
        val policy = AutoPollingPolicy(
            unleashFetcher = unleashFetcher,
            cache = toggleCache,
            config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientKey = "some-key"),
            context = UnleashContext(),
            autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
        verify(toggleCache, never()).write(anyString(), eq(result))
    }

    @Test
    fun `same response still sends a toggles checked update`() {
        val result = mapOf("variantToggle" to Toggle("variantToggle", enabled = false))

        val unleashFetcher = mock<UnleashFetcher> {
            on { getTogglesAsync(any()) } doReturn CompletableFuture.completedFuture(
                    ToggleResponse(
                            Status.FETCHED,
                            result
                    )
            )
        }
        val toggleCache = mock<ToggleCache> {
            on { read(anyString()) } doReturn result
        }
        val checks = LongAdder()
        val checkListener = TogglesCheckedListener { checks.add(1) }
        val policy = AutoPollingPolicy(
                unleashFetcher = unleashFetcher,
                cache = toggleCache,
                config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientKey = "some-key"),
                context = UnleashContext(),
                autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        )
        policy.addTogglesCheckedListener(checkListener)
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
        verify(toggleCache, never()).write(anyString(), eq(result))
        assertThat(checks.sum()).isEqualTo(1)

    }

    @Test
    fun `Can fetch once when asked to check`() {
        val result = mapOf("variantToggle" to Toggle("variantToggle", enabled = false))

        val unleashFetcher = mock<UnleashFetcher> {
            on { getTogglesAsync(any()) } doReturn CompletableFuture.completedFuture(
                    ToggleResponse(
                            Status.FETCHED,
                            result
                    )
            )
        }
        val ready = AtomicBoolean(false)
        val readyListener = ReadyListener {
            ready.set(true)
        }
        val toggleCache = mock<ToggleCache> {
            on { read(anyString()) } doReturn result
        }
        val policy = AutoPollingPolicy(
                unleashFetcher = unleashFetcher,
                cache = toggleCache,
                config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientKey = "some-key"),
                context = UnleashContext(),
                autoPollingConfig = PollingModes.fetchOnce(listener = { }, readyListener = readyListener) as AutoPollingMode
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
        verify(toggleCache, never()).write(anyString(), eq(result))
        assertThat(policy.isReady).isTrue
        assertThat(ready.get()).isTrue
    }
    @Test
    fun `yields correct identifier`() {
        val f = PollingModes.autoPoll(5)
        assertThat(f.pollingIdentifier()).isEqualTo("auto")
    }

    @Test
    fun togglesChanged() {
        val server = MockWebServer()
        val isCalled = CompletableFuture<Unit>()
        val pollingMode = PollingModes.autoPoll(20) { isCalled.complete(null) }
        val config = UnleashConfig.newBuilder().proxyUrl(server.url("/").toString()).clientKey("my-secret").build()
        val fetcher = UnleashFetcher(unleashConfig = config)
        val cache = InMemoryToggleCache()
        val policy = AutoPollingPolicy(unleashFetcher = fetcher, cache = cache, config = config, context = UnleashContext(), pollingMode as AutoPollingMode)
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestResponses.complicatedVariants))
        assertThat(isCalled).succeedsWithin(Duration.ofSeconds(5))
        server.close()
        policy.close()
    }

    @Test
    fun `if fetching fails error is delegated to listeners`() {
        val someException = mock<Exception>()
        val unleashFetcher = mock<UnleashFetcher> {
            on { getTogglesAsync(any<UnleashContext>()) } doReturn CompletableFuture.completedFuture(
                ToggleResponse(
                    Status.FAILED,
                    error = someException
                )
            )
        }
        val errorListener = mock<TogglesErroredListener>()

        AutoPollingPolicy(
            unleashFetcher = unleashFetcher,
            cache = mock(),
            config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientKey = "some-key"),
            context = UnleashContext(),
            autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        ).apply {
            addTogglesErroredListener(errorListener)
        }.getConfigurationAsync().get()

        verify(errorListener, times(1)).onError(someException)
    }
}
