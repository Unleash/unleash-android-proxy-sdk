package io.getunleash

import io.getunleash.data.Parser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
}