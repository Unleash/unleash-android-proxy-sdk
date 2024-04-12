package io.getunleash

/**
 *
 * To standardise a few activation strategies, we also needed to standardise the Unleash context,
 * which contains fields that vary per request, required to implement the activation strategies.
 * [See the documentation](https://docs.getunleash.io/docs/user_guide/unleash_context)
 * @property userId Whatever implementation of userId you use,
 * if you use the userWithId strategy you'll need to set this
 * @property sessionId However you roll out your session id, if you'd like to use the flexibleRollout strategy with stickiness
 * bound to sessionId you'll need to set this
 * @property instanceId Used as a unique identifier for the current instantiation of the client - defaults to the one set in the [io.getunleash.UnleashConfig]
 * @property remoteAddress the Ip address of the client. If your feature uses the remoteAddress strategy
 * you'll need to set this
 * @property properties - Other properties for custom strategies.
 * @property appName - The name of your app, used for evaluating strategies - defaults to the one set in the [io.getunleash.UnleashConfig]
 * @property environment - Which environment are you running in - defaults to the one set in the [io.getunleash.UnleashConfig]
 */
data class UnleashContext(
    val userId: String? = null,
    val sessionId: String? = null,
    val remoteAddress: String? = null,
    val instanceId: String? = null,
    val properties: Map<String, String> = emptyMap(),
    val appName: String? = null,
    val environment: String? = null,
) {
    /**
     * Used to get a new builder with current state
     */
    fun newBuilder(): Builder = Builder(
        userId = userId,
        sessionId = sessionId,
        remoteAddress = remoteAddress,
        properties = properties.toMutableMap(),
        appName = appName,
        environment = environment
    )

    companion object {
        /**
         * Used to get a Builder with no fields set, hopefully simplifying constructing contexts
         */
        fun newBuilder(): Builder = Builder()
    }

    data class Builder(
        var userId: String? = null,
        var sessionId: String? = null,
        var remoteAddress: String? = null,
        var properties: MutableMap<String, String> = mutableMapOf(),
        var appName: String? = null,
        var environment: String? = null,
        var instanceId: String? = null,
    ) {

        fun userId(userId: String) = apply { this.userId = userId }

        fun sessionId(sessionId: String) = apply { this.sessionId = sessionId }

        fun remoteAddress(address: String) = apply { this.remoteAddress = address }

        fun addProperty(key: String, value: String) = apply { this.properties[key] = value }

        fun properties(map: MutableMap<String, String>) = apply { this.properties = map }

        fun appName(appName: String) = apply { this.appName = appName }

        fun environment(environment: String) = apply { this.environment = environment }

        fun instanceId(id: String) = apply { this.instanceId = id }

        fun build(): UnleashContext {
            return UnleashContext(
                userId = userId,
                sessionId = sessionId,
                remoteAddress = remoteAddress,
                properties = properties.toMap(),
                appName = appName,
                environment = environment,
                instanceId = instanceId,
            )
        }
    }
}