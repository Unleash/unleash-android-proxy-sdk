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
        server.enqueue(MockResponse().setBody(TestResponses.threeToggles))
        config = UnleashConfig.newBuilder()
            .pollingMode(PollingModes.autoPoll(500) {})
            .proxyUrl(server.url("/proxy").toString())
            .clientKey("some-key")
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

    @Test
    fun `Updating context causes the URL used to include new parameters`() {
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { client ->
            Thread.sleep(800)
            val newCtx = context.newBuilder().addProperty("test2", "test2value").build()
            assertThat(client.updateContext(newCtx)).succeedsWithin(Duration.ofSeconds(2))
            val first = server.takeRequest() // initial request
            assertThat(first.requestUrl!!.queryParameterNames).doesNotContain("properties[test2]")
            val second = server.takeRequest() // after updateContext
            assertThat(second.requestUrl!!.queryParameterNames).contains("properties[test2]")
        }
    }

    // Copied from unleash-proxy-client-response
    @Test
    fun `Should include context fields on request`() {
        val context = UnleashContext.newBuilder().appName("web")
            .environment("prod")
            .userId("123")
            .sessionId("456")
            .remoteAddress("address")
            .addProperty("property1", "property1")
            .addProperty("property2", "property2")
            .build()
        UnleashClient.newBuilder().unleashConfig(config).unleashContext(context).build().use { _ ->
            Thread.sleep(500)
            val request = server.takeRequest()
            val url = request.requestUrl!!
            assertThat(url.queryParameter("userId")).isEqualTo("123")
            assertThat(url.queryParameter("sessionId")).isEqualTo("456")
            assertThat(url.queryParameter("userId")).isEqualTo("123")
            assertThat(url.queryParameter("sessionId")).isEqualTo("456")
            assertThat(url.queryParameter("remoteAddress")).isEqualTo("address")
            assertThat(url.queryParameter("properties[property1]")).isEqualTo("property1")
            assertThat(url.queryParameter("properties[property2]")).isEqualTo("property2")
            assertThat(url.queryParameter("appName")).isEqualTo("web")
            assertThat(url.queryParameter("environment")).isEqualTo("prod")
        }
    }


    @Test
    fun `Can configure a client that does not poll until requested to do so`() {
        val manuallyStartedConfig = config.newBuilder().pollingMode(PollingModes.manuallyStartPolling(500) {}).build()
        context = UnleashContext.newBuilder().appName("unleash-android-proxy-sdk").userId("some-user-id").build()
        UnleashClient.newBuilder().unleashConfig(manuallyStartedConfig).unleashContext(context).build().use { client ->
            val updatedFuture = CompletableFuture<Void>()
            client.addTogglesUpdatedListener { updatedFuture.complete(null) }
            assertThat(client.isPolling()).isFalse
            Thread.sleep(2000)
            assertThat(updatedFuture).isNotCompleted
            assertThat(client.isEnabled("variantToggle")).isFalse
            client.startPolling()
            assertThat(client.isPolling()).isTrue
            assertThat(updatedFuture).succeedsWithin(Duration.ofSeconds(2))
        }
    }
}
