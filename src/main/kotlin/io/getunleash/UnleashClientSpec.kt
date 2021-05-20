package io.getunleash

import io.getunleash.data.Variant
import java.io.Closeable
import java.util.concurrent.CompletableFuture

interface UnleashClientSpec : Closeable {
    fun isEnabled(toggleName: String): Boolean
    fun getVariant(toggleName: String): Variant
    fun updateContext(context: UnleashContext): CompletableFuture<Void>
    fun getContext(): UnleashContext
}