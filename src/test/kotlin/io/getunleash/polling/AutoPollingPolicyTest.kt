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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

class AutoPollingPolicyTest {
    val complicatedVariants = """{
            	"toggles": [
                    {
                        "name": "variantToggle",
                        "enabled": true,
                        "variant": {
                            "name": "green",
                            "payload": {
                                "type": "number",
                                "value": 54
                            }
                        }
                    }, {
                        "name": "featureToggle",
                        "enabled": true,
                        "variant": {
                            "name": "disabled"
                        }
                    }, {
                        "name": "simpleToggle",
                        "enabled": true
                        "variant": {
                            "name": "red",
                            "payload": {
                                "type": "json",
                                "value": { "key": "value" }
                            }
                        }
                    }
                ]
            }""".trimIndent()

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
        val isCalled = AtomicBoolean()
        val pollingMode = PollingModes.autoPoll(2, object: ToggleUpdatedListener {
            override fun onTogglesUpdated() {
                isCalled.set(true)
            }
        })
        val config = UnleashConfig.newBuilder().proxyUrl(server.url("/").toString()).clientSecret("my-secret").build()
        val fetcher = UnleashFetcher(unleashConfig = config)
        val cache = InMemoryToggleCache()
        val policy = AutoPollingPolicy(unleashFetcher = fetcher, cache = cache, config = config, context = UnleashContext(), pollingMode as AutoPollingMode)
        server.enqueue(MockResponse().setResponseCode(200).setBody(complicatedVariants))
        Thread.sleep(1000)
        assertThat(isCalled.get()).isTrue
        server.close()
        policy.close()
    }
}