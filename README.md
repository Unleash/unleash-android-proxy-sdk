# Unleash Android SDK
[![Coverage Status](https://coveralls.io/repos/github/Unleash/unleash-android-proxy-sdk/badge.svg?branch=main)](https://coveralls.io/github/Unleash/unleash-android-proxy-sdk?branch=main)
[![main](https://github.com/Unleash/unleash-android-proxy-sdk/actions/workflows/test.yml/badge.svg)](https://github.com/Unleash/unleash-android-proxy-sdk/actions/workflows/test.yml)
[![latest](https://badgen.net/maven/v/maven-central/io.getunleash/unleash-android-proxy-sdk)](https://search.maven.org/search?q=g:io.getunleash%20AND%20a:unleash-android-proxy-sdk)

## Getting started

You will require the SDK on your classpath, so go ahead and add it to your gradle file

```kotlin
implementation("io.getunleash:unleash-android-proxy-sdk:${unleash.sdk.version}")
```

### Now configure your client instance

You should use this as a singleton to avoid file contention on cache directory.

#### Unleash Context
The important properties to configure on the context are
* Appname - In case you use strategies that depend on which app
* UserId - GradualRolloutStrategies often use this to decide stickiness when assigning which group of users the user end up in
* SessionId - GradualRolloutStrategies often use this to decide stickiness

#### Unleash Config
For the config you must set two variables, and if you'd like to be notified when the polling thread has found updates you should also configure pollMode
* proxyUrl - Where your proxy installation is located, for Unleash-Hosted's demo instance this is at `https://app.unleash-hosted.com/demo/proxy` but yours will be somewhere else
* clientSecret - The api key for accessing your proxy.
* pollMode - For now we only support autoPolling, but when constructing the pollMode you can set the duration of the polling interval and pass in a lambda describing what to do when the poller notifies you that toggles have been updated.


Example setup:

```kotlin
val context = UnleashContext.newBuilder()
    .appName("Your AppName")
    .userId("However you resolve your userid")
    .sessionId("However you resolve your session id") 
    .build()
val config = UnleashConfig.newBuilder()
    .proxyUrl("URL to your proxy installation")
    .clientSecret("yourProxyApiKey")
    .pollMode(PollingModes.autoPoll(Duration.ofSeconds(60)) {
        featuresUpdated()
    })
    .build()
val client = UnleashClient(config = config, unleashContext = context)
```
In [the sample app](./samples/android/app/src/main/java/com/example/unleash/MainActivity.kt)
we use this to update the text on the first view

```kotlin
 this@MainActivity.runOnUiThread {
    val firstFragmentText = findViewById<TextView>(R.id.textview_first)
    firstFragmentText.text = "Variant ${unleashClient.getVariant("unleash_android_sdk_demo").name}"
 }
```