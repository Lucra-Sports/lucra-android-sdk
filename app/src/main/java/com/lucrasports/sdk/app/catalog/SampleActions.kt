package com.lucrasports.sdk.app.catalog

import android.view.View
import com.lucrasports.sdk.app.headless_api.HandshakeAuthApiHandler
import com.lucrasports.sdk.app.headless_api.MatchupApiHandler
import com.lucrasports.sdk.app.headless_api.PhoneAuthApiHandler
import com.lucrasports.sdk.app.headless_api.TournamentApiHandler
import com.lucrasports.sdk.app.headless_api.UserApiHandler
import com.lucrasports.sdk.app.ui.dialogs.ComponentDialogs
import com.lucrasports.sdk.app.ui.dialogs.DebugDialogs
import com.lucrasports.sdk.app.ui.dialogs.FlowDialogs
import com.lucrasports.sdk.app.ui.dialogs.MiniGameDialogs
import com.lucrasports.sdk.app.ui.dialogs.RecreationalGameDialogs
import com.lucrasports.sdk.app.ui.dialogs.TournamentDialogs
import com.lucrasports.sdk.app.ui.dialogs.UserDialogs
import com.lucrasports.sdk.core.ui.LucraUiProvider
import com.lucrasports.sdk.core.user.SDKUser

/** Helpers are exposed as properties so each is reused unchanged. */
internal interface SampleActions {

    val flows: FlowDialogs
    val tournaments: TournamentDialogs
    val users: UserDialogs
    val recreational: RecreationalGameDialogs
    val components: ComponentDialogs
    val miniGames: MiniGameDialogs
    val debug: DebugDialogs

    val matchupApi: MatchupApiHandler
    val tournamentApi: TournamentApiHandler
    val phoneAuthApi: PhoneAuthApiHandler
    val handshakeAuthApi: HandshakeAuthApiHandler
    val userApi: UserApiHandler

    /** Latest value from `observeSDKUserFlow()`; null while signed out. */
    val currentUser: SDKUser?

    fun onUserUpdated(user: SDKUser?)

    fun launchFlow(flow: LucraUiProvider.LucraFlow)

    fun showRewardSheet()

    fun lucraComponent(component: LucraUiProvider.LucraComponent): View
}
