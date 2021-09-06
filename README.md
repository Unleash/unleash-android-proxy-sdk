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
* pollMode - See [PollingModes](#PollingModes)

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
    .pollMode(PollingModes.autoPoll(60000) { // poll interval in milliseconds
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

### PollingModes
#### Autopolling
If you'd like for changes in toggles to take effect for you; use AutoPolling.
You can configure the pollInterval and a listener that gets notified when toggles are updated in the background thread

The listener is a no-argument lambda that gets called by the RefreshPolicy for every poll that
1. Does not return `304 - Not Modified`
2. Does not return a list of toggles that's exactly the same as the one we've already stored in local cache. Just in case the ETag/If-None-Match fails.

Example usage is equal to the `Example setup` above
```kotlin
val context = UnleashContext.newBuilder()
    .appName("Your AppName")
    .userId("However you resolve your userid")
    .sessionId("However you resolve your session id")
    .build()
val config = UnleashConfig.newBuilder()
    .proxyUrl("URL to your proxy installation")
    .clientSecret("yourProxyApiKey")
    .pollMode(PollingModes.autoPoll(60000) { poll interval in milliseconds
        featuresUpdated()
    })
    .build()
val client = UnleashClient(config = config, unleashContext = context)
```

#### FilePolling (since v0.2)
The name `FilePolling` can be a tad misleading, since this policy doesn't actually poll, it simply loads a file of toggles from disk on startup, and uses that to answer all client calls.
Useful when your app might have limited internet connectivity, you'd like to run tests with a known toggle state or you simply do not want background activity.

The following example shows how to use it, provided the file to use is located at /tmp/proxyresponse.json
```kotlin
val toggles = File("/tmp/proxyresponse.json")
val pollingMode = PollingModes.fileMode(toggles)
val context = UnleashContext.newBuilder()
    .appName("Your AppName")
    .userId("However you resolve your userid")
    .sessionId("However you resolve your session id")
    .build()
val config = UnleashConfig.newBuilder()
    .proxyUrl("URL to your proxy installation") // These two don't matter for FilePolling, 
    .clientSecret("yourProxyApiKey") // since the client never speaks to the proxy
    .pollMode(pollingMode)
    .build()
val client = UnleashClient(config = config, unleashContext = context)

```

### Metrics (since v0.2)
If you'd like the client to post metrics to the proxy so the admin interface can be updated, add a call to `enableMetrics()`.

```kotlin
val config = UnleashConfig
    .newBuilder()
    .appName()
    .userId()
    .sessionId()
    .enableMetrics()
    .build()
```

The default configuration configures a daemon to report metrics once every minute, this can be altered using the `metricsInterval(Duration d)` method on the builder, so if you'd rather see us post in 5 minutes intervals you could do
```kotlin
UnleashConfig().newBuilder().metricsInterval(300000) // Every 5 minutes
```

