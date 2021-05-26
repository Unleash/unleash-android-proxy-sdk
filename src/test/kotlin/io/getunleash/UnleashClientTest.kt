package io.getunleash

import io.getunleash.polling.PollingModes
import io.getunleash.polling.TestResponses
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

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
            .pollingMode(PollingModes.autoPoll(Duration.ofMillis(500)) {})
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
}