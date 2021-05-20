package io.getunleash

import io.getunleash.data.Toggle

abstract class ToggleCache {
    abstract fun read(key: String): Map<String, Toggle>
    abstract fun write(key: String, value: Map<String, Toggle>)
}