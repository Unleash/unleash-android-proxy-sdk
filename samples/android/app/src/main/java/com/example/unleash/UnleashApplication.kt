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
                    .environment("dev")
                    .clientSecret("proxy-123")
                    .proxyUrl("http://192.168.1.42:3200/proxy")
                    .enableMetrics()
                    .pollingMode(
                        PollingModes.autoPoll(
                            autoPollIntervalSeconds = 15
                        ) {

                        }
                    )
                    .environment("dev").build()
            )
            .cache(InMemoryToggleCache())
            .unleashContext(unleashContext)
            .build()
    }

}

@HiltAndroidApp
class UnleashApplication : Application() {


}