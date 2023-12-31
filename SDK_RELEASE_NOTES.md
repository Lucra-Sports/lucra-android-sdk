# Release Notes
## Upcoming Release Changes
## 2.3.0-beta
* Fixed issues around missing matchups
* Fixed user balance emission updates
* Included Aeropay functionality
* Fixed deposit flow bugs

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