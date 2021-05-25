package io.getunleash.data


/**
 * Json response for a variant see [Variants](https://docs.getunleash.io/docs/advanced/toggle_variants)
 * @property name
 * @property payload
 */
data class Variant(val name: String, val enabled: Boolean = true, val payload: Payload? = null)

val disabledVariant = Variant("disabled")
