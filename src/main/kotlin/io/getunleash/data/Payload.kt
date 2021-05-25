package io.getunleash.data

import com.fasterxml.jackson.databind.JsonNode

/**
 * Used as a child of [Variant] for further specialization when evaluating toggle
 * @property type - Type of the payload. This can be any type 'string' | 'number' | 'json' ...
 * @property value - The actual payload represented as a String
 */
data class Payload(val type: String, val value: JsonNode)