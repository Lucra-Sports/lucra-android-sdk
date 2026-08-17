# Integrating the Lucra Android SDK — Start Here (LLM & Engineer Guide)

This is the connective map for integrating the Lucra Android SDK. It holds **no API reference** of
its own — it explains how the pieces fit together and points you to the exact doc page for each
detail.

**If you are an LLM/agent:** read this page top to bottom first. Then load only the section relevant
to the user's stated goal, and follow its pointers. Five companion skills go deeper:
`skills/lucra-android-start` (setup + troubleshooting), `skills/lucra-android-minigames`,
`skills/lucra-android-errors` (diagnosing runtime `Failure` results after setup),
`skills/lucra-android-provisioning` (features that render empty or no-op with no error —
tenant provisioning gaps, plus the pre-flight checklist before feature work), and
`skills/lucra-android-events` (observing change: event listener vs re-fetch vs webhooks,
event caveats, matchup status glossary).

## The one mental model

The SDK is a self-contained flow engine. You do **not** build auth, screens, or game logic. You:

1. **Add the SDK** from Maven Central — `com.lucrasports.sdk:sdk-core` (headless APIs) and
   `com.lucrasports.sdk:sdk-ui` (screens; transitively includes `sdk-core`) — pin `6.8.0` or later. Your app must also meet
   a short list of host requirements (manifest entries, `FragmentActivity`, image loader).
2. **Configure one client** — `LucraClient.initialize(...)` with your API key + environment, passing
   a `LucraUi` instance as the `lucraUiProvider` if you want any SDK screens.
3. **Delegate the user session** to the SDK — present its `Login` flow (or use headless phone auth)
   and observe `observeSDKUserFlow()`; you never build a login screen.
4. Then either **present a full SDK flow** (`LucraUiProvider.LucraFlow.*`) for UI, or call a
   **headless function** (`LucraClient().*`) to pull data and render it yourself.

Everything else is deciding *which* flow or *which* headless call, and reacting to the sealed result
types and Flows the SDK hands back to you.

## Route by intent

| Goal | Go to |
|---|---|
| Add the SDK / first run / it won't build | [Project Setup](1.0.0_project_setup.md) → skill `lucra-android-start` |
| Initialize the client / API keys / environment | [LucraClient Initialization](1.2.0_initialize_client.md) |
| Know if a user is signed in / delegate auth | [Headless Interactions → Observe the SDK User](1.2.9_headless_interactions.md) |
| Present any SDK screen | [Lucra Flows](1.2.7_lucraflows.md) |
| Mini Games (UI or headless) | skill `lucra-android-minigames` + [Flows](6.3_mini_games_flows.md) / [Headless](6.1_mini_games_headless.md) |
| A user's recent matchups for a custom screen | [Headless Interactions → User Matchups](1.2.9_headless_interactions.md) |
| Tournaments UI (tournaments home) | `LucraFlow.HomePage(locationId:)` — see [Tournaments Flows](3.1_tournaments_flows.md) |
| Handle deep links (shared / matchup-invite links) | [Deep Links](1.2.2_deeplinks.md) |
| React to game/tournament state / wire the event listener | skill `lucra-android-events` → [Event Listener](1.2.10_lucra_event_listener.md), [Mini Game Events](6.2_mini_game_events.md) |
| A `LucraClient` call returned `Failure` / runtime errors after setup | skill `lucra-android-errors` |
| A feature renders empty / an option is missing, with no error | skill `lucra-android-provisioning` |
| Confirm the tenant is provisioned before building a feature | skill `lucra-android-provisioning` |

## How the parts connect (the things docs cover in isolation)

- **Install is two artifacts plus a host contract.** `sdk-core` alone is headless-only; add `sdk-ui`
  for any SDK screen. The host app must satisfy the setup contract — manifest permissions +
  GeoComply entries, Auth0 manifest placeholders (even if you don't use Auth0), a
  `LucraCoilImageLoader`-backed `Application`, and a **`FragmentActivity`** host (a plain
  `ComponentActivity` silently fails to show the device-security prompt for funds flows).
  → [Project Setup](1.0.0_project_setup.md)
- **Flows need a real `LucraUiProvider`.** `initialize`'s `lucraUiProvider` defaults to a no-op —
  headless calls work, but every flow launch does nothing. Pass `LucraUi(lucraFlowListener = ...)`
  from `sdk-ui`. → [Initialization](1.2.0_initialize_client.md)
- **Auth is delegated.** Present `LucraFlow.Login` (it dismisses immediately if already signed in),
  or use the headless phone-auth pair. You learn the result by collecting `observeSDKUserFlow()` —
  there is no `isSignedIn` boolean; you subscribe, you don't poll. `configure(SDKUser)` is a
  *different* path (pushing your user's profile attributes into Lucra) — don't confuse the two.
  → [Headless Interactions](1.2.9_headless_interactions.md)
- **Headless calls must wait for init.** They fail if invoked before initialization completes — gate
  them with `LucraClient.waitForLucraClient()`. Flows handle this themselves by showing a loading
  state. → [Initialization → asynchronously](1.2.0_initialize_client.md)
- **Flows vs Headless.** A `LucraFlow` is a full SDK screen you present (as a Fragment, full-screen
  dialog, or Composable) and observe; a headless call returns data you render yourself. Pick the
  lightest one that meets the goal, and mix them: headless powers *your* lists, flows power the
  *interactive* parts. → [Flows](1.2.7_lucraflows.md) / [Headless](1.2.9_headless_interactions.md)
- **State comes back as sealed result types** (exhaustive `when`) through callbacks and Kotlin
  Flows — not as return values from the flow you presented. Wire your UI to those, plus the
  `LucraEventListener` for session/tournament lifecycle events.

## Fastest path to a working integration

1. Add both SDK artifacts and get an empty app building against them (host contract included).
2. Initialize `LucraClient`, present `LucraFlow.Login`, and confirm `observeSDKUserFlow()` emits
   `SDKUserResult.Success`.
3. Only then add features. The public sample is a legitimate starting point:
   [lucra-android-sdk sample app](https://github.com/Lucra-Sports/lucra-android-sdk/tree/develop/app).

## When you're stuck

Go to `skills/lucra-android-start` → Troubleshooting. It maps each common failure (bad key, wrong
environment, no network, flows that show nothing, missing device-security prompt, user never signs
in) to the thing to check and the doc page to reopen.
