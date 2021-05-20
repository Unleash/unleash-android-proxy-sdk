package io.getunleash.polling

import io.getunleash.UnleashConfig
import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.cache.ToggleCache
import io.getunleash.polling.TestReponses.toToggleMap
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class LazyLoadingPolicySyncTest {
    @Test
    fun `can get config`() {
        val server = MockWebServer()
        val unleashConfig = UnleashConfig(
            proxyUrl = server.url("/proxy").toString(),
            clientSecret = "some-secret",
            httpClientConnectionTimeout = 2L,
            httpClientReadTimeout = 5L
        )
        val mode = PollingModes.lazyLoad(5)
        val unleashFetcher = UnleashFetcher(unleashConfig)
        val cache = InMemoryToggleCache()
        val start = Instant.now()
        val clock = Clock.fixed(start, ZoneOffset.UTC)
        val policy = LazyLoadingPolicy(
            unleashFetcher = unleashFetcher,
            cache = cache,
            config = unleashConfig,
            lazyLoadingMode = mode as LazyLoadingMode
        )
        policy.clock = clock
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.threeToggles))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(TestReponses.complicatedVariants)
                .setBodyDelay(1, TimeUnit.SECONDS)
        )

        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.threeToggles.toToggleMap())
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.threeToggles.toToggleMap())
        policy.clock = Clock.fixed(start.plusSeconds(6), ZoneOffset.UTC)
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.complicatedVariants.toToggleMap())
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `handles cache failing`() {
        val server = MockWebServer()
        val unleashConfig = UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "some-secret")
        val mode = PollingModes.lazyLoad(5)
        val unleashFetcher = UnleashFetcher(unleashConfig)
        val failingCache = mock<ToggleCache> {
            on { read(anyString()) } doThrow IllegalStateException()
            on { write(anyString(), any()) } doThrow IllegalStateException()
        }
        val start = Instant.now()
        val clock = Clock.fixed(start, ZoneOffset.UTC)
        val policy = LazyLoadingPolicy(
            unleashFetcher = unleashFetcher,
            cache = failingCache,
            config = unleashConfig,
            lazyLoadingMode = mode as LazyLoadingMode
        )
        policy.clock
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.threeToggles))
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(TestReponses.complicatedVariants)
                .setBodyDelay(1, TimeUnit.SECONDS)
        )
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.threeToggles.toToggleMap())
        policy.clock = Clock.fixed(start.plusSeconds(6), ZoneOffset.UTC)
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.complicatedVariants.toToggleMap())
    }

    @Test
    fun `handles failing refresh by keeping old state`() {
        val server = MockWebServer()
        val unleashConfig = UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "some-secret")
        val mode = PollingModes.lazyLoad(5)
        val unleashFetcher = UnleashFetcher(unleashConfig)
        val cache = InMemoryToggleCache()
        val start = Instant.now()
        val clock = Clock.fixed(start, ZoneOffset.UTC)
        val policy = LazyLoadingPolicy(
            unleashFetcher = unleashFetcher,
            cache = cache,
            config = unleashConfig,
            lazyLoadingMode = mode as LazyLoadingMode
        )
        policy.clock = clock
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.threeToggles))
        server.enqueue(MockResponse().setResponseCode(500))
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.threeToggles.toToggleMap())
        policy.clock = Clock.fixed(start.plusSeconds(6), ZoneOffset.UTC)
        assertThat(
            policy.getConfigurationAsync().get()
        ).isEqualTo(TestReponses.threeToggles.toToggleMap()) // Same because refresh failure

    }

    @Test
    fun `same response from server does not update cache`() {
        val server = MockWebServer()
        val unleashConfig = UnleashConfig(proxyUrl = server.url("/proxy").toString(), clientSecret = "some-secret")
        val mode = PollingModes.lazyLoad(5)
        val unleashFetcher = UnleashFetcher(unleashConfig)
        val result = TestReponses.threeToggles.toToggleMap()
        val cache: ToggleCache = mock {
            on { read(anyString()) } doReturn result
        }
        val start = Instant.now()
        val clock = Clock.fixed(start, ZoneOffset.UTC)
        val policy = LazyLoadingPolicy(
            unleashFetcher = unleashFetcher,
            cache = cache,
            config = unleashConfig,
            lazyLoadingMode = mode as LazyLoadingMode
        )
        policy.clock = clock
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.threeToggles))
        server.enqueue(MockResponse().setResponseCode(200).setBody(TestReponses.threeToggles))
        assertThat(policy.getConfigurationAsync().get()).isEqualTo(TestReponses.threeToggles.toToggleMap())
        policy.clock = Clock.fixed(start.plusSeconds(6), ZoneOffset.UTC)
        assertThat(
            policy.getConfigurationAsync().get()
        ).isEqualTo(TestReponses.threeToggles.toToggleMap()) // Same because refresh failure
        verify(cache, never()).write(anyString(), eq(result))


    }
}