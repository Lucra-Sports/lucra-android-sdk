package com.lucrasports.sdk.app.catalog

import com.lucrasports.sdk.app.catalog.AuthRequirement.HostBlocks
import com.lucrasports.sdk.app.catalog.AuthRequirement.SdkPrompts
import com.lucrasports.sdk.app.catalog.SampleCategory.AuthIdentity
import com.lucrasports.sdk.app.catalog.SampleCategory.DebugTools
import com.lucrasports.sdk.app.catalog.SampleCategory.EmbeddedComponents
import com.lucrasports.sdk.app.catalog.SampleCategory.GamesMinigames
import com.lucrasports.sdk.app.catalog.SampleCategory.HomeFeeds
import com.lucrasports.sdk.app.catalog.SampleCategory.ProfileWallet
import com.lucrasports.sdk.app.catalog.SampleCategory.RecreationalGames
import com.lucrasports.sdk.app.catalog.SampleCategory.RewardsAchievements
import com.lucrasports.sdk.app.catalog.SampleCategory.SportsContests
import com.lucrasports.sdk.app.catalog.SampleCategory.Tournaments
import com.lucrasports.sdk.app.catalog.SampleSurface.Api
import com.lucrasports.sdk.app.catalog.SampleSurface.Flow
import com.lucrasports.sdk.app.catalog.SampleSurface.Ui
import com.lucrasports.sdk.core.ui.LucraUiProvider.LucraComponent
import com.lucrasports.sdk.core.ui.LucraUiProvider.LucraFlow

/**
 * Every entry point the sample app exposes. Adding a demo is one entry here.
 *
 * @param includeDebugOnly false on release builds.
 */
internal fun sampleCatalog(includeDebugOnly: Boolean): List<SampleEntry> =
    allEntries.filter { includeDebugOnly || !it.debugOnly }

private const val AUTH = "auth"
private const val HEADLESS = "headless"

