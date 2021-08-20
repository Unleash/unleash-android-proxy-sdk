package io.getunleash

import io.getunleash.data.Variant
import io.getunleash.polling.TogglesErroredListener
import io.getunleash.polling.TogglesUpdatedListener
import java.io.Closeable
import java9.util.concurrent.CompletableFuture

interface UnleashClientSpec : Closeable {
    fun isEnabled(toggleName: String): Boolean
    fun getVariant(toggleName: String): Variant
    fun updateContext(context: UnleashContext): CompletableFuture<Void>
    fun getContext(): UnleashContext
    fun addTogglesUpdatedListener(listener: TogglesUpdatedListener)
    fun addTogglesErroredListener(listener: TogglesErroredListener)
}