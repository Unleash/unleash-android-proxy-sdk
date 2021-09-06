package io.getunleash.data

import com.fasterxml.jackson.module.kotlin.readValue
import io.getunleash.polling.TestResponses
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

class ParserTest {
    data class TestObject(val field: Date = Date.from(LocalDateTime.of(2021, 1, 1, 12, 0, 0).toInstant(ZoneOffset.UTC)))
    @Test
    fun jacksonSerializesDatesInISO8601Format() {
        val data = Parser.jackson.writeValueAsString(TestObject())
        assertThat(data).contains("2021-01-01T12:00:00")
    }
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