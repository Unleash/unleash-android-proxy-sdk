package io.getunleash

import io.getunleash.polling.AutoPollingMode
import io.getunleash.polling.PollingMode

data class UnleashConfig(
    val proxyUrl: String,
    val clientSecret: String,
    val appName: String? = null,
    val environment: String? = null,
    val pollingMode: PollingMode = AutoPollingMode(60)
) {
    fun newBuilder(): Builder =
        Builder(proxyUrl = proxyUrl, clientSecret = clientSecret, appName = appName, environment = environment, pollingMode = pollingMode)

    companion object {
        fun newBuilder(): Builder = Builder()
    }

    data class Builder(
        var proxyUrl: String? = null,
        var clientSecret: String? = null,
        var appName: String? = null,
        var environment: String? = null,
        var pollingMode: PollingMode? = null
    ) {
        fun proxyUrl(proxyUrl: String) = apply { this.proxyUrl = proxyUrl }
        fun clientSecret(secret: String) = apply { this.clientSecret = secret }
        fun appName(appName: String) = apply { this.appName = appName }
        fun environment(environment: String) = apply { this.environment = environment }
        fun pollingMode(pollingMode: PollingMode) = apply { this.pollingMode = pollingMode }
        fun build(): UnleashConfig = UnleashConfig(
            proxyUrl = proxyUrl ?: throw IllegalStateException("You have to set proxy url in your UnleashConfig"),
            clientSecret = clientSecret ?: throw IllegalStateException("You have to set client secret in your UnleashConfig"),
            appName = appName,
            environment = environment,
            pollingMode = pollingMode ?: AutoPollingMode(60)
        )

    }
}