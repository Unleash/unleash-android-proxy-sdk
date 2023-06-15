package com.example.unleash

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import io.getunleash.UnleashClient
import io.getunleash.UnleashConfig
import io.getunleash.UnleashContext
import io.getunleash.cache.InMemoryToggleCache
import io.getunleash.polling.PollingModes
import javax.inject.Singleton
import kotlin.random.Random


@Module
@InstallIn(SingletonComponent::class)
internal object UnleashClientModule {
    val unleashContext = UnleashContext.newBuilder()
        .appName("unleash-android")
        .instanceId("main-activity-unleash-demo-${Random.nextLong()}")
        .userId("unleash_demo_user")
        .sessionId(Random.nextLong().toString())
        .build()


    @Provides
    @Singleton
    fun unleashClient(): UnleashClient {
        return UnleashClient.newBuilder()
            .unleashConfig(
                UnleashConfig.newBuilder()
                    .appName("unleash-android")
                    .instanceId("unleash-android-${Random.nextLong()}")
                    .enableMetrics()
                    .clientSecret("*:development.8e2b03838a586e7de5009e08b56e839cf538f6f9968dc03eddcb44d7")
                    .proxyUrl("http:///192.168.50.135:3063/api/frontend")
                    .pollingMode(
                        PollingModes.autoPoll(
                            autoPollIntervalSeconds = 15
                        ) {

                        }
                    )
                    .build()
            )
            .cache(InMemoryToggleCache())
            .unleashContext(unleashContext)
            .build()
    }

}

@HiltAndroidApp
class UnleashApplication : Application() {


}