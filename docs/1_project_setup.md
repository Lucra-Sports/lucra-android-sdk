# Getting Started

## Compatibility Overview

This SDK (v5.0.0+) requires the following minimum versions in your app environment:

| Component | Minimum Version | Notes |
|------------|----------------|-------|
| **Android Gradle Plugin (AGP)** | 8.10.1 | Requires Gradle 8.12+ |
| **Gradle** | 8.12 | Use Gradle wrapper 8.12+ |
| **Kotlin** | 2.0.21 | Includes new K2 compiler |
| **Compose Compiler Plugin** | 2.0.21 | Managed via `org.jetbrains.kotlin.plugin.compose` |
| **Compose BOM** | 2025.09.01+ | Must match Kotlin version |
| **compileSdk** | 35 | Android 15 |
| **targetSdk** | 35 | Recommended for full compatibility |
| **AndroidX Navigation** | 2.9.3+ | Optional but recommended |

### Migration Notes

- The SDK now uses Kotlin’s **K2 compiler**, requiring Kotlin 2.x.
- The **Compose compiler plugin** is now managed separately:
  ```kotlin
  plugins {
      id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
  }

### Gradle setup

In your project's `build.gradle` add the following and replace the credentials with your provided
PAT and your username

```gradle
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven(url = "https://zendesk.jfrog.io/zendesk/repo")
        maven {
            name = "LucraGithubPackages"
            url = uri("https://maven.pkg.github.com/Lucra-Sports/lucra-android-sdk")
            credentials {
                username = {YOUR_GITHUB_USERNAME}
                password = {YOUR_GITHUB_LUCRA_PAT}
            }
        }
    }
```

In `app/build.gradle`

```gradle 
// All surface level APIs to interact with Lucra SDK
implementation("com.lucrasports.sdk:sdk-core:{LATEST-VERSION}")
// Optional for UI functionality - transitively includes `sdk-core`
implementation("com.lucrasports.sdk:sdk-ui:{LATEST-VERSION}")

// Required desugaring library, which allows the project to be built with embedded jdks, it's not 
// what is ran on the device (java 11). See more here https://github.com/android/nowinandroid/pull/731

defaultConfig {
  targetSdk = 35 //Target SDK must be 35 or lower
}

compileOptions {
  isCoreLibraryDesugaringEnabled = true
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
```

#### Auth0 compliance (if not already using Auth0)

Lucra leverages Auth0 for user auth, if your app doesn't use it already, add the following to your app's default
config.

Gradle.kts

```gradle.kts
android{
    defaultConfig {
        // Dumby placeholders to make Auth0 happy
        addManifestPlaceholders(mapOf("auth0Domain" to "LUCRA_SDK", "auth0Scheme" to "LUCRA_SDK"))
    }
}
```

Groovy

```groovy
manifestPlaceholders = [
        // Dumby placeholders to make Auth0 happy
        'auth0Domain': 'LUCRA_SDK',
        'auth0Scheme': 'LUCRA_SDK'
]
```

### Manifest Requirements

The following manifest permissions, features, receivers and services are required to use Lucra

```xml

<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <uses-permission android:name="android.permission.CAMERA" />
  <uses-permission android:name="android.permission.INTERNET" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
  <uses-permission android:name="android.webkit.PermissionRequest" />
  <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
  <uses-permission android:name="com.google.android.providers.gsf.permission.READ_GSERVICES" />
  <uses-permission android:name="android.permission.READ_PHONE_STATE" />
  <uses-permission android:name="android.permission.USE_BIOMETRIC" />
  <uses-feature android:name="android.hardware.camera.autofocus" />
  <uses-feature android:name="android.hardware.camera" />

  <application
  ...
  >

  <!--    Geocomply requirements-->
  <receiver android:name="com.geocomply.client.GeoComplyClientBootBroadcastReceiver"
          android:enabled="true" android:exported="true">
    <intent-filter>
      <action android:name="android.intent.action.BOOT_COMPLETED" />
      <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
  </receiver>

  <service android:name="com.geocomply.location.WarmingUpLocationProvidersService"
          android:exported="false" />
  <service android:name="com.geocomply.security.GCIsolatedSecurityService"
          android:exported="false" android:isolatedProcess="true" tools:targetApi="q" />

  <receiver android:name="com.geocomply.client.GeoComplyClientBroadcastReceiver" />
  </application>
</manifest>
```

### Proguard Requirements

```
#https://issuetracker.google.com/issues/247066506
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.io.**
-dontwarn android.content.res.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
-keep class org.xmlpull.** { *; }
-keepclassmembers class org.xmlpull.** { *; }
```

### Application Requirements

Lucra leverages [Coil](https://coil-kt.github.io/coil/) to render images and SVGs. In your
application class, provider the LucraCoilImageLoader

```kotlin
// Don't forget to set the app manifest to use this Application
class MyApplication : Application(), ImageLoaderFactory {
  // Use Lucra's ImageLoader to decode SVGs as needed
  override fun newImageLoader() = LucraCoilImageLoader.get(this)
}

```

### From here, `LucraClient` can now be initialized
[Initialization steps](1.2_initialize_client)