package io.getunleash

data class UnleashConfig(val proxyUrl: String, val clientSecret: String, val appName: String? = null, val environment: String? = null) {
    companion object {
        fun newBuilder(): Builder = Builder()
    }

    data class Builder(var proxyUrl: String? = null, var clientSecret: String? = null, var appName: String? = null, var environment: String? = null) {
        fun proxyUrl(proxyUrl: String) = apply { this.proxyUrl = proxyUrl }
        fun clientSecret(secret: String) = apply { this.clientSecret = secret }
        fun appName(appName: String) = apply { this.appName = appName}
        fun environment(environment: String) = apply { this.environment = environment }
        fun build(): UnleashConfig = UnleashConfig(
            proxyUrl = proxyUrl!!,
            clientSecret = clientSecret!!,
            appName = appName,
            environment = environment
        )
    }
}