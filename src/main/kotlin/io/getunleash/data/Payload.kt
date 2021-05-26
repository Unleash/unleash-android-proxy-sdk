package io.getunleash.data

import com.fasterxml.jackson.databind.JsonNode

/**
 * Used as a child of [Variant] for further specialization when evaluating toggle.
 * Use the type parameter to extract data from the JsonNode. Helper methods are provided for Json Primitive and Array values
 *
 * @property type - Type of the payload. This can be any type 'string' | 'number' | 'json' ...
 * @property value - The actual payload represented as a [com.fasterxml.jackson.databind.JsonNode]
 */
data class Payload(val type: String, val value: JsonNode) {
    /**
     * Helper to extract value as String from JsonNode
     */
    fun getValueAsString(): String? {
        return value.textValue()
    }
    fun getValueAsInt() = value.asInt()
    fun getValueAsDouble() = value.asDouble()
    fun getValueAsBoolean() = value.asBoolean()
}

