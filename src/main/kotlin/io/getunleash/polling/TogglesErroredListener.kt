package io.getunleash.polling

fun interface TogglesErroredListener {
    fun onError(e: Exception): Unit
}