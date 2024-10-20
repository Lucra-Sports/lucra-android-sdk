## 3.9.2
* UI improvements to Deposit & Withdraw funds flow

## 3.9.1
* Various UI improvements to GYP flow

## 3.9.0
* Added Convert to Credit Withdrawal option

## 3.8.1
* Improvements to add funds flow.
* Paypal/Venmo is no longer auto-selected as a default deposit method.
* Bug fixes around feature flags and theming.

## 3.8.0
* Get Matchup API call now supports unauthed calls
* Improvements to Sports Player selection screens
* Introduced Paypal and Venmo support for deposit and Withdrawal

## 3.7.0
* Added "View All" to `MyMatchups` and `Profile` to view and search against specific categories of your Matchups
* Added the ability to rematch a GYP contest and automatically invite teammates and opponents.
* Added 'TakeEm' contests to the public feeds
* UX updates to Add Funds text field

## 3.6.1
* Light & Dark mode styling updates

## 3.6.0
* [Breaking Change] Updated `getSportsMatchup(matchupId)` to return new `SportsMatchupType` model
* Upon logging out in the middle of a Flow, LoginFlow will reset the Flow stack and launch
* A "No network" screen will show if network drops for any reason.
* Added Head to Head Stats
* Client theme now supports light and dark mode. `ClientTheme`'s `colorStyle` constructor field is deprecated. Use new `ClientTheme(lightColorStyle: ..., darkColorStyle:...)` instead.
* Fixed View All player and opponent stats
* `Login` flow can now be entered even if user is already logged in. They'll be met with "User is already onboarded"
* `SDKUser` will now accept a birthday `Calendar` object to prefill KYC and SSN flows. This birthday will continue to emit with user updates.
* Updated money transaction notifications to navigate user to Transaction history
* Added ability to edit a user's profile image
* Added support to see all submitted support request and add additional comments to them
* Various bug fixes around Verification status, sports public feeds and styling.

## 3.5.0
* Fixed invalid login auth state
* Added TOS bottom sheet
* Fixed add funds logic around accepting GYP flow
* Fixed various deposit flow bugs
* Improved CC expiration date input
* Added additional address line for CC entry
* Added unsupported CC type error
* Migrated user stats query from v3 to v4

## 3.4.1
* [Deprecated Release] Use version >=3.5.0
* Added update username feature from profile
* Fixed SDK state when application restarts from configuration changes
* Withdrawal and add funds actions will invoke device security.
* Bug fix with suspension screens
* Added responsible gaming screens

## 3.4.0
* [Deprecated Release] Use version >=3.5.0
* Added `SDKUserResult.WaitingForLogin` to depict scenarios where the User action is awaiting completion of user login prior to completing the user action. E.g. `LucraClient().configure(sdkUser)`
  * After successful configuration and login, the user info will be automatically updated
* If `configure(sdkUser)` is called prior to login, the (valid) phone number will be used to prepopulated the login screen.
* Lucra specific push notifications are now supported
* Fixed user auth subscription issue when user logs out
* Fixed SDKUser balance

## 3.3.1
* [Deprecated Release] Use version >=3.5.0
* Created Contest Flows now respect the configuration of the client. e.g. Games you play vs games you watch
* Fixed insufficient funds logic
* Multiple Games You Play bug fixes
* Updated multiple androidx and plugin dependencies, please notify us if you notice any issues
  * Desuraging is now required by host application with latest kotlin and AGP dependencies
  * ```
    compileOptions {
    isCoreLibraryDesugaringEnabled = true
    }
  
    dependencies {
      coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    }
    ```

## 3.3.0
* [Deprecated Release] Use version >=3.5.0
* Added `LucraComponent.RecommendedMatchups` LucraComponent
* Fixed unauthed flow user experience
* Added `LucraClient().getSportsMatchup()` API call to retrieve sports matchups
* Added support for unauthed API call for GYP and SYW contest details

## 3.2.0
* [Deprecated Release] Use version >=3.5.0
* Added `LucraComponent.ContestCard` (View) to embed a sports contest card within the host app
* Added Floating Action Button to User Profile
* Updated MiniPublicFeed to allow a list of player ids instead of only two
* Updated MiniPublicFeed to show empty state when no contests are available
* Added `LucraComponent.FloatingActionButton` (View) for embedded FABs to create sports contests.
* Added balance to SDKUser to represent the user's account balance
* [Breaking Change] `FontFamily` no longer accepts a list of fonts. You must explicitly set each `Font` associated with each text style
  * Param name for `Font` changed from `fontName` to `fontAssetFilePath`
  * `weight` param was remove from `Font`
  * All font files must be included in the `src/main/assets` directory
  * `fontAssetFilePath` must include the file extension
