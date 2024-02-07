# Unleash Android SDK
[![Coverage Status](https://coveralls.io/repos/github/Unleash/unleash-android-proxy-sdk/badge.svg?branch=main)](https://coveralls.io/github/Unleash/unleash-android-proxy-sdk?branch=main)
[![main](https://github.com/Unleash/unleash-android-proxy-sdk/actions/workflows/test.yml/badge.svg)](https://github.com/Unleash/unleash-android-proxy-sdk/actions/workflows/test.yml)
[![latest](https://badgen.net/maven/v/maven-central/io.getunleash/unleash-android-proxy-sdk)](https://search.maven.org/search?q=g:io.getunleash%20AND%20a:unleash-android-proxy-sdk)
[KDoc](https://unleash.github.io/unleash-android-proxy-sdk)

## Getting started

### Step 1

You will require the SDK on your classpath, so go ahead and add it to your dependency management file

#### Gradle
```kotlin
implementation("io.getunleash:unleash-android-proxy-sdk:${unleash.sdk.version}")
```
#### Maven

```xml
<dependency>
    <groupId>io.getunleash</groupId>
    <artifactId>unleash-android-proxy-sdk</artifactId>
    <version>Latest version here</version>
</dependency>
```
#### Minimum Android SDK
- We are currently aiming for a minimum SDK level of 21. Keeping in tune with OkHttp's requirement.

#### Proguard
For now, you'll need to have Proguard ignore our classes as well as fasterxml (Jackson)
```
-keep public class io.getunleash.** {*;}
-keep class com.fasterxml.** { *; }
```

### Step 2: Enable internet

Your app will need internet permission in order to reach the proxy. So in your manifest file add

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Step 3: Configure your client instance

Now configure your client instance

You should use a singleton pattern to avoid file contention on cache directory.

#### Step 3a: Unleash Context

The important properties to configure on the context are
* Appname - In case you use strategies that depend on which app
* UserId - GradualRolloutStrategies often use this to decide stickiness when assigning which group of users the user end up in
* SessionId - GradualRolloutStrategies often use this to decide stickiness

```kotlin
import io.getunleash.UnleashContext

val context = UnleashContext.newBuilder()
    .appName("Your AppName")
    .userId("However you resolve your userid")
    .sessionId("However you resolve your session id") 
    .build()
```

#### Step 3b: Unleash Config
To create a client, use the UnleashConfig.newBuilder method. When building a configuration, you'll need to provide it with:

- `proxyUrl`: the URL the Unleash front-end API is available at **OR** the URL your proxy is available at
- `clientKey`: the API token or proxy client key you wish to use (this method was known as clientSecret prior to version 0.4.0)
- `pollMode`: how you want to load the toggle status

As of v0.1 the SDK supports an automatic polling with an adjustable poll period or loading the state from disk. Most users will probably want to use the polling client, but it's nice to know that you can instantiate your client without actually needing Internet if you choose loading from File


##### Connection options

To connect this SDK to your Unleash instance's [front-end API](https://docs.getunleash.io/reference/front-end-api), use the URL to your Unleash instance's front-end API (`<unleash-url>/api/frontend`) as the `proxyUrl` argument. For the `clientKey` parameter, use a `FRONTEND` token generated from your Unleash instance. Refer to the [_how to create API tokens_](https://docs.getunleash.io/how-to/how-to-create-api-tokens) guide for the necessary steps.

To connect this SDK to the [Unleash proxy](https://docs.getunleash.io/reference/unleash-proxy), use the proxy's URL and a [proxy client key](https://docs.getunleash.io/reference/api-tokens-and-client-keys#proxy-client-keys). The [_configuration_ section of the Unleash proxy docs](https://docs.getunleash.io/reference/unleash-proxy#configuration) contains more info on how to configure client keys for your proxy.


##### Step 3b: Configure client polling proxy

Configuring a client with a 60 seconds poll interval:

```kotlin
val config = UnleashConfig.newBuilder()
    .proxyUrl("URL to your front-end API or proxy")
    .clientKey("your front-end API token or proxy client key")
    .pollingMode(PollingModes.autoPoll(60) { // poll interval in seconds
        featuresUpdated()
    })
    .build()
```

##### Step 3b: Configure client loading toggles from a file

If you need to have a known state for your UnleashClient, you can perform a query against the proxy using your HTTP client of choice and save the output as a json file. Then you can tell Unleash to use this file to setup toggle states.

```kotlin
import io.getunleash.UnleashConfig
import io.getunleash.polling.PollingModes

val toggles = File("/tmp/proxyresponse.json")
val pollingMode = PollingModes.fileMode(toggles)

val config = UnleashConfig.newBuilder()
    .proxyUrl("Doesn't matter since we don't use it when sent a file")
    .clientKey("Doesn't matter since we don't use it when sent a file")
    .pollMode(pollingMode)
    .build()
```

### Step 4: Instantiate the client
Having created your UnleashContext and your UnleashConfig you can now instantiate your client. Make sure you only do this once, and pass the instantiated client to classes/functions that need it.

```kotlin
import io.getunleash.UnleashClient

val unleashClient = UnleashClient(unleashConfig = config, unleashContext = context)
```

### Details
#### PollingModes
##### Autopolling
If you'd like for changes in toggles to take effect for you; use AutoPolling.
You can configure the pollInterval and a listener that gets notified when toggles are updated in the background thread. 
If you set the poll interval to 0, the SDK will fetch once, but not set up polling.

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
    .proxyUrl("URL to your front-end API or proxy")
    .clientKey("your front-end API token or proxy client key")
    .pollingMode(PollingModes.autoPoll(60) { // poll interval in seconds
        featuresUpdated()
    })
    .build()
val client = UnleashClient(unleashConfig = config, unleashContext = context)
```

##### FilePolling (since v0.2)
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
    .proxyUrl("URL to your front-end API or proxy") // These two don't matter for FilePolling,
    .clientKey("front-end API token / proxy client key") // since the client never speaks to the proxy
    .pollingMode(pollingMode)
    .build()
val client = UnleashClient(unleashConfig = config, unleashContext = context)

```

### Metrics (since v0.2)
If you'd like the client to post metrics to the proxy so the admin interface can be updated, add a call to `enableMetrics()`.

#### NB Only supported by SDK version >=26

```kotlin
val config = UnleashConfig
    .newBuilder()
    .appName()
    .userId()
    .sessionId()
    .enableMetrics()
    .build()
```

The default configuration configures a daemon to report metrics once every minute, this can be altered using the `metricsInterval(long milliseconds)` method on the builder, so if you'd rather see us post in 5 minutes intervals you could do
```kotlin
UnleashConfig().newBuilder().metricsInterval(300000) // Every 5 minutes
```

#### Example main activity
In [the sample app](./samples/android/app/src/main/java/com/example/unleash/MainActivity.kt)
we use this to update the text on the first view

```kotlin
 this@MainActivity.runOnUiThread {
    val firstFragmentText = findViewById<TextView>(R.id.textview_first)
    firstFragmentText.text = "Variant ${unleashClient.getVariant("unleash_android_sdk_demo").name}"
 }
```

## Releasing

### Create a github tag prefixed with v
- So, if you want to release 0.6.0, make a tag v0.6.0
