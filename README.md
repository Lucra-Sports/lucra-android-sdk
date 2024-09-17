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

// Required desugaring library, which allows the project to be built with embedded jdks, it's not 
// what is ran on the device (java 11). See more here https://github.com/android/nowinandroid/pull/731
```
compileOptions {
        isCoreLibraryDesugaringEnabled = true
}

dependencies {
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
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
    lightColorStyle = ColorStyle(),
    darkColorStyle = ColorStyle(),
    fontFamily = FontFamily()
  )
)
```

### Setting up Push Notifications

Lucra's push notification works with your existing Firebase push notification solution. 

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        LucraPushNotificationService.refreshFirebaseToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (!LucraPushNotificationService.handlePushNotification(
                context = this,
                remoteMessage = message,
                activityClass = MainActivitySdk::class.java,
                smallIcon = R.drawable.lucra_letter_landing
            )
        ) {
            showNotification(message)
        }
    }

    private fun showNotification(message: RemoteMessage) {
        //Show your app specific notifications
    }
}
```
`refreshFirebaseToken`

Pass in the unique device token that's generated by Firebase. 

The Firebase token can also be retrieved from your Activity via...

```kotlin
FirebaseMessaging.getInstance().token
    .addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            return@addOnCompleteListener
        }

        val token = task.result
        LucraPushNotificationService.refreshFirebaseToken(token)
    }
```

`handlePushNotification`


This function will return `true` if the incoming push notification is Lucra-specific. The Lucra SDK will consume and display the notification to the user.

- **Parameters:**
  - `context`: `Context` Context of the Service
  - `remoteMessage`: `RemoteMessage` The push notification data from Firebase
  - `activityClass`: `Class<*>` The Java class of the Activity that you want the push notification to open after a click.
  - `smallIcon`: `Int` The drawable resource id of the icon you want to display next to the Lucra notification

#### Handling Lucra push notification click events

Within the `activityClass` Activity you specified above, handle the incoming `Intent` and parse the Lucra-specific notification to show a `LucraFlow`. Add `handleNotificationIntent` to the following locations.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val flow = LucraPushNotificationService.handleNotificationIntent(intent)
    flow?.let { launchFlow(it) }
}

override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val flow = LucraPushNotificationService.handleNotificationIntent(intent)
    flow?.let { launchFlow(it) }
}
```
`onNewIntent` contains the notification data when the Android app is in the background and the notification is clicked. 

`onCreate` contains the notification data when the app is dead and the notification is clicked

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

Updates the `SDKUser` for the current logged in user. If the user is not logged in yet, the configuration data will be submitted once they do log in. This is helpful to prepopulate information like username, date of birth and phone number.

Passing in `phoneNumber` will prepopulate the phone number login field and lock it.

Passing in `birthday` will prepopulate KYC and SSN forms, make sure to pass in a `Calendar` in UTC


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
          
            is SDKUserResult.WaitingForLogin -> {
                Log.d("Sample", "Waiting for user to login prior to submitting configuration")
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

`retrieveSportsMatchup`

Retrieve a sports contest with the given ID.

- **Parameters:**
  - `matchupId`: ID of the contest.
  - `onResult`: Callback with a result of type `RetrieveGamesMatchupResult`.

- **Example usage:**

```kotlin
LucraClient().getSportsMatchup(
    matchupId = "matchupId",
) { result ->
    when (result) {
        is RetrieveSportsMatchupResult.Failure -> {
            // Handle failure scenario
        }
        RetrieveSportsMatchupResult.SportsMatchupDetailsOutput -> {
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
### Event listeners

`setEventListener`

Sets a event listener so you can listen for Lucra-specific events that occur with the SDK. A `LucraEvent` may hold data associated with the event (ex: `LucraEvent.SportsContest.Create` holds a `contestId`).

- **Parameters:**
  - `LucraEventListener`: listener used to receive a `LucraEvent`

- **Example usage:**

```kotlin
LucraClient().setEventListener(object : LucraEventListener {
  override fun onEvent(event: LucraEvent) {
    //Handle Events
  }
})
```




### UI styling

**Requires `:sdk-ui`***

Your can customize your Lucra implementation with your own color scheme and fonts by providing
the `ClientTheme` object to `LucraClient`.

The `ClientTheme` class has three nested classes, `lightColorStyle`, `darkColorStyle`, and `FontFamily`.

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
    lightColorStyle = ColorStyle(
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
    darkColorStyle = ColorStyle(
      primary = "#09E35F",
      secondary = "#5E5BD0",
      tertiary = "#9C99FC",
      surface = "#1C2575",
      background = "#001448",
      onPrimary = "#001448",
      onSecondary = "#FFFFFF",
      onTertiary = "#FFFFFF",
      onSurface = "#FFFFFF",
      onBackground = "#FFFFFF"
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

`LucraUiProvider.LucraComponent.ContestCard`
Show a sports contest card within your native app.
Pass in the sports contest unique UUID to display the contest card.
```kotlin
val view = LucraClient().getLucraComponent(
  this,
  LucraUiProvider.LucraComponent.ContestCard(
    contestId = contestId //The unique,
  ) {
    launchFlow(it)
  }
)
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

`LucraUiProvider.LucraComponent.RecommendedMatchups`
Show a carousel of recommended matchups within your app
```kotlin
val view = LucraClient().getLucraComponent(
  this,
  LucraUiProvider.LucraComponent.RecommendedMatchups {
    launchFlow(it) // Launch the flow as you would normally
  })
viewGroup.addView(view)
```

### Setting up Convert to Credit

To allow users to withdraw money in credits relevant to your internal system, a `LucraConvertToCreditWithdrawMethod` object must be provided to the `LucraClient`.

Anytime a user changes the the amount field on the Withdraw Screen, the function `getCreditAmount` will be called from the provided `LucraConvertToCreditProvider`.

If `setConvertToCreditProvider` is never called or set to `null`, no Convert to Credit card will be displayed on the Withdraw Screen.

```kotlin
LucraClient().setConvertToCreditProvider(object : LucraConvertToCreditProvider {
  override suspend fun getCreditAmount(cashAmount: Double): LucraConvertToCreditWithdrawMethod {
    val convertedAmount = withdrawalDollarAmount * 3
    return LucraConvertToCreditWithdrawMethod(
      id = "Unique id",
      type = "game-credits",
      title = "Game Credits",
      amount = withdrawalDollarAmount,
      convertedAmount = convertedAmount,
      iconUrl = "https://a-path-to-a-icon.com",
      convertedAmountDisplay = "$convertedAmount credits",
      shortDescription = "This is a short description for the end user.",
      longDescription = "This is a longer, more descriptive version of a message you want the end user to see about.",
      //You can provide metadata about the conversion to be saved on your server
      metaData = mapOf("key" to "value"),
      //See image below for UI Example
      theme = WithdrawCardTheme(
        cardColor = "#5A1668",
        cardTextColor = "#FFFFFF",
        pillColor = "#5A1668",
        pillTextColor = "#FFFFFF",
      )
    )
  }
})
```

The info set in `ConvertToCreditWithdrawMethod` will be passed to Lucra's servers and then communicated to your servers through webhooks.

<img src="..%2FdocAssets%2FConvertToCreditImage.png" alt="Convert to Credit Image" width="200"/>
