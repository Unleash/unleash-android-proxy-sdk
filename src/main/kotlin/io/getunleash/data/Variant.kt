package io.getunleash.data

import kotlinx.serialization.Serializable

/**
 * Json response for a variant see [Variants](https://docs.getunleash.io/docs/advanced/toggle_variants)
 * @property name
 * @property payload
 */
@Serializable
data class Variant(val name: String, val payload: Payload? = null)
