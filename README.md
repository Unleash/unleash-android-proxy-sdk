# Unleash Android SDK
[![Coverage Status](https://coveralls.io/repos/github/Unleash/unleash-android-sdk/badge.svg?branch=main)](https://coveralls.io/github/Unleash/unleash-android-sdk?branch=main)
[![main](https://github.com/Unleash/unleash-android-sdk/actions/workflows/test.yml/badge.svg)](https://github.com/Unleash/unleash-android-sdk/actions/workflows/test.yml)
[![latest](https://badgen.net/maven/v/maven-central/io.getunleash/unleash-android-sdk)](https://search.maven.org/search?q=g:io.getunleash%20AND%20a:unleash-android-sdk)

## Getting started

You will require the SDK on your classpath, so go ahead and add it to your gradle file

```kotlin
implementation("io.getunleash:unleash-android:sdk:${unleash.sdk.version}")
```

### Now configure your client instance

You should use this as a singleton to avoid file contention on cache directory

```kotlin
val context = UnleashContext.newBuilder()
    .appName("Your AppName")
    .userId("However you resolve your userid").sessionId("However you resolve your session id")
val config = UnleashConfig.newBuilder().proxyUrl("URL to your proxy installation").clientSecret("yourProxyApiKey").unleashContext()
```