private val allEntries: List<SampleEntry> = listOf(

    SampleEntry(
        id = "api.phone_authentication",
        title = "Phone Authentication",
        description = "Authenticate via phone number with an SMS verification code. No login required.",
        category = AuthIdentity, surface = Api,
        keywords = listOf(HEADLESS, AUTH, "sms", "otp", "sign in", "login"),
    ) { phoneAuthApi.showPhoneAuthDialog() },

    SampleEntry(
        id = "api.handshake_authentication",
        title = "Handshake Authentication",
        description = "Sign in from a partner-signed token with no Lucra login UI. Mints test tokens " +
            "on device, with failure injection to observe the phone auth fallback.",
        category = AuthIdentity, surface = Api,
        keywords = listOf(HEADLESS, AUTH, "token", "jwt", "partner", "sso"),
    ) { handshakeAuthApi.showHandshakeAuthScreen() },

    SampleEntry(
        id = "api.logout",
        title = "Logout",
        description = "Log the current user out.",
        category = AuthIdentity, surface = Api,
        keywords = listOf(HEADLESS, AUTH, "sign out"),
    ) { userApi.logout() },

    SampleEntry(
        id = "flow.login",
        title = "Login",
        description = "Navigate directly to the login screen. Exits immediately if already signed in.",
        category = AuthIdentity, surface = Flow,
        keywords = listOf(AUTH, "sign in"),
    ) { launchFlow(LucraFlow.Login) },

    SampleEntry(
        id = "flow.verify_identity",
        title = "Verify User Identity",
        description = "Navigate to the identity verification screen.",
        category = AuthIdentity, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "kyc", "verification", "identity"),
    ) { launchFlow(LucraFlow.VerifyIdentity) },

    SampleEntry(
        id = "flow.demographic_form",
        title = "Demographic Form",
        description = "Navigate to the demographic form to collect user information.",
        category = AuthIdentity, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "dob", "email", "zip", "address"),
    ) { launchFlow(LucraFlow.DemographicForm) },

    SampleEntry(
        id = "api.submit_demographic_form",
        title = "Submit Demographic Form",
        description = "Submit demographic data headlessly: DOB, email, zip and name.",
        category = AuthIdentity, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "dob", "email", "zip"),
    ) { users.showSubmitDemographicFormDialog() },

    SampleEntry(
        id = "api.check_kyc_status",
        title = "Check KYC Status",
        description = "Verify the KYC status of the current user.",
        category = AuthIdentity, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "kyc", "verification", "identity"),
        // The bang-bangs are safe only because HostBlocks gates this entry.
    ) { userApi.checkKYCStatus(currentUser!!.userId!!) },

    SampleEntry(
        id = "api.configure_user",
        title = "Configure User",
        description = "Update or preconfigure the SDK user, signed in or not.",
        category = AuthIdentity, surface = Api,
        keywords = listOf(HEADLESS, AUTH, "sdk user", "preconfigure"),
    ) { users.showConfigureUserDialog(currentUser) { onUserUpdated(it) } },

    SampleEntry(
        id = "api.update_username",
        title = "Update Username",
        description = "Update the current user's username.",
        category = AuthIdentity, surface = Api,
        keywords = listOf(HEADLESS, AUTH, "username", "handle"),
    ) { users.showUpdateUsernameDialog(currentUser) { onUserUpdated(it) } },

    SampleEntry(
        id = "flow.profile",
        title = "Profile",
        description = "Navigate to the current user's profile.",
        category = ProfileWallet, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "account", "user"),
    ) { launchFlow(LucraFlow.Profile) },

    SampleEntry(
        id = "flow.wallet",
        title = "Wallet",
        description = "Navigate to the current user's wallet.",
        category = ProfileWallet, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "balance", "funds", "money"),
    ) { launchFlow(LucraFlow.Wallet) },

    SampleEntry(
        id = "flow.add_funds",
        title = "Add Funds",
        description = "Navigate to the add funds flow.",
        category = ProfileWallet, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "deposit", "wallet", "money"),
    ) { launchFlow(LucraFlow.AddFunds) },

    SampleEntry(
        id = "flow.withdraw_funds",
        title = "Withdraw Funds",
        description = "Navigate to the withdraw funds flow.",
        category = ProfileWallet, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "cash out", "wallet", "money", "credit"),
    ) { launchFlow(LucraFlow.WithdrawFunds) },

    SampleEntry(
        id = "flow.minigames_profile",
        title = "Minigames Profile",
        description = "The minigames-flavored profile with stats and competition results.",
        category = ProfileWallet, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "minigame", "arcade", "stats"),
    ) { launchFlow(LucraFlow.MinigamesProfile) },

    SampleEntry(
        id = "flow.home_page",
        title = "Home Page",
        description = "The primary starting point for Tournaments and Games.",
        category = HomeFeeds, surface = Flow,
        keywords = listOf("landing", "location id"),
    ) { flows.showHomePageDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "flow.public_sports_feed",
        title = "Public Sports Feed",
        description = "The public sports feed. Any action from it prompts the user to authenticate.",
        category = HomeFeeds, surface = Flow,
        keywords = listOf("feed", "social", "sports"),
    ) { launchFlow(LucraFlow.PublicFeed) },

    SampleEntry(
        id = "flow.minigames_home",
        title = "Minigames Home",
        description = "Profile pill, achievement card, game carousel and featured tournament. " +
            "Playing routes into Game Mode Selection.",
        category = HomeFeeds, surface = Flow,
        keywords = listOf("minigame", "arcade", "carousel", "landing"),
    ) { launchFlow(LucraFlow.MinigamesHome) },

    SampleEntry(
        id = "flow.create_sports_matchup",
        title = "Create Sports Matchup",
        description = "Navigate to the create sports matchup flow.",
        category = SportsContests, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "syw", "sports you watch", "contest"),
    ) { launchFlow(LucraFlow.CreateSportsMatchup) },

    SampleEntry(
        id = "flow.show_matchup",
        title = "Show Matchup",
        description = "Navigate to the matchup details flow for a matchup ID.",
        category = SportsContests, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "details", "contest"),
    ) { flows.showMatchupDetailsDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "flow.deeplink_matchup_details",
        title = "Deeplink to Matchup Details",
        description = "Navigate to a specific matchup via a legacy deeplink URI.",
        category = SportsContests, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "uri", "link", "legacy"),
    ) { flows.showDeeplinkDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "api.retrieve_matchup_details",
        title = "Retrieve Matchup Details",
        description = "Fetch a matchup by ID and print the result.",
        category = SportsContests, surface = Api,
        keywords = listOf(HEADLESS, "fetch", "contest"),
    ) { matchupApi.showRetrieveMatchupDialog() },

    SampleEntry(
        id = "api.subscribe_matchup_details",
        title = "Subscribe to Matchup Details",
        description = "Subscribe to live updates for a matchup ID.",
        category = SportsContests, surface = Api,
        keywords = listOf(HEADLESS, "live", "realtime", "websocket"),
    ) { matchupApi.showSubscribeMatchupDialog() },

    SampleEntry(
        id = "api.cancel_matchup_subscription",
        title = "Cancel Matchup Details Subscription",
        description = "Cancel the active matchup details subscription, if any.",
        category = SportsContests, surface = Api,
        keywords = listOf(HEADLESS, "live", "realtime", "unsubscribe"),
    ) { matchupApi.cancelMatchupDetailsSubscription() },

    SampleEntry(
        id = "api.get_user_matchups",
        title = "Get User Matchups",
        description = "Retrieve the current user's matchups grouped by type and status.",
        category = SportsContests, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "mine", "contests"),
    ) { userApi.getUserMatchups() },

    SampleEntry(
        id = "api.search_matchups_by_metadata",
        title = "Search Matchups by Metadata",
        description = "Search for matchups using metadata criteria.",
        category = SportsContests, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "metadata", "query"),
    ) { tournaments.showSearchMatchupsByMetadataDialog() },

    SampleEntry(
        id = "flow.create_games_matchup",
        title = "Create Games Matchup",
        description = "Navigate to the create games matchup flow.",
        category = GamesMinigames, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "gyp", "games you play", "contest"),
    ) { flows.showCreateGamesMatchupDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "flow.show_minigame_matchup",
        title = "Show Minigame Matchup",
        description = "Minigames-themed matchup details. Requires a matchup whose game has " +
            "minigame_enabled = true.",
        category = GamesMinigames, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "minigame", "arcade", "details"),
    ) { flows.showMinigameMatchupDetailsDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "flow.minigame",
        title = "MiniGame",
        description = "Launch the MiniGame flow. Prompts for game ID, mode, wager and matchup ID.",
        category = GamesMinigames, surface = Flow,
        keywords = listOf("minigame", "arcade", "wager", "practice"),
    ) { miniGames.showLaunchMiniGameFlowDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "api.start_minigame",
        title = "Start MiniGame (Headless)",
        description = "Headless start of a minigame session. Returns the iframe URL for the " +
            "partner-owned WebView.",
        category = GamesMinigames, surface = Api,
        keywords = listOf(HEADLESS, "minigame", "iframe", "webview"),
    ) { miniGames.showStartMiniGameApiDialog() },

    SampleEntry(
        id = "api.preload_geo_token",
        title = "Preload Geo Token",
        description = "Pre-fetch a GeoComply token for cash modes. Fire-and-forget; call it early " +
            "so the token is cached before a cash minigame starts.",
        category = GamesMinigames, surface = Api,
        keywords = listOf(HEADLESS, "geocomply", "location", "cash"),
    ) { miniGames.preloadGeoToken() },

    SampleEntry(
        id = "api.get_minigames_list",
        title = "Get MiniGames list",
        description = "Headless fetch of the minigames enabled for the current tenant, each with " +
            "its config options.",
        category = GamesMinigames, surface = Api,
        keywords = listOf(HEADLESS, "minigame", "tenant", "config"),
    ) { miniGames.showGetMiniGamesApiDialog() },

    SampleEntry(
        id = "flow.tournaments",
        title = "Tournaments",
        description = "Navigate to the Tournaments screen.",
        category = Tournaments, surface = Flow,
        keywords = listOf("bracket", "leaderboard"),
    ) { launchFlow(LucraFlow.Tournaments) },

    SampleEntry(
        id = "flow.show_tournament_matchup",
        title = "Show Tournament Matchup",
        description = "Navigate to the tournament details flow for a tournament ID.",
        category = Tournaments, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "details", "bracket"),
    ) { flows.showTournamentDetailsDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "api.retrieve_tournament",
        title = "Retrieve Tournament",
        description = "Fetch a tournament by ID and print the result.",
        category = Tournaments, surface = Api,
        keywords = listOf(HEADLESS, "fetch"),
    ) { tournaments.showRetrieveTournamentDialog() },

    SampleEntry(
        id = "api.retrieve_tournament_details_light",
        title = "Retrieve Tournament Details (light)",
        description = "Fetch the lightweight ui_tournament_details payload and print the full result.",
        category = Tournaments, surface = Api,
        keywords = listOf(HEADLESS, "fetch", "light", "ui_tournament_details"),
    ) { tournaments.showRetrieveTournamentDetailsDialog() },

    SampleEntry(
        id = "api.join_tournament",
        title = "Join Tournament",
        description = "Free tournaments launch the demographic form if email or zip is missing. " +
            "Paid tournaments launch verification if the user is not verified.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "enter", "entry fee"),
    ) { tournaments.showJoinTournamentDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "api.submit_tournament_score",
        title = "Submit tournament score manually",
        description = "Submit a score for a specified tournament. Tournament ID is required.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "score", "result"),
    ) { tournaments.showSubmitScoreDialog { flow -> launchFlow(flow) } },

    SampleEntry(
        id = "api.submit_score_by_metadata",
        title = "Submit Score by Metadata",
        description = "Submit a tournament score by matching matchup details, game ID or location ID.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "score", "metadata"),
    ) { tournaments.showSubmitUserScoreByMetadataDialog() },

    SampleEntry(
        id = "api.recommended_tournaments",
        title = "Recommended Tournaments",
        description = "Retrieve 20 recommended tournaments.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "suggested", "discovery"),
    ) { tournaments.showRecommendedTournamentsDialog() },

    SampleEntry(
        id = "api.recommended_tournaments_light",
        title = "Recommended Tournaments Light",
        description = "Retrieve lightweight recommended tournament data.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "suggested", "discovery", "light"),
    ) { tournaments.showRecommendedTournamentsLightDialog() },

    SampleEntry(
        id = "api.auto_join_tournaments",
        title = "Auto-Join Tournaments",
        description = "Manually trigger auto-join for all eligible free tournaments.",
        category = Tournaments, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "autojoin", "free"),
    ) { tournamentApi.autoJoinTournaments() },

    SampleEntry(
        id = "flow.achievements",
        title = "Achievements",
        description = "View and claim achievement rewards.",
        category = RewardsAchievements, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "badges", "claim"),
    ) { launchFlow(LucraFlow.Achievements) },

    SampleEntry(
        id = "flow.claim_prize_sheet",
        title = "Claim Prize Sheet",
        description = "The reward sheet over the user's live unclaimed tournament, achievement and " +
            "promotion rewards. Opening it marks them viewed; with nothing new it closes at once.",
        category = RewardsAchievements, surface = Flow, auth = HostBlocks,
        keywords = listOf(AUTH, "prize", "claim", "carousel", "promotion", "bonus", "cash"),
    ) { showRewardSheet() },

    SampleEntry(
        id = "flow.minigames_rewards",
        title = "Minigames Rewards",
        description = "The minigames-flavored rewards list with claimable achievement rewards by game.",
        category = RewardsAchievements, surface = Flow, auth = SdkPrompts,
        keywords = listOf(AUTH, "minigame", "arcade", "claim"),
    ) { launchFlow(LucraFlow.MinigamesRewards) },

    SampleEntry(
        id = "api.create_recreational_game",
        title = "Create Recreational Game",
        description = "Create a recreational game with specified parameters.",
        category = RecreationalGames, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "rec", "group"),
    ) { recreational.showCreateGameDialog() },

    SampleEntry(
        id = "api.accept_versus_recreational_game",
        title = "Accept Versus Recreational Game",
        description = "Accept a Group vs Group recreational game.",
        category = RecreationalGames, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "rec", "versus", "group"),
    ) { recreational.showAcceptVersusGameDialog() },

    SampleEntry(
        id = "api.accept_free_for_all_recreational_game",
        title = "Accept Free-For-All Recreational Game",
        description = "Accept a Free-For-All recreational game.",
        category = RecreationalGames, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "rec", "ffa", "free for all"),
    ) { recreational.showAcceptFreeForAllGameDialog() },

    SampleEntry(
        id = "api.cancel_recreational_game",
        title = "Cancel Recreational Game",
        description = "Cancel a recreational game.",
        category = RecreationalGames, surface = Api, auth = HostBlocks,
        keywords = listOf(HEADLESS, AUTH, "rec"),
    ) { recreational.showCancelGameDialog() },

    // No onSelect: Ui entries are dispatched by the list, which expands them in place.

    SampleEntry(
        id = "ui.profile_pill",
        title = "Profile Pill",
        description = "The profile pill with the user balance. Renders signed out; tapping it " +
            "launches the auth flow.",
        category = EmbeddedComponents, surface = Ui,
        keywords = listOf("component", "embed", "balance", "pill"),
        embedded = { actions, onReady ->
            onReady(actions.lucraComponent(LucraComponent.ProfilePill { actions.launchFlow(it) }))
        },
    ),

    SampleEntry(
        id = "ui.recommended_matchups_banner",
        title = "Recommended Matchups Banner",
        description = "The recommended matchups banner component.",
        category = EmbeddedComponents, surface = Ui, auth = SdkPrompts,
        keywords = listOf("component", "embed", AUTH, "banner", "suggested"),
        embedded = { actions, onReady ->
            onReady(
                actions.lucraComponent(LucraComponent.RecommendedMatchups { actions.launchFlow(it) })
            )
        },
    ),

    SampleEntry(
        id = "ui.floating_action_button",
        title = "Floating Action Button",
        description = "The floating action button that creates a sports contest.",
        category = EmbeddedComponents, surface = Ui,
        keywords = listOf("component", "embed", "fab", "button"),
        embedded = { actions, onReady ->
            onReady(
                actions.lucraComponent(LucraComponent.FloatingActionButton { actions.launchFlow(it) })
            )
        },
    ),

    SampleEntry(
        id = "ui.mini_public_feed",
        title = "Mini Public Feed",
        description = "Contest cards in a non-scrolling list. Prompts for two player IDs, though " +
            "the component accepts an array.",
        category = EmbeddedComponents, surface = Ui,
        keywords = listOf("component", "embed", "feed", "cards", "players"),
        embedded = { actions, onReady ->
            actions.components.showMiniPublicFeedDialog { playerOneId, playerTwoId ->
                onReady(
                    actions.lucraComponent(
                        LucraComponent.MiniPublicFeed(listOf(playerOneId, playerTwoId)) {
                            actions.launchFlow(it)
                        }
                    )
                )
            }
        },
    ),

    SampleEntry(
        id = "ui.contest_card",
        title = "Contest Card",
        description = "The contest card for a specific contest ID.",
        category = EmbeddedComponents, surface = Ui,
        keywords = listOf("component", "embed", "card", "contest"),
        embedded = { actions, onReady ->
            actions.components.showContestCardDialog { contestId ->
                onReady(
                    actions.lucraComponent(
                        LucraComponent.ContestCard(contestId = contestId) { actions.launchFlow(it) }
                    )
                )
            }
        },
    ),

    SampleEntry(
        id = "api.push_debug",
        title = "Push Debug",
        description = "Pick a mock push: tournament, achievement or funds. The banner fires after " +
            "2s; tap it to validate deeplink handling.",
        category = DebugTools, surface = Api, debugOnly = true,
        keywords = listOf(HEADLESS, "notification", "fcm", "deeplink"),
    ) { debug.showPushDebugPicker() },
)
