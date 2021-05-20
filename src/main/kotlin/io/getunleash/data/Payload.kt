package io.getunleash.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Used as a child of [Variant] for further specialization when evaluating toggle
 * @property type - Type of the payload. This can be any type 'string' | 'number' | 'json' ...
 * @property value - The actual payload represented as a [JsonElement]
 */
@Serializable
data class Payload(val type: String, val value: JsonElement)
