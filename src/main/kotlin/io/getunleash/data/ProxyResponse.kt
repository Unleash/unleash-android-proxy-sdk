package io.getunleash.data

import kotlinx.serialization.Serializable

@Serializable
/**
 * Holder for parsing response from proxy
 */
data class ProxyResponse(val toggles: List<Toggle>)
