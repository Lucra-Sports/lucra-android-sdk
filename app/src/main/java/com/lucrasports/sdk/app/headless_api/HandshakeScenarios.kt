package com.lucrasports.sdk.app.headless_api

/**
 * The paths worth walking through this screen, written down.
 *
 * Handshake auth has a small number of behaviours that are easy to get wrong by testing
 * the wrong thing: the ToS gate only fires for users who do not exist yet, an existing
 * user is resolved by PHONE and not by the partner's `sub`, and a failed handshake falls
 * back to phone auth silently. Someone poking at the form without knowing that will
 * report "the ToS prompt never appears" after five runs as a seeded user, every one of
 * which already accepted.
 *
 * Plain data with no Android dependency so the copy can be read — and asserted on —
 * without an emulator.
 */
internal data class Scenario(
    val title: String,
    val summary: String,
    val steps: List<String>,
    val expected: String,
    /** The traps specific to this path — mostly "why it looks broken when it isn't". */
    val watchOut: List<String> = emptyList(),
) {

    /** Rendered for the detail dialog: numbered steps, then what should happen. */
    fun body(): String = buildString {
        append(summary)
        append("\n\n")
        steps.forEachIndexed { index, step -> append("${index + 1}. $step\n") }
        append("\nEXPECT\n")
        append(expected)
        if (watchOut.isNotEmpty()) {
            append("\n\nWATCH OUT\n")
            append(watchOut.joinToString("\n") { "• $it" })
        }
    }
}

internal object HandshakeScenarios {

