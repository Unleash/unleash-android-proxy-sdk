package io.getunleash.data

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object Parser {
    val jackson = jacksonObjectMapper().registerModule(JavaTimeModule())
}