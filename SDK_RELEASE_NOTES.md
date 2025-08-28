## 4.4.0
* Enhanced UI/UX for tournament flows. 
  * You can now perform actions that are not dependent on the load state. 
  * Enhanced haptic feedbacks. 
  * Enhanced error handling. 
* Fixed a bug where only the "tournament" and "game" were filterable categories.
* Fixed a bug where tournament details screen would not prompt for location permission.
* Removed various unused assets.
* Removed dispute button when user is not part of the matchup.
* Demographic collection form will now auto-fill information provided before.
* Updated to allow setting `locationId` via `LucraClient` at any point (after SDK init) in the flow. 
  * Consolidated tournament location operations into `TournamentLocationProvider`
  * `TournamentLocationProvider` allows setting and getting of `locationId` either from user or from client.
* Tournaments where buy in is $0 (free) can be joined with a single tap from the big featured game cards.
* Bug fix of how tournament pot was displayed in the big featured game cards.

## 4.3.0
* Updated Wordings for free to play tournament
* New UI for phone number authentication flow
* Fixed a bug where game ID was not being passed when creating a Games matchup directly from bottom sheet
* Other various UI improvements

## 4.2.1
* Fixed issue where invalid geocomply license key would cause the SDK to crash

## 4.2.0
* Updated Geocomply to 2.17.0
* Added LucraEvent `LucraEvent.GamesContest.Started` to indicate when a Games matchup creator has pressed the "Start Matchup" button. Note: All other Games related events are now firing for the Free For All and Team vs Team matchups.
* Free to play products now does not require age assurance check, instead soft checks against DOB
* Introduced OrbitMVI library (version `9.0.0`)
* Added the ability to create a tournament with a private join code that only allows users with that code to join
* Added the ability to replay a tournament for select locations and tournaments

## 4.1.1
* [Breaking Change] updated `LucraMatchup`'s `atStake` field to `buyInAmount`, and type from `AtStake` object to `Double`.
  * if the game is `tenantReward` based, the `buyInAmount` will be `0.0`.
* You can still access the `Reward` object that represents a `tenantReward` through `lucraMatchup.participantGroups.participants.tenantReward`.

## 4.1.0
* [Breaking Change] `LucraClient.getMatchup()` returns `LucraMatchup` on success. This version updates the object's `recreationGameExtension` property to include `AtStake` object instead of `buyInAmount: Int` that represented the buy in amount.
* [Breaking Change] `LucraMatchupRecreationalGameExtension` now contains `AtStake` object instead of `buyInAmount: Int`. `AtStake` can either be buy in amount, or a client defined `Reward` object.
* Free to play (`Reward`) feature added for group play!
* UI enhancements across Group Play and Tournaments.

## 4.0.1
* Updates to GeoComply

## 4.0.0
* [Breaking Change] Methods to get(both sport and recreational matchups), create, accept, and cancel recreational games matchup has been migrated:
  * `LucraClient.getMatchup`
  * `LucraClient.createRecreationalGame`
  * `LucraClient.acceptVersusRecreationalGame`
  * `LucraClient.acceptFreeForAllRecreationalGame`
  * `LucraClient.cancelRecreationalGame`
* Profile page performance enhancement. We now only pre-load 20 items for in progress, accepted and completed matchups.
* Updated `com.braintreepayments.api` `paypal`, `venmo`, and `data-collector` SDKs to `5.8.0`
* Improved geolocation related performance.
* UI updates to Game Matchup Details screen.
* Updated compose dependencies: `androidxComposeBom` to `2025.03.00`, `androidxConstraintLayoutCompose` to `1.1.0`
* Upgraded `androidx.navigatio:nnavigation-compose` to `2.8.3`, resolving backwards compatibility issues with `2.7.7`
* Patched authentication subscription issues when token would be dropped during an attempted request
* Migrated to `androidx.compose.material3:material3` removing dependency to the legacy `androidx.compose.material:material` library
* Added `LucraUiProvider.LucraFlow.TournamentDetails` Lucra flow
* Group play feature added. We now support teams versus (team vs team) and free for all matchups for games!
* `CreateGamesMatchup` now supports optional `locationId` to specify a location for filtered tournaments

## 3.13.1
* Updated `Tournament` data model to include `potNetAmount`.

## 3.13.0
* Added `accountStatus` to `SDKUser` payload to indicate the user's account status
* Fixed Games Creation and Acceptance flows to always show Loading dialogs after requesting location permission
* Depreciated fields `background`, `onBackground`, `surface`, and `onSurface` in `ColorStyle`
* Improved Games Create flow with a more streamlined UI
* Improved Sports You Watch flow with a more streamlined UI
* Sports You Watch now supports live games

## 3.12.0
* [Breaking Change] The [RetrieveGamesMatchupResult] now returns [TopLevelMatchupType] instead of [SportsMatchupType] on success callback.
* [Breaking Change] The [TopLevelMatchupType]'s field `ownerId` is now `creatorId` and `type` is now `subtype`
* Bumped Aerosync version from `1.0.9` to `1.3.0`
* Updated how bank deletion logic is handled per provider
* Added `LucraUiProvider.LucraFlow.GamesMatchupDetails` Lucra flow
* Updated Geocomply version to support TargetSDK 35
* Added `LucraClient().closeFullScreenLucraFlows()` to close all open LucraFlows
* Fixed network overlay from displaying too frequently

## 3.11.4
* Renamed `Tournament` fields `tournamentId` and `poolTotalAmount`. Added `Tournament` fields `metadata` and `iconUrl`
* Renamed `Partcipiant` field `userId`

## 3.11.3
* Introduced support for Tournaments
* Introduced `LucraClient().joinTournament()`, `LucraClient().queryRecommendedTournaments()` and `LucraClient().retrieveTournament()`

## 3.11.2
* Introduced Nuvei for Deposits
* Introduced `LucraRewardProvider#viewRewards` to support the Profile  "View My Rewards" entry. **This will not be backwards compatible if `LucraRewardProvider` is implemented.**
* Added `LucraUiProvider.LucraFlow.CreateGamesMatchupById` Lucra flow

## 3.11.1
* Upgraded `androidx.navigatio:nnavigation-compose` to 2.8.3, resolving backwards compatibility issues with 2.7.7
* Patched authentication subscription issues when token would be dropped during an attempted request
* RetrieveGamesMatchupResult now contains `game` param that contains the game details associated with the matchup

## 3.11.0
* NotificationChannel is now set up automatically by the SDK - no need to define it on the consumer side.
* LucraRewardProvider is now available for FTP SYW usage. See release notes for details.

## 3.10.1
* Introduced Nuvei for ACH withdrawals
* UI improvements to Games contests
* Bug fix related to Android Material library versions

## 3.10.0
* [Breaking Change] The params for `LucraConvertToCreditWithdrawMethod` were updated. The param `type` was remove and `conversionTerms` was added.
* [Deprecated] User's `birthday` field for configuration. It will now be ignored and return as null in `SDKUser`.

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