    val all = listOf(
        Scenario(
            title = "Normal handshake sign-in",
            summary = "The happy path: a partner-signed token becomes a Lucra session with " +
                    "no Lucra login UI at all.",
            steps = listOf(
                "Open Provider. It should read Registered — the screen registers on your " +
                        "first visit. If it says Not registered, tap Register.",
                "Open Token and pick a seeded Partner user, e.g. Test User 1.",
                "Leave Break it on None.",
                "Tap Sign in with handshake auth.",
            ),
            expected = "The status dot goes orange while the exchange runs, then green: " +
                    "Signed in as that user. The log reads token provider called → token " +
                    "minted locally → exchange ok → profile loaded.",
            watchOut = listOf(
                "Open Lucra profile afterwards to confirm the session is real and not just " +
                        "a green dot.",
                "Last token shows the claims that were actually sent — the fastest way to " +
                        "check the token matches the user you picked.",
            ),
        ),

        Scenario(
            title = "New user → Terms of Service prompt",
            summary = "A user Lucra has never seen has to accept the Terms of Service " +
                    "before an account is created. This is the one handshake failure that " +
                    "is NOT a broken integration, and the one that must not fall back to " +
                    "phone auth.",
            steps = listOf(
                "Open Token and set Partner user to Custom.",
                "Type a phone_number in E.164 (+1…) that no Lucra user already has. Any " +
                        "unused number works — bump the last digits each run.",
                "Leave sub empty. It is derived from the phone, so a new number is a new " +
                        "identity; a fixed sub would pin every run to one account.",
                "Check Provider: TOS bypass must be UNTICKED for this path.",
                "Tap Sign in with handshake auth.",
            ),
            expected = "The exchange succeeds in every way except the agreement: Last " +
                    "failure reads TosNotAccepted, and this screen presents the Terms of " +
                    "Service flow off that error — exactly as an integrator would. Accept " +
                    "in the flow and it resubmits the sign-in itself; the status goes green " +
                    "on a brand-new account.",
            watchOut = listOf(
                "Run it twice with the same phone and the prompt will NOT appear the second " +
                        "time. That user now exists. Change the number for every run.",
                "Seeded test users can never reach this — they already exist and already " +
                        "accepted. Custom with an unused phone is the only way in.",
                "A phone that clashes with an existing Lucra user links to that account " +
                        "instead of creating one, and you will land signed in with no prompt.",
            ),
        ),

        Scenario(
            title = "Existing phone-auth user → same account",
            summary = "Someone who signed up through phone + 2FA must land on THAT account " +
                    "when they come back through handshake auth, not a second one.",
            steps = listOf(
                "Open Provider and tap Clear, so Lucra flows use phone auth again.",
                "Go back to the sample menu and use Phone Authentication to sign in with a " +
                        "phone number and its 2FA code.",
                "Note the username or user id the Status row shows, and the phone you used.",
                "Return here, tap Sign out, then Provider → Register.",
                "Open Token, set Partner user to Custom, and put that SAME phone number in " +
                        "phone_number. Leave sub empty.",
                "Tap Sign in with handshake auth.",
            ),
            expected = "Status shows the SAME username / user id as the phone-auth session, " +
                    "with no ToS prompt — the user already exists and already accepted. A " +
                    "new sub carrying a known phone links to that account rather than " +
                    "creating another one.",
            watchOut = listOf(
                "Compare the id, not the display name. Two accounts can show the same name.",
                "Once a sub has been used it IS the identity: later runs resolve it directly " +
                        "and the phone in the token is ignored.",
                "Open Lucra profile on both legs and check the balance and history match.",
            ),
        ),

        Scenario(
            title = "Sign out, then back in",
            summary = "Signing out of Lucra does not mean the next tap shows a login screen " +
                    "— which surprises people who expect a session to stay dead.",
            steps = listOf(
                "Sign in first, so the status is green.",
                "Tap Sign out. The status returns to Idle — provider ready.",
                "Now tap Present Terms of Service flow, or Open Lucra profile, or go back " +
                        "and tap any Lucra component in the sample.",
            ),
            expected = "You are signed straight back in through handshake auth, with no " +
                    "prompt. Sign-out only keeps BACKGROUND traffic out; any user action " +
                    "counts as asking, and the provider is still registered.",
            watchOut = listOf(
                "To actually stay signed out, use Provider → Clear. Flows then fall back to " +
                        "phone auth.",
                "The log records the re-entry even though nothing on this screen was tapped.",
            ),
        ),

        Scenario(
            title = "Sign in without this screen",
            summary = "The path a real integration actually takes: the provider is " +
                    "registered at app start and the SDK runs the handshake on its own the " +
                    "first time a flow needs a session.",
            steps = listOf(
                "Make sure Provider reads Registered, then tap Sign out.",
                "Leave this screen and return to the sample's main menu.",
                "Tap any Lucra component or flow — Wallet, Home, a matchup.",
                "Come back to this screen and open the Log.",
            ),
            expected = "The handshake runs in place behind a loader and you land where you " +
                    "tapped — no phone screen, no visit to this screen. The whole run is in " +
                    "the log afterwards, because the log is process-scoped rather than " +
                    "owned by this Activity.",
            watchOut = listOf(
                "This is the run most worth reading: it is the one an integrator's users " +
                        "will actually take.",
                "Kill and relaunch the app and it still works — the provider re-registers at " +
                        "app start when one was registered before.",
            ),
        ),

        Scenario(
            title = "Failure → fallback to phone auth",
            summary = "Every handshake failure except TosNotAccepted falls back to phone " +
                    "auth silently. That is correct behaviour, and indistinguishable from a " +
                    "broken integration unless you read the reason.",
            steps = listOf(
                "Open Token and choose something under Break it. The line underneath says " +
                        "what that injection should produce.",
                "Tap Sign out, leave the screen, and tap a Lucra component — that shows the " +
                        "real fallback. Signing in from this screen shows the error but not " +
                        "the fallback.",
                "Come back and read Last failure.",
            ),
            expected = "The phone screen appears instead of a session, and Last failure " +
                    "names the type — ProviderTimedOut, ProviderFailed, ExchangeFailed — " +
                    "with what to check underneath.",
            watchOut = listOf(
                "Slow provider (3s) is NOT a failure: it returns inside the SDK's 5s " +
                        "deadline and still signs in. Provider timeout (6s) is the one that " +
                        "trips ProviderTimedOut.",
                "Break it persists. Set it back to None or every later scenario fails too.",
                "Last failure clears on the next SUCCESS, not on the next attempt, so it is " +
                        "labelled previous attempt while a retry is in flight.",
            ),
        ),

        Scenario(
            title = "One phone, two partner users",
            summary = "Why the phone number is the thing that matters: a perfectly valid " +
                    "token from a different partner user lands on someone else's account.",
            steps = listOf(
                "Sign in as a seeded user and note the account.",
                "Sign out, then open Token and set Partner user to Hack Joe's Account.",
                "Its sub is unique but its phone belongs to an account that already exists.",
                "Sign in with handshake auth.",
            ),
            expected = "The token is valid and the exchange succeeds — onto the account that " +
                    "owns that PHONE, not a new one. A new sub consults the phone: a known " +
                    "phone links, an unused one creates.",
            watchOut = listOf(
                "This is why the seeded test users all carry distinct numbers. Two users " +
                        "sharing a phone would silently be one Lucra account.",
                "It is also why a partner backend must never let a user assert a phone " +
                        "number it has not verified.",
            ),
        ),

        Scenario(
            title = "TOS bypass (gated)",
            summary = "Some tenants are allowed to create users without showing the Terms " +
                    "of Service. The app REQUESTS it; the backend decides.",
            steps = listOf(
                "Open Provider and tick Request TOS bypass. It re-registers immediately — " +
                        "the bypass is handed over when the provider is stored, not carried " +
                        "by the token.",
                "Open Token, set Partner user to Custom and enter a fresh unused phone " +
                        "number, exactly as in the new-user scenario.",
                "Sign in with handshake auth.",
            ),
            expected = "With the Statsig gate on for this tenant, the new user is created " +
                    "and signed in with no ToS prompt. With the gate off you still get " +
                    "TosNotAccepted — the tick is a request, not a grant.",
            watchOut = listOf(
                "Getting TosNotAccepted here does not mean the bypass is broken. Check the " +
                        "gate for the tenant shown in the Token summary first.",
                "Reuse a phone and nothing is proven: that user exists and would not have " +
                        "been prompted anyway.",
            ),
        ),

        Scenario(
            title = "Local minting vs the partner backend",
            summary = "Where the token comes from. On-device signing is a harness shortcut; " +
                    "the partner backend is the shape a real integration has.",
            steps = listOf(
                "Open Provider → Token source.",
                "Mint on device needs a Signing secret under Token, and only appears on " +
                        "builds where a disposable key is acceptable.",
                "Partner BE calls the handshake POC instead: /login for a session cookie, " +
                        "then /api/lucra/token with it — two legs, no signing key on device.",
                "On an emulator the base URL must be 10.0.2.2, not localhost: localhost is " +
                        "the emulator itself.",
                "Sign in on each and compare the Last token claims.",
            ),
            expected = "Both produce a token with the same identity and both sign in. The " +
                    "log names which side signed it, and the Token summary row says local " +
                    "or partner BE before you run anything.",
            watchOut = listOf(
                "If local minting is rejected for its signature, try the other Secret " +
                        "encoding — the POC signs with the UTF-8 bytes of the hex STRING, " +
                        "not the bytes that string spells.",
                "Choosing Mint on device with no secret silently routes to the partner " +
                        "backend instead, and says so in the log.",
                "A dead backend surfaces as ProviderFailed rather than a timeout: the HTTP " +
                        "timeouts sit inside the SDK's 5s provider deadline on purpose.",
            ),
        ),
    )
}
