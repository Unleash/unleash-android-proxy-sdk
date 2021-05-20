package io.getunleash

import io.getunleash.data.Variant
import java.io.Closeable

interface UnleashClientSpec : Closeable {
    fun isEnabled(toggleName: String): Boolean
    fun getVariant(toggleName: String): Variant
    fun updateContext(context: UnleashContext)
    fun getContext(): UnleashContext
}