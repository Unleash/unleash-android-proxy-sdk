package io.getunleash.cache

import io.getunleash.data.Toggle

interface ToggleCache {
    fun read(key: String): Map<String, Toggle>
    fun write(key: String, value: Map<String, Toggle>)
}