* Added `LucraClient().setDeeplinkTransformer` to enable modification to QR deeplink uri generated by the SDK. This allows lucra specific URI to be modified/wrapped to client specific string.
* Added `LucraClient().getLucraFlowForDeeplinkUri` to convert a valid deeplink lucra URI into a flow.
* `setDeeplinkTransformer` and `getLucraFlowForDeeplinkUri` is meant to be used in conjunction to allow client specific QR code generation as well as later converting lucra URI contained in the QR code to a lucra flow (UI).  
* Added `LucraClient().setEventListener` to allow a way for clients to listen to events and transmit data from event occurring within the SDK.

## 3.1.0
* [Deprecated Release] Use version >=3.5.0
* Allow public feed (`LucraFlow.PublicFeed`) to be filtered by league IDs through `SdkIdFilter`. It can be accessed through `LucraClient().getPublicFeedLeagueIdFilter()`.
* Deprecated `LucraClient().getSDKUser`
* Introduced `LucraClient().observeSDKUserFlow()` and `LucraClient().observeSDKUser { sdkUser -> }`, these will emit the active user logged in and returns null upon logging out
* `SDKUser` now includes `avatarUrl`
* Added `LucraComponent.ProfilePill` (View) for embedded Profile Pills
* Added `LucraComponent.MiniPublicFeed` (View) to embed the public feed within the host app
* Added `LucraClient().getGamesMatchup()` API call to retrieve games matchups
* Bug fixes to public feed and profile flows

## 3.0.0
* [Deprecated Release] Use version >=3.5.0
* Added required `apiUrl` to `LucraClient` constructor
* Updated `authClientId` to `apiKey`
* Added support for unauthed viewing of the public feed. Any proceeding actions will now launch the auth flow
* Improvements to `LucraFlow` experience
* Added update username API for scenarios where only the username needs updates

## 2.3.1-beta
* [Deprecated Release] Use version >=3.5.0
* This version is largely a test to verify release automation
* Various bug fixes and UI updates
* Allow user info of a same user to be updated when username has not changed
* Update Aerosync sdk to 1.0.8
* Refactor Aerosync response handling per breaking change coming in March 
* Update deeplink handling back to the SDK

## 2.3.0-beta
* [Deprecated Release] Use version >=3.5.0
* Fixed issues around missing matchups
* Fixed user balance emission updates
* Included Aeropay functionality
* Fixed deposit flow bugs
* Aeropay deeplinks back to add funds page after bank link.

## 2.2.0-beta
* [Deprecated Release] Use version >=3.5.0
* Removed "Send Reminders" card from the public feed
* Updated "Created Contest" popup to notify user where to find their contest
* Added notifications to profile screen
* Moved continue button up on sms auth screen
* Fixed DatePicker UI

## 2.1.0-beta
* [Deprecated Release] Use version >=3.5.0
* Introduced `LucraClient().getSDKUser {...}` to retrieve the current logged in user
* Introduced `LucraClient().configure(sdkUser)` to update the current logged in user
* Fixed UserService state to listen to user authed status
* Now showing username in SDK profile view
* Fixed Public Feed refresh state
* Introduced `LucraClient().logout(context)`
* Introduced new flow `LucraFlow.Login`

## 2.0.0-beta
* [Deprecated Release] Use version >=3.5.0
* [Breaking Change] Introduced required `LucraFlowListener` as a property of `LucraUi` to consume permitted flow launch points. See README for more details
  * Implement `launchNewLucraFlowEntryPoint` to accept full screen launch points
  * Implement `onFlowDismissRequested` to dismiss any full screen launch points, including the original launch point.
* [Breaking Change] Removed `LucraClient#lucraClientListener` in favor of `LucraFlowListener` in `LucraUi`
* Fixed duplicate logs upon client recreation

## 1.1.2-beta
* [Deprecated Release] Use version >=3.5.0
* Introduced SANDBOX Environment, `environment = Environment.SANDBOX`
* Updated maven artifact location from `https://maven.pkg.github.com/Lucra-Sports/lucra-android` to `https://maven.pkg.github.com/Lucra-Sports/lucra-android-sdk`

## 1.1.1-beta
* [Deprecated Release] Use version >=3.5.0
* Testing release process, no changes

## 1.1.0-beta
* [Deprecated Release] Use version >=3.5.0
* API Create and Cancel Contests
* LucraUI introduced to support lucra flows
  `LucraUiProvider.LucraFlow.VerifyIdentity`
  `LucraUiProvider.LucraFlow.AddFunds`
  `LucraUiProvider.LucraFlow.WithdrawFunds`
  `LucraUiProvider.LucraFlow.CreateGamesMatchup`