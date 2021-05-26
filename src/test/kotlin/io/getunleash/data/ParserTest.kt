package io.getunleash.data

import com.fasterxml.jackson.module.kotlin.readValue
import io.getunleash.polling.TestResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParserTest {

    @Test
    fun `Able to parse payload value as string`() {
        val response: ProxyResponse = Parser.jackson.readValue(TestResponses.threeToggles)
        val map = response.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
        val toggle = map["variantToggle"]!!
        assertThat(toggle.variant.payload!!.getValueAsString()).isEqualTo("some-text")
    }

    @Test
    fun `Able to parse payload value as integer`() {
        val response: ProxyResponse = Parser.jackson.readValue(TestResponses.complicatedVariants)
        val map = response.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
        val toggle = map["variantToggle"]!!
        assertThat(toggle.variant.payload!!.getValueAsInt()).isEqualTo(54)
    }

    @Test
    fun `Able to parse payload value as boolean`() {
        val response: ProxyResponse = Parser.jackson.readValue(TestResponses.complicatedVariants)
        val map = response.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
        val toggle = map["booleanVariant"]!!
        assertThat(toggle.variant.payload!!.getValueAsBoolean()).isTrue
    }

    @Test
    fun `Able to parse payload value as double`() {
        val response: ProxyResponse = Parser.jackson.readValue(TestResponses.complicatedVariants)
        val map = response.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
        val toggle = map["doubleVariant"]!!
        assertThat(toggle.variant.payload!!.getValueAsDouble()).isEqualTo(42.0)
    }

    @Test
    fun `Able to parse payload value as json node`() {
        val response: ProxyResponse = Parser.jackson.readValue(TestResponses.complicatedVariants)
        val map = response.toggles.groupBy { it.name }.mapValues { (_, v) -> v.first() }
        val toggle = map["simpleToggle"]!!
        val payload = toggle.variant.payload!!
        assertThat(payload.value.isObject).isTrue
        assertThat(payload.value.has("key")).isTrue

    }
}