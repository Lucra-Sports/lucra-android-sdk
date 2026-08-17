## 6.9.0
* `ClientTheme` now takes separate `lightColorStyle` and `darkColorStyle` parameters, and its single `colorStyle` property no longer exists. The deprecated `ClientTheme(colorStyle, fontFamily)` constructor now maps `colorStyle` to `darkColorStyle` only.
* Added light mode support to the SDK UI. Supplying only `darkColorStyle` (or using the deprecated constructor) keeps the SDK locked to dark, supplying only `lightColorStyle` locks it to light, and supplying both makes the SDK follow the system appearance. Lucra owns `background`, `surface`, `onBackground`, and `onSurface`; partner values for those `ColorStyle` fields are ignored. See [Theming/Appearance](1.2.1_theming.md) and the `uiMode` host-Activity requirement in [Project Setup](1.0.0_project_setup.md#appearance-lightdark-configuration-changes).
* Added optional `includePrivateViewableTournaments` parameter (defaults to `false`) to the lightweight `LucraClient.queryRecommendedTournaments(locationId, ...)` overload. When `true`, the response also includes private tournaments that are viewable without a join code — a code is still required to join them. Check the new `RecommendedTournamentLight.isPrivate` convenience property (or `visibilityLevel`) to distinguish them. See [3.3_tournaments_headless.md](3.3_tournaments_headless.md).
* The Minigames Game Mode Selection tournaments tab now surfaces private tournaments.

## 6.8.0
* New headless method `LucraClient.retrieveTournamentDetails(tournamentId, leaderboardLimit, leaderboardOffset, onResult)` returns a lightweight `TournamentDetails` payload backed by the `ui_tournament_details` API — the same response that powers Lucra's in-app tournament details screen. It includes lifecycle flags, buy-in details, reward type, payout structure, how-to-play steps, earned rewards, attempt/replay data, a pageable leaderboard, and terms. See [3.3_tournaments_headless.md](3.3_tournaments_headless.md) for the full type reference and example.
* Deprecated `LucraClient.retrieveTournament(tournamentId, onResult)` in favor of `retrieveTournamentDetails` for a smaller headless response. The deprecated call keeps working unchanged.
* `LucraClient.joinTournament(...)` now validates the tournament via the lighter `ui_tournament_details` request instead of the full tournament query. Behavior and results are unchanged.

## 6.7.0
* Added `allowRewardSheetToDisplay` parameter to `LucraClient.initialize()` (defaults to `true`). When set to `false`, reward sheets are globally suppressed and will never appear — neither inside the SDK UI nor outside of it. Mini games flow no longer shows the reward sheet.
* Hardened the `LucraClient.startMiniGame` session-launch guardrails to match the join-tournament/create-matchup contract.
* Added `LucraUiProvider.LucraFlow.MinigamesProfile` which is a minigames-flavored profile screen.
* Added `LucraUiProvider.LucraFlow.MinigamesRewards`, the minigames-flavored achievement rewards screen. It groups achievement progress and claimable rewards by game, presents the reward redemption flow, and provides per-game claimed-reward history.
* Added `LucraUiProvider.LucraFlow.MinigamesHome`, the minigames-flavored home screen (profile pill, achievement card, game carousel, featured tournament). Playing a game routes into Game Mode Selection (mode/wager/battle-tier picker) before launching the minigame.
* Added `LucraUiProvider.LucraFlow.MinigameMatchupDetails(matchupId)`, the minigames-themed matchup details screen. Prefer launching `MatchupDetails` from outside the minigames flow; it routes to this destination automatically when the matchup's game is minigame-enabled.
* Added headless method `LucraClient.getMiniGames(onResult)`, returns every minigame enabled for the current tenant, each with the tenant's subscribed config options.
* Introduced `handlePostNavigation` to `LucraUiProvider.LucraFlow.MiniGame` to allow Tournament details to automatically launch any Minigame related tournament join intents. Defaults to `false`.

## 6.6.2
* Removed the "Go" button from the achievements screen.

## 6.6.1
* Fixed sporadic addObserver off main thread exception

## 6.6.0
* `LucraClient.retrieveTournament(tournamentId, onResult)` now returns the full tournament reward structure. Two new fields are added to `Tournament` — a purely additive, non-breaking change; all existing fields (including the cash `rewardStructure` list) are unchanged:
  * `rewardType: String?` — the raw API reward category (`"POOL_CASH_REWARD"` for cash tournaments, `"POOL_TENANT_REWARD"` for tangible-prize tournaments). Use this to branch your reward presentation logic. `null` when the backend does not supply it.
  * `payoutStructure: PayoutStructure?` — the complete payout structure: pre-formatted place/position/reward/amount labels, jackpot total, percentage-vs-fixed and show-amount flags, and an ordered list of `PayoutReward` rows. Each `PayoutReward` carries an optional `CatalogReward` (type, title, description, icon, banner, disclaimer) for tangible-prize rows. `CatalogReward` intentionally omits redemption details (discount codes, claim URLs, free-item IDs). `null` when the backend returns none.
  * See [3.3_tournaments_headless.md](3.3_tournaments_headless.md) for the full type reference and example.
* Provided correct `LucraClient.startMiniGame` consumer proguard files.

## 6.5.3
* Removed CTA buttons from the profile matchups empty state

## 6.5.2
* Fixed location fetching errors.

## 6.5.1
* Fixed a bug where insufficient fund would lead to infinite loading when the tenant has deferred kyc on.

## 6.5.0
* Added headless method `LucraClient.startMiniGame(gameId, gameMode, amount, matchupId, onProgress, onResult)` — orchestrates user validation, funds check, GeoComply verification, and returns a `MiniGameSession` with the iframe URL for host-owned WebView rendering.
* Added headless method `LucraClient.preloadGeoToken(type)`. Pre-warms a GeoComply token (fire-and-forget) so the next `startMiniGame` call doesn't block on geo verification.
* Added `LucraUiProvider.LucraFlow.MiniGame(gameId, gameMode, amount, matchupId)`. UI-bundled counterpart to `startMiniGame`.
* Added `LucraEvent.MiniGame.Finished(gameId, gameMode, amount, matchupId)`. Emitted after a minigame session ends.
* `LucraClient.claimReward(...)`, `markRewardViewed(...)`, and `getUserTournamentRewards(...)` now also cover minigame matchup rewards (previously tournament-only).
* Added minigame-specific geo compliance contexts (`FreeMinigames` / `CashMinigames`). When a game has `minigameEnabled = true`, tournament joins, GYP matchup creation/acceptance, and minigame sessions now use these contexts instead of the standard `FreeBuyIn` / `CashBuyIn`.
* Added optional `minigameEnabled` parameter to `LucraClient.createRecreationalGame(...)`. Pass `true` when creating a matchup for a minigame-enabled game to use the correct geo compliance context.
* Added `minigameEnabled` field to `Tournament.Game` and `RecommendedTournamentGameLight` headless models.
* Added `LucraClient.observeGamesMatchupFee()` — a `Flow<Double>` that emits the platform service fee for mini games matchups (defaults to `0.05` until config is received).
* Added `LucraClient.getMatchupDetails(...)` and `LucraMatchupDetails`, including group scores, individual payouts, and per-participant ranking scores for recreational game matchup details.
* Introduced Lucra Flow modifiers to better customize flow behavior, such as safe inner padding, per flow on dismiss handlers and the ability to hide the close button for root flow embedded screens.
* Fixed a bug where the home/featured games screen could remain in a loading state indefinitely when no tournament locations were set.
* Added headless demographic submission call.
* New headless method `LucraClient.getUserMatchups(limit, onResult)` returns the current user's matchup history grouped by type (Tournaments, Games, Sports) and status (Results, In Progress, Upcoming, Pending).
* New headless method `LucraClient.uploadUserAvatar(bitmap, onResult)` uploads a new avatar image for the current user. Scales the image, uploads to S3, and updates the avatar URL. The user subscription emits the updated user on completion.
* Added new `LucraFlow` types: `TransactionHistory`, `CustomerSupport`, `ResponsibleGaming`, and `Notifications` for launching SDK screens directly.
* Added `placement`, `userPayout`, `subtypeLabel`, `date`, and `outcome` fields to `ProfileMatchupItem` for richer matchup card display.
* New experimental headless methods for granular reward state mutations (previously only available via the bottom-sheet flows):
  * `LucraClient.claimAchievement(userAchievementId, onResult)` — mark a user achievement as claimed.
  * `LucraClient.markAchievementViewed(userAchievementId, onResult)` — mark a user achievement as viewed.
  * `LucraClient.claimReward(rewardId, onResult)` — mark a tournament reward as claimed.
  * `LucraClient.markRewardViewed(rewardId, onResult)` — mark a tournament reward as viewed.
* Added `currentProgress: Int` to `LucraAchievement`. Completion percentage in the range `0..100`; reaches `100` once `isEarned` flips to `true`.
* Updated Sardine MDI SDK from 1.2.59 to **1.2.67**

## 6.4.1
* Introduced new `queryRecommendedTournaments` to return smaller payloads for tournaments.

## 6.4.0
* Add Trackman Account Linking
* Added Achievements to the SDK — users can now earn, view, and claim achievements tied to their game and tournament activity.
  * New LucraUiProvider.LucraFlow.Achievements entry point presents the full-screen Achievements list.
  * New Achievements bottom sheet entry points.
  * New headless method LucraClient.getUserAchievements(...) returns the current user's achievements with optional filtering.
  * New headless method LucraClient.getUserTournamentRewards((...) -> Unit) returns the current user's tournament rewards with optional filtering.
  * New public models: LucraAchievement, LucraAchievementDefinition, LucraAchievementCriteriaType, LucraAchievementCriteriaConfig.
  * New public reward models: LucraTournamentReward, LucraCatalogReward, LucraDiscountCodeConfig, LucraFreeItemConfig.

## 6.3.0
* Improved mid-flow authentication handling: navigation destinations now correctly enforce authentication requirements.
* Added `score` and `avatarUrl` fields to the headless `Tournament.Participant` model. 
  * Both fields default to `null` for backward compatibility.
* Fixed a bug where stale or incorrect game details could be shown when navigating to the matchup details screen.
* Added reward UI to tournament details
* Added auto settlement support to the Games Your Play flows
* New earned rewards section on the tournament completed page.
* New rewards section explaining the rewards for non finished tournaments.
* New rewards pop up when a user enters the SDK if they have pending rewards that can be claimed.
* Added Achievements to the SDK.
  * New `LucraUiProvider.LucraFlow.Achievements` entry point presents the full-screen Achievements list.
  * New sheet-style reward flows: `LucraUiProvider.LucraFlow.ClaimRewards` and `LucraUiProvider.LucraFlow.ClaimAchievementRewards`.
  * New experimental headless methods `LucraClient.getUserAchievements(...)` and `LucraClient.getUserTournamentRewards(...)`.

## 6.2.0
* **Updated Dependencies:**
  * Upgraded Kotlin from 2.0.21 to **2.2.20**
  * Upgraded kotlinx-coroutines from 1.7.3 to **1.10.1**
  * Upgraded kotlinx-serialization from 1.4.0 to **1.8.0**
* Removed `-Xskip-metadata-version-check` compiler workaround — all library metadata is now aligned
* Migrated build configuration from deprecated `kotlinOptions` to `compilerOptions` (Kotlin 2.2 requirement)
* Removed deprecated `composeOptions` blocks — Compose compiler is now managed via the `kotlin.plugin.compose` plugin

## 6.1.0
* Patched `LucraClient.configure(SDKUser)` function to not allow redundant requests to be made unless there is a meaningful difference between the current user and the submitted user.
* Updated GeoComplyLibrary to `2.18.0`
* Added GeoComplyExtension version `1.0.0`
* Added more clarity on tournament details UI so payouts per user and each ranking on the leaderboard has improved visibility.

## 6.0.1
* Patched critical authentication issue where refreshing tokens would result in invalid user state.

## 6.0.0 (Do not use)
* **[Breaking Change - LucraClient signature update & new apiKey]**
  * `apiUrl` is no longer required and the prior `apiKey` CANNOT be reused. If you don't have the new key already contact Lucra for your new API key.
* Added new `HomePage` flow that launch the home page where user can create tournaments or matchups. Supports unauthed access.
* Added `submitUserScoreByMetadata` and `searchMatchupsByMetadata` to `LucraClient` to allow headless tournament score submissions and searching
* Redesign of the user profile screen `LucraUiProvider.LucraFlow.Profile`

## 5.3.0
* **[Breaking Change - Repository Migration]** SDK artifacts are now published to Maven Central instead of GitHub Packages.
  * No authentication required - artifacts are publicly accessible
  * Remove the `LucraGithubPackages` maven repository and credentials from your build.gradle
  * Simply use `mavenCentral()` which is typically already included in your repositories block
* Added support for unauthenticated tournament details screen.
* Added support for new type of visibility for tournaments:
  * Private: Tournament can only be discovered by entering private code. (no change)
  * Visible: Tournament is publicly viewable on available locations. (no change)
  * Private Visible: Tournament is publicly viewable on available locations, but can only be joined by providing a private join code. (new)
* Tournament details now auto updates the leaderboard every minute.
* Other UI/UX improvements 

## 5.2.0
* Added `LucraUiProvider.LucraFlow.Wallet` flow to allow for a simplified user profile screen.
* Updated GeoComplyContexts to be more streamlined.
* Added support for an unauthenticated Home Screen.

## 5.1.0
* Added `submitUserScore` to `LucraClient` to allow headless tournament score submissions
* Added `Game` data object to `Tournament` to provide more details about tournaments for headless API calls
* Optimized loading time for Tournaments/Games Landing screen queries.
* Tournaments-only clients now have a simplified, vertically scrolling landing page for easier navigation.
* Big cards for featured games now has a scroll indicator as well as "peek" animation for the card so it's more obvious that there are multiple cards in the carousel.
* Add funds bottom sheet is now scrollable on smaller devices.
* Location selector now only shows the venue name, instead of "city - venue name".

## 5.0.1
* Hotfix for preconfigured phone numbers

## 5.0.0
* Added `metadata` field to `SDKUser` to allow linking between client users and Lucra users. This can be updated via the `configure(sdkUser: SDKUser)` method.
* Added `LucraClient().setMatchupInviteDeeplinkProvider` to allows clients to append a Lucra specific matchupID to their existing deeplinks
* [Breaking Change] `DemographicInformationMissing` is now part of `UserStateError` and can be returned if a user who is missing demographic information attempts to join a tournament that requires it.
* Added DemographicForm flow to `LucraUiProvider` for headless access to demographic collection
* Enhanced tournament joining logic to validate demographic data and return specific error types
* Updated demographic form UI to use direct text input for date of birth instead of date picker
* Complete the Android 16kb platform alignment requirement.
* Simplified unused code path for fund flows.
* Added back phone number lock on login if `configure(sdkUser)` is called prior.
* **Major dependency updates and compatibility requirements:**
  * **Updated Dependencies:**
  * Upgraded Kotlin from 1.9.23 to **2.0.21** (major version update with new K2 compiler)
  * Upgraded Android Gradle Plugin (AGP) from 8.3.2 to **8.10.1**
  * Upgraded Gradle from 8.4 to **8.12**
  * Updated Compose BOM from 2025.03.00 to **2025.09.01**
  * Updated AndroidX Navigation from 2.8.3 to **2.9.3**
  * Updated AndroidX Constraint Layout Compose from 1.1.0 to **1.1.1**
  * Updated AndroidX Window Manager to **1.2.0**
  * Updated AndroidX WebKit to **1.14.0**
  * Updated Sardine MDI SDK from 1.2.46 to **1.2.59**
  * **Architecture Changes:**
  * Migrated to new Compose Compiler plugin architecture
  * **Removed Dependencies (cleanup):**
  * Removed `android-installreferrer`
  * Removed `androidx-core-splashscreen`
  * Removed unused Camera2 dependencies
  * Removed unused tracing-ktx dependency
  * And more...

## 4.5.1
* Fixed crash upon viewing completed tournament details

## 4.5.0
* Added `LucraUiProvider.LucraFlow.MatchupDetails` Lucra flow to allow routing to the respective details view for each Matchup Type.
* Added `LucraEvent.GamesContest.StartedActive` to indicate when a Games matchup creator starts a matchup while the non creator is viewing the matchup details. 
* Added SnackBar
* Reduced the amount of clicks/screens to complete KYC flow
* Reduced number of clicks when Signing in by removing the Terms & Conditions checkbox.
* Updated the funding and withdraw flow with a new design that reduces the number of clicks required to complete the process.
* Improved the matchup joining flow, making it more streamlined with fewer clicks required.
* Updated how we fetch tournament data to consume less bandwidth.
* Introduced Feature Flag to configure profile FAB

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
