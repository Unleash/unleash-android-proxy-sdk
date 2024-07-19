package com.example.unleash

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.getunleash.android.DefaultUnleash
import io.getunleash.android.Unleash
import io.getunleash.android.UnleashConfig
import io.getunleash.android.data.UnleashContext
import javax.inject.Singleton
import kotlin.random.Random


@Module
@InstallIn(SingletonComponent::class)
internal object UnleashClientModule {
    val unleashContext = UnleashContext.newBuilder()
        .userId("unleash_demo_user")
        .sessionId(Random.nextLong().toString())
        .build()


    @Provides
    @Singleton
    fun unleashClient(
        @ApplicationContext context: Context
    ): Unleash {
        val unleash = DefaultUnleash(
            androidContext = context,
            unleashConfig = UnleashConfig.newBuilder("unleash-android")
                .clientKey("default:development.5d6b7aaeb6a9165f28e91290d13ba0ed39f56f6d9e6952c642fed7cc")
                .proxyUrl("https://eu.app.unleash-hosted.com/demo/api/frontend")
                .pollingStrategy.interval(15000)
                .metricsStrategy.interval(5000)
                .build(),
            unleashContext = unleashContext
        )
        unleash.start()
        return unleash
    }

}

@HiltAndroidApp
class UnleashApplication : Application() {


}