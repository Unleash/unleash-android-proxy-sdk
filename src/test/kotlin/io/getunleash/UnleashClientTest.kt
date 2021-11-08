package io.getunleash

import io.getunleash.polling.PollingModes
import io.getunleash.polling.TestResponses
import io.getunleash.polling.TogglesErroredListener
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class UnleashClientTest {
    lateinit var server: MockWebServer
    lateinit var config: UnleashConfig
    lateinit var context: UnleashContext

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        server.enqueue(MockResponse().setBody(TestResponses.threeToggles))
        config = UnleashConfig.newBuilder()
            .pollingMode(PollingModes.autoPoll(500) {})
            .proxyUrl(server.url("/proxy").toString())
            .clientSecret("some-secret")
            .build()
        context = UnleashContext.newBuilder().appName("unleash-android-proxy-sdk").userId("some-user-id").build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `Can check toggle status`() {
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            assertThat(client.isEnabled("variantToggle")).isFalse
            Thread.sleep(5000)
            assertThat(client.isEnabled("variantToggle")).isTrue
        }
    }

    @Test
    fun `Can get variant`() {
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            assertThat(client.getVariant("variantToggle").name).isEqualTo("disabled")
            Thread.sleep(5000)
            assertThat(client.getVariant("variantToggle").name).isEqualTo("green")
        }
    }

    @Test
    fun `Updating context causes an immediate get`() {
        server.enqueue(MockResponse().setBody(TestResponses.complicatedVariants))
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            val newCtx = context.newBuilder().sessionId("session-2").build()
            assertThat(client.updateContext(newCtx)).succeedsWithin(Duration.ofSeconds(2))
            assertThat(server.requestCount).isEqualTo(2) // one for poller, one for updateContext call
        }
    }

    @Test
    fun `Not having an UnleashConfig throws an exception`() {
        assertThrows<IllegalStateException> {
            UnleashClient.newBuilder().build()
        }
    }

    @Test
    fun `Minimal config is just an unleash config`() {
        assertDoesNotThrow {
            UnleashClient.newBuilder().unleashConfig(config).build()
        }
    }

    @Test
    fun `Default http client when building has a cache`() {
        val client = UnleashClient.newBuilder().unleashConfig(config).build()
        assertThat(client.httpClient.cache).isNotNull
    }
    @Test
    fun `Can override http client`() {
        val client = UnleashClient.newBuilder().unleashConfig(config)
            .httpClient(OkHttpClient.Builder().readTimeout(15, TimeUnit.SECONDS).build()).build()
        assertThat(client.httpClient.cache).isNull()
    }

    @Test
    fun `Can add update listeners to client`() {
        UnleashClient.newBuilder().unleashConfig(config).build().use { client ->
            val updatedFuture = CompletableFuture<Void>()
            client.addTogglesUpdatedListener { updatedFuture.complete(null) }
            assertThat(updatedFuture).succeedsWithin(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `Can add error listeners to client`() {
        server.enqueue(MockResponse().setResponseCode(500))
        UnleashClient.newBuilder().unleashConfig(config).build().use { client ->
            val updatedFuture = CompletableFuture<Void>()
            client.addTogglesErroredListener { e -> updatedFuture.completeExceptionally(e) }
            assertThat(updatedFuture).failsWithin(Duration.ofSeconds(2))
        }
    }

    @Test
    fun `Can check unknown toggle status with default value of false`() {
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            assertThat(client.isEnabled("unknownToggle")).isFalse
            Thread.sleep(5000)
            assertThat(client.isEnabled("unknownToggle")).isFalse
        }
    }

    @Test
    fun `Can check unknown toggle status with default value of true`() {
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            assertThat(client.isEnabled("unknownToggle", true)).isTrue
            Thread.sleep(5000)
            assertThat(client.isEnabled("unknownToggle", true)).isTrue
        }
    }
}
