package io.getunleash

import io.getunleash.data.Variant
import okhttp3.OkHttpClient

class UnleashClient(val unleashConfig: UnleashConfig,
                    val unleashContext: UnleashContext,
                    val httpClient: OkHttpClient) : UnleashClientSpec {



    override fun isEnabled(toggleName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getVariant(toggleName: String): Variant {
        TODO("Not yet implemented")
    }

    override fun updateContext(context: UnleashContext) {
        TODO("Not yet implemented")
    }

    override fun getContext(): UnleashContext {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    data class Builder(val httpClient: OkHttpClient? = null, )
}