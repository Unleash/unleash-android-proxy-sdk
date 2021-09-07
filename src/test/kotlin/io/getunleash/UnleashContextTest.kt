package io.getunleash

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

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

    @Test
    fun `Can set environment with builder`() {
        val ctx = UnleashContext(properties = mapOf("somekey" to "somevalue", "someotherkey" to "someothervalue"), remoteAddress = "127.0.0.1", appName = "my-app")
        val updatedCtx = ctx.newBuilder().environment("dev").build()
        assertThat(updatedCtx.environment).isEqualTo("dev")
    }

    @Test
    fun `Can set instanceId with builder`() {
        val ctx = UnleashContext(properties = mapOf("somekey" to "somevalue", "someotherkey" to "someothervalue"), remoteAddress = "127.0.0.1", appName = "my-app")
        val uuid = UUID.randomUUID().toString()
        val updatedCtx = ctx.newBuilder().instanceId(uuid).build()
        assertThat(updatedCtx.instanceId).isEqualTo(uuid)
    }

    @Test
    fun `Can override properties with builder method`() {
        val ctx = UnleashContext(properties = mapOf("somekey" to "somevalue", "someotherkey" to "someothervalue"), remoteAddress = "127.0.0.1", appName = "my-app")
        val updatedCtx = ctx.newBuilder().properties(mutableMapOf("third" to "3")).build()
        assertThat(updatedCtx.properties).hasSize(1)
    }

}