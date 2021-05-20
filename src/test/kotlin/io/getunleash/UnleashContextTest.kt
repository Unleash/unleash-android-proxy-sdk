package io.getunleash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UnleashContextTest {

    @Test
    fun `can build context with builder`() {
        val ctx = UnleashContext.newBuilder().appName("my-app").addProperty("somekey", "somevalue")
            .addProperty("someotherkey", "someothervalue")
            .remoteAddress("127.0.0.1")
            .build()

        assertThat(ctx.properties).containsEntry("somekey", "somevalue").containsEntry("someotherkey", "someothervalue")
        assertThat(ctx.remoteAddress).isEqualTo("127.0.0.1")
        assertThat(ctx.appName).isEqualTo("my-app")
    }

    @Test
    fun `can build context using normal class constructor`() {
        val ctx = UnleashContext(properties = mapOf("somekey" to "somevalue", "someotherkey" to "someothervalue"), remoteAddress = "127.0.0.1", appName = "my-app")
        assertThat(ctx.properties).containsEntry("somekey", "somevalue").containsEntry("someotherkey", "someothervalue")
        assertThat(ctx.remoteAddress).isEqualTo("127.0.0.1")
        assertThat(ctx.appName).isEqualTo("my-app")
    }

    @Test
    fun `Can get a builder from existing context`() {
        val ctx = UnleashContext(properties = mapOf("somekey" to "somevalue", "someotherkey" to "someothervalue"), remoteAddress = "127.0.0.1", appName = "my-app")
        val newCtx = ctx.newBuilder().addProperty("third", "3").build()
        assertThat(ctx.properties).doesNotContainEntry("third", "3")
        assertThat(newCtx.properties).containsEntry("third", "3")
    }

}