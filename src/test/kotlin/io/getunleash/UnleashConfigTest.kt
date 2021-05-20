package io.getunleash

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UnleashConfigTest {

    @Test
    fun `can build config with builder`() {
        val config =
            UnleashConfig.newBuilder().appName("my-app").environment("default").proxyUrl("https://localhost:4242/proxy")
                .clientSecret("some-secret").build()


        assertThat(config.appName).isEqualTo("my-app")
        assertThat(config.environment).isEqualTo("default")
        assertThat(config.proxyUrl).isEqualTo("https://localhost:4242/proxy")
        assertThat(config.clientSecret).isEqualTo("some-secret")
    }

    @Test
    fun `can build context using normal class constructor`() {
        val config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientSecret = "some-secret", appName = "my-app", environment = "default")
        assertThat(config.appName).isEqualTo("my-app")
        assertThat(config.environment).isEqualTo("default")
        assertThat(config.proxyUrl).isEqualTo("https://localhost:4242/proxy")
        assertThat(config.clientSecret).isEqualTo("some-secret")
    }

    @Test
    fun `Can get a builder from existing context`() {
        val config = UnleashConfig(proxyUrl = "https://localhost:4242/proxy", clientSecret = "some-secret", appName = "my-app", environment = "default")
        val newConfig = config.newBuilder().appName("third").build()
        assertThat(config.appName).isEqualTo("my-app")
        assertThat(newConfig.appName).isEqualTo("third")
    }

    @Test
    fun `Failure to set proxy url or client secret fails the builder`() {
        assertThatThrownBy {
            UnleashConfig.newBuilder().appName("my-app").build()
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("You have to set proxy url in your UnleashConfig")
        assertThatThrownBy {
            UnleashConfig.newBuilder().proxyUrl("http://localhost:4242/proxy").build()
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("You have to set client secret in your UnleashConfig")
    }
}