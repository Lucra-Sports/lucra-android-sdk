# Lucra SDK

[Release notes](SDK_RELEASE_NOTES.md)

## Getting Started

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
// All surface level APIs to interact with Lucra
implementation("com.lucrasports.sdk:sdk-core:2.0.0-beta") //TODO reference latest github release #
// Optional for UI functionality
implementation("com.lucrasports.sdk:sdk-ui:2.0.0-beta") //TODO reference latest github release #
```

#### Auth0 compliance (if not already using Auth0)

We use Auth0 for auth, if your app doesn't use it already, add the following to your app's default
config.

Gradle.kts

```gradle.kts
android{
    defaultConfig {
        addManifestPlaceholders(mapOf("auth0Domain" to "LUCRA_SDK", "auth0Scheme" to "LUCRA_SDK"))
    }
}
```

Groovy

```groovy
manifestPlaceholders = [
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
</application></manifest>
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

### Initialization

In your application class, initialize the Lucra instance in `onCreate`.

```kotlin
LucraClient.initialize(
  // Required - provide Auth0 client ID to use for authorization
  apiKey = "your api key",
  //Required - provide URL that will work along side apiKey
  apiUrl = "your api url",
  // Optionally provide LucraUiProvider implementation from "com.lucrasports.sdk:sdk-ui:*"
  lucraUiProvider = LucraUi(
    lucraFlowListener = object : LucraFlowListener {

      // Callback for entering Lucra permitted flow launch points.
      override fun launchNewLucraFlowEntryPoint(entryLucraFlow: LucraUiProvider.LucraFlow): Boolean {
        Log.d("Sample", "launchNewLucraFlowEntryPoint: $entryLucraFlow")
        showLucraDialogFragment(entryLucraFlow)
        return true
      }

      //Callback for exiting all Lucra permitted flow launch points
      override fun onFlowDismissRequested(entryLucraFlow: LucraUiProvider.LucraFlow) {
        Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow")
        supportFragmentManager.findFragmentByTag(entryLucraFlow.toString())?.let {
          Log.d("Sample", "Found $entryLucraFlow as $it")

          if (it is DialogFragment)
            it.dismiss()
          else
            supportFragmentManager.beginTransaction().remove(it).commit()
        } ?: run {
          Log.d("Sample", "onFlowDismissRequested: $entryLucraFlow not found")
        }
      }
    }
  ),
  // Optionally provide Lucra.Logger implementation to track events happening through the experience
  customLogger = null,
  // Optionally provide environment to use, defaults to Environment.PRODUCTION
  environment = Environment.SANDBOX,
  // Optionally specify to output logs to Logcat, defaults to false
  outputLogs = true,
  // Optionally add your own color scheme and fonts, from "com.lucrasports.sdk:sdk-ui:*", defaults to the Lucra Defaults
  clientTheme = ClientTheme(
    colorStyle = ColorStyle(),
    fontFamily = FontFamily()
  )
)
```

### Interacting with APIs

Use Lucra instance easily by invoking the class operator `LucraClient().*` or fetching the
instance `LucraClient.getInstance().*`

Once you have the instance, you can interact with our headless SDK methods:

### User API

Get and update the current user's properties

**Methods**

`observeSDKUserFlow`, `observeSDKUser`

Observe the active `SDKUser` based on authed status.

- **Example usage:**

```kotlin
private fun observeLoggedInUser() {
  // Or use observeSDKUser { result -> ... }
  LucraClient().observeSDKUserFlow().onEach { sdkUserResult ->
    when (sdkUserResult) {
      is SDKUserResult.Success -> {
        Log.d("Lucra SDK Sample", "Fetched latest user")
        lucraSDKUser = sdkUserResult.sdkUser
      }

      is SDKUserResult.Error -> {
        Log.e("Lucra SDK Sample", "Unable to get username ${sdkUserResult.error}")
        lucraSDKUser = null
      }

      SDKUserResult.InvalidUsername -> {
        // Shouldn't happen here
      }

      SDKUserResult.NotLoggedIn -> {
        Log.e(
          "Lucra SDK Sample",
          "User not logged in yet!"
        )
        lucraSDKUser = null
      }

      SDKUserResult.Loading -> {

      }
    }
  }.launchIn(lifecycleScope)
}
```

`configure`

Updates the `SDKUser` for the current logged in user
> [!IMPORTANT]
> At the moment, users can only log in after attempting to interact with a `LucraFlow`


- **Parameters:**
  - `onResult`: `SDKUserResult` callback

- **Example usage:**
```kotlin
// sdkUser from `getSDKUser`
LucraClient().configure(sdkUser.copy(username = newUsername)) {
        when (it) {
            is SDKUserResult.Error -> {
                Log.e(
                    "Lucra SDK Sample",
                    "Unable to update username ${it.error}"
                )
            }

            SDKUserResult.InvalidUsername -> {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Invalid username, try a different one",
                    Toast.LENGTH_LONG
                )
                    .show()
            }

            SDKUserResult.NotLoggedIn -> {
                Log.e(
                    "Lucra SDK Sample",
                    "User not logged in yet!"
                )
            }

            is SDKUserResult.Success -> {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Username updated to ${it.sdkUser.username}",
                    Toast.LENGTH_LONG
                )
                    .show()
                dialog.dismiss()
            }
        }
    }
```

`logout`

Logs out the current user, if any


- **Example usage:**

```kotlin
 LucraClient().logout(context)
```

### GamesMatchup API

The GamesMatchup interface provides methods to manage game contests. It allows users to create,
accept, and cancel contests. Each method provides a callback mechanism to handle the result of the
operation.

**Methods**

`createGamesMatchup`

Creates a new game contest.

- **Parameters:**
  - `gameTypeId`: ID associated with the game type.
  - `atStake`: Amount of money being wagered.
  - `onResult`: Callback with a result of type `CreateGamesMatchupResult`.

- **Example usage:**

```kotlin
LucraClient().createContest(
    gameTypeId = "gameTypeId",
    atStake = 25.0
) { result ->
    when (result) {
        is CreateGamesMatchupResult.Failure -> {
            // Handle failure scenario
        }
        is CreateGamesMatchupResult.GYPCreatedMatchupOutput -> {
            // Handle success scenario
        }
    }
}
```

`acceptGamesMatchup`

Accepts a contest with the given ID.

- **Parameters:**
  - `matchupId`: ID of the contest.
  - `teamId`: ID of the team the user wants to join.
  - `onResult`: Callback with a result of type `MatchupActionResult`.

- **Example usage:**

```kotlin
LucraClient().acceptGamesYouPlayContest(
    matchupId = "matchupId",
    teamId = "teamId"
) { result ->
    when (result) {
        is MatchupActionResult.Failure -> {
            // Handle failure scenario
        }
        MatchupActionResult.Success -> {
            // Handle success scenario
        }
    }
}
```

`retrieveGamesMatchup`

Retrieve a contest with the given ID.

- **Parameters:**
  - `matchupId`: ID of the contest.
  - `onResult`: Callback with a result of type `RetrieveGamesMatchupResult`.

- **Example usage:**

```kotlin
LucraClient().getGamesMatchup(
    matchupId = "matchupId",
) { result ->
    when (result) {
        is RetrieveGamesMatchupResult.Failure -> {
            // Handle failure scenario
        }
        RetrieveGamesMatchupResult.GYPMatchupDetailsOutput -> {
            // Handle success scenario
        }
    }
}
```

`cancelGamesMatchup`

Cancels a contest with the given ID.

- **Parameters:**
  - `matchupId`: ID of the contest.
  - `onResult`: Callback with a result of type `MatchupActionResult`.

- **Example usage:**

```kotlin
LucraClient().cancelGamesYouPlayContest(matchupId = "matchupId") { result ->
    when (result) {
        is MatchupActionResult.Failure -> {
            // Handle failure scenario
        }
        MatchupActionResult.Success -> {
            // Handle success scenario
        }
    }
}
```

`setDeeplinkTransformer`

Sets lambda function that will be used to transform original lucra URI to client specific URI. Make sure to keep the original URI in the process to allow conversion into `LucraFlow` later.

- **Parameters:**
  - `suspend (String) -> String`: suspended string transformer lambda 

- **Example usage:**

```kotlin
LucraClient().setDeeplinkTransformer { originalUri ->
    transformUriToAppLink(originalUri)
}
```

`getLucraFlowForDeeplinkUri`

Convert lucra URI (ex: `lucra://...`) into `LucraFlow` that can be launched to show a UI component.

- **Parameters:**
  - `uri`: uri string to convert into a LucraFlow

- **Example usage:**

```kotlin
LucraClient().apply {
  getLucraFlowForDeeplinkUri("uriString")?.let { lucraFlow ->
    getLucraDialogFragment(lucraFlow).also { fragment ->
      fragment.show(supportFragmentManager, lucraFlow.toString())
    }
  }
}
```

**Result Types**

`MatchupActionResult`

Represents the result of an operation on an existing `GamesMatchup`.

- `Success`: Represents a successful operation.
- `Failure`: Represents a failed operation. Contains a `failure` of type `FailedCreateGamesMatchup`.

`CreateGamesMatchupResult`

Represents the result of creating a `GamesMatchup`.

- `GYPCreatedMatchupOutput`: Represents a successfully created matchup.
  Contains `matchupId`, `ownerTeamId`, and `opponentTeamId`.
- `Failure`: Represents a failed operation. Contains a `failure` of type `FailedCreateGamesMatchup`.

**Error Types**

`FailedCreateGamesMatchup`

Represents the types of errors that can occur when creating a `GamesMatchup`.

- `UserStateError`: Errors related to the current user account status or available funds.
  - `NotInitialized`: User account is not initialized.
  - `Unverified`: User account is unverified.
  - `NotAllowed`: User is not allowed to perform the operation.
  - `InsufficientFunds`: User has insufficient funds.
- `LocationError`: Errors related to the user's location. Contains a `message`.
- `APIError`: All other errors. Contains a `message`.

### Get User's KYC Status

Fetching the KYC status will determine if the current user is verified or not, allowing for money and sports contest interactions. If the user is not verified, launch the `VerifyIdentity` LucraFlow.

**Methods**

`checkUsersKYCStatus`

Returns the status of the current logged in user.

- **Parameters:**
  - `userId`: ID associated to the current user (see)
  - `lucraKYCStatusListener`: Listener to implement specific callbacks for KYC interaction

- **Example usage:**

```kotlin
    LucraClient().checkUsersKYCStatus(
        "user-id", //fetched from #getSDKUser
        object : LucraClient.LucraKYCStatusListener {
            override fun onKYCStatusCheckFailed(exception: Exception) {
                Toast.makeText(
                    this@MainActivitySdk,
                    "Verified Failed ${exception}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onKYCStatusAvailable(isVerified: Boolean) {
                Toast.makeText(this@MainActivitySdk, "Verified Success", Toast.LENGTH_LONG)
                    .show()
                if (!isVerified) 
                    launchFlow(LucraUiProvider.LucraFlow.VerifyIdentity)
            }
        })
```

### UI styling

**Requires `:sdk-ui`***

Your can customize your Lucra implementation with your own color scheme and fonts by providing
the `ClientTheme` object to `LucraClient`.

The `ClientTheme` class has two nested classes, `ColorStyle` and `FontFamily`.

`ColorStyle`

Represents the 10 different colors your can provide to the SDK. Each field in this class is an
hexadecimal string value.

The colors used
are `Primary`, `Secondary`, `Tertiary`, `Surface`, `Background`, `OnPrimary`, `OnSecondary`, `OnTertiary`, `OnSurface`, `OnBackground`

`FontFamily`

Represents a collection of the 4 different font styles that the SDK uses(`mediumFont`, `normalFont`, `semiBoldFont`, `boldFont`). 
The name of the font style field corresponds with the font weight associated with the imported font file.
Each text style is represented by a `Font` object.

`Font`

Each `Font` object requires you to specify the `fontAssetFilePath`.

The `fontAssetFilePath` is the exact file path of your custom font(ex: `/fonts/wingding.ttf`). The root directory is your assets
directory (`main/assets/`). All font files must be included in `assets`.
These fonts are imported through Reach Native.

```kotlin

LucraClient.initialize(
  /*...*/
  clientTheme = ClientTheme(
    colorStyle = ColorStyle(
      primary = "#1976D2",
      secondary = "#F57C00",
      tertiary = "#388E3C",
      surface = "#FFFFFF",
      background = "#F5F5F5",
      onPrimary = "#FFFFFF",
      onSecondary = "#FFFFFF",
      onTertiary = "#FFFFFF",
      onSurface = "#000000",
      onBackground = "#000000"
    ),
    fontFamily = FontFamily(
      mediumFont = Font("my_medium_font.ttf"),
      normalFont = Font("my_regular_font.ttf"),
      semiBoldFont = Font("my_semi_bold_font.ttf"),
      boldFont = Font("my_bold_font.ttf"),
    )
  )
)
```

### Showing full Lucra flow

**Requires `:sdk-ui`***

Launch the LucraFragment in your Activity or Fragment by passing in a LucraFlow. The following flows
are supported:

`LucraUiProvider.LucraFlow.VerifyIdentity`
Launch the verify identity flow for users

`LucraUiProvider.LucraFlow.AddFunds`
Launch the add funds flow for users, identity verification will launch if the user hasn't verified
their identity yet

`LucraUiProvider.LucraFlow.WithdrawFunds`
Launch the withdraw funds flow for users, identity verification will launch if the user hasn't
verified
their identity yet

`LucraUiProvider.LucraFlow.CreateGamesMatchup`
Launch the create games matchup flow, identity verification will launch if the user hasn't verified
their identity yet

`LucraUiProvider.LucraFlow.CreateSportsMatchup`
Launch the Create Sport Matchup flow to create a new sports matchup

`LucraUiProvider.LucraFlow.Profile`
Launch the profile view for users to add and withdrawal funds, identity verification will launch if
the user hasn't verified their identity yet

`LucraUiProvider.LucraFlow.PublicFeed`
Launch to view the public feed of sports matchups.

`LucraUiProvider.LucraFlow.Dynamic`
Launch specific destinations, proprietary to the host app.

> [!NOTE]
> Use `LucraClient().[add/remove/clear]publicFeedLeagueIdFilter(...)` to filter the public feed by league.



`LucraUiProvider.LucraFlow.MyMatchup`
Launch to view the current user's matchups

`LucraUiProvider.LucraFlow.Login`
Launch to view the login flow

```kotlin
val lucraFlow = LucraUiProvider.LucraFlow.Profile
supportFragmentManager.beginTransaction()
  .add(
    R.id.lucraFragment,
    LucraClient().getLucraFragment(LucraUiProvider.LucraFlow.Profile),
    lucraFlow.toString()
  )
  .commit()
```

```kotlin
// Or use the LucraDialogFragment to show the flow in a dialog
val lucraDialog = LucraClient().getLucraDialogFragment(lucraFlow)
lucraDialog.show(supportFragmentManager, lucraFlow.toString())
```

### Showing partial Lucra elements

**Requires `:sdk-ui`***

`LucraComponents` Components are provided via a Via and are designed to be embedded in experiences unique to the host. Each component requires a callback to launch Lucra Flows from a new fragment. From there, the Flow operates as normal.

`LucraUiProvider.LucraComponent.MiniPublicFeed`
Show the public feed within your native app.
(optional) Pass in Player ids to filter the feed to only show matchups associated with the provided players
```kotlin
val view = LucraClient().getLucraComponent(
    this,
    LucraUiProvider.LucraComponent.MiniPublicFeed(
      playerIds = listOf("playerId1", "playerId2", ...) // optional
    ) {
        launchFlow(it) // Launch the flow as you would normally
    })
viewGroup.addView(view)
```

`LucraUiProvider.LucraComponent.ProfilePill`
Show the Profile Pill anywhere within your app. 
```kotlin
val view = LucraClient().getLucraComponent(
  this,
  LucraUiProvider.LucraComponent.ProfilePill {
    launchFlow(it) // Launch the flow as you would normally
  })
viewGroup.addView(view)
```

`LucraUiProvider.LucraComponent.FloatingActionButton`
Show a Floating Action Button anywhere within your app to launch the sports you watch contest creation flow.
```kotlin
val view = LucraClient().getLucraComponent(
  this,
  LucraUiProvider.LucraComponent.FloatingActionButton {
    launchFlow(it) // Launch the flow as you would normally
  })
viewGroup.addView(view)
```


