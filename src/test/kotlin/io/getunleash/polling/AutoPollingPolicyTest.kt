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
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Duration
import java.util.concurrent.CompletableFuture

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
            config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientSecret = "some-secret"),
            context = UnleashContext(),
            autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
    }

    @Test
    fun `same response does not update cache`() {
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
            on { read(anyString()) } doReturn result
        }
        val policy = AutoPollingPolicy(
            unleashFetcher = unleashFetcher,
            cache = toggleCache,
            config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientSecret = "some-secret"),
            context = UnleashContext(),
            autoPollingConfig = PollingModes.autoPoll(2) as AutoPollingMode
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(result)
        verify(toggleCache, never()).write(anyString(), eq(result))
    }

    @Test
    fun togglesChanged() {
        val server = MockWebServer()
        val isCalled = CompletableFuture<Unit>()
        val pollingMode = PollingModes.autoPoll(Duration.ofMillis(20)) { isCalled.complete(null) }
        val config = UnleashConfig.newBuilder().proxyUrl(server.url("/").toString()).clientSecret("my-secret").build()
        val fetcher = UnleashFetcher(unleashConfig = config)
        val cache = InMemoryToggleCache()
        val policy = AutoPollingPolicy(unleashFetcher = fetcher, cache = cache, config = config, context = UnleashContext(), pollingMode as AutoPollingMode)
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.complicatedVariants))
        assertThat(isCalled).succeedsWithin(Duration.ofMillis(500))
        server.close()
        policy.close()
    }
}