## 3.1.0
* Allow public feed (`LucraFlow.PublicFeed`) to be filtered by league IDs through `SdkIdFilter`. It can be accessed through `LucraClient().getPublicFeedLeagueIdFilter()`.
* Deprecated `LucraClient().getSDKUser`
* Introduced `LucraClient().observeSDKUserFlow()` and `LucraClient().observeSDKUser { sdkUser -> }`, these will emit the active user logged in and returns null upon logging out
* `SDKUser` now includes `avatarUrl`
* Added `LucraComponent.ProfilePill` (View) for embedded Profile Pills
* Added `LucraComponent.MiniPublicFeed` (View) to embed the public feed within the host app
* Added `LucraClient().getGamesMatchup()` API call to retrieve games matchups
* Bug fixes to public feed and profile flows

## 3.0.0
* Added required `apiUrl` to `LucraClient` constructor
* Updated `authClientId` to `apiKey`
* Added support for unauthed viewing of the public feed. Any proceeding actions will now launch the auth flow
* Improvements to `LucraFlow` experience
* Added update username API for scenarios where only the username needs updates

## 2.3.1-beta
* This version is largely a test to verify release automation
* Various bug fixes and UI updates
* Allow user info of a same user to be updated when username has not changed
* Update Aerosync sdk to 1.0.8
* Refactor Aerosync response handling per breaking change coming in March 
* Update deeplink handling back to the SDK

## 2.3.0-beta
* Fixed issues around missing matchups
* Fixed user balance emission updates
* Included Aeropay functionality
* Fixed deposit flow bugs
* Aeropay deeplinks back to add funds page after bank link.

## 2.2.0-beta
* Removed "Send Reminders" card from the public feed
* Updated "Created Contest" popup to notify user where to find their contest
* Added notifications to profile screen
* Moved continue button up on sms auth screen
* Fixed DatePicker UI

## 2.1.0-beta
* Introduced `LucraClient().getSDKUser {...}` to retrieve the current logged in user
* Introduced `LucraClient().configure(sdkUser)` to update the current logged in user
* Fixed UserService state to listen to user authed status
* Now showing username in SDK profile view
* Fixed Public Feed refresh state
* Introduced `LucraClient().logout(context)`
* Introduced new flow `LucraFlow.Login`

## 2.0.0-beta

* [Breaking Change] Introduced required `LucraFlowListener` as a property of `LucraUi` to consume permitted flow launch points. See README for more details
  * Implement `launchNewLucraFlowEntryPoint` to accept full screen launch points
  * Implement `onFlowDismissRequested` to dismiss any full screen launch points, including the original launch point.
* [Breaking Change] Removed `LucraClient#lucraClientListener` in favor of `LucraFlowListener` in `LucraUi`
* Fixed duplicate logs upon client recreation

## 1.1.2-beta
* Introduced SANDBOX Environment, `environment = Environment.SANDBOX`
* Updated maven artifact location from `https://maven.pkg.github.com/Lucra-Sports/lucra-android` to `https://maven.pkg.github.com/Lucra-Sports/lucra-android-sdk`

## 1.1.1-beta
* Testing release process, no changes

## 1.1.0-beta

* API Create and Cancel Contests
* LucraUI introduced to support lucra flows
  `LucraUiProvider.LucraFlow.VerifyIdentity`
  `LucraUiProvider.LucraFlow.AddFunds`
  `LucraUiProvider.LucraFlow.WithdrawFunds`
  `LucraUiProvider.LucraFlow.CreateGamesMatchup`