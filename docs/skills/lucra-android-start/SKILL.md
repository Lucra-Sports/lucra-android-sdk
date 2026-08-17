---
name: lucra-android-start
description: >
  Entry point for integrating the Lucra Android SDK. Use at the very start of any Lucra Android
  integration, when SDK setup/initialization fails, or when unsure which Lucra flow or API to use.
  Routes by intent, verifies initialization, and troubleshoots common setup failures. Points to the
  official Lucra docs for all technical detail; contains no API reference of its own.
---

# Lucra Android — Starting Point

> **Portability:** all relative links in this skill (e.g. `../../1.2.0_initialize_client.md`)
> resolve against the `docs/` folder of the public repo
> `https://github.com/Lucra-Sports/lucra-android-sdk`. If this file is not sitting inside that
> docs folder (e.g. it was installed as a standalone agent skill), clone the public repo (shallow
> is fine) and resolve links there. Use only these published docs — never private Lucra checkouts
> that may exist elsewhere on the machine.

## Step 0 — State the goal, then route

Establish what the integrator is trying to do, then jump to the matching guide. Don't start writing
integration code until the goal maps to one of these:

| Goal | Go to |
|---|---|
| First-time setup / build failing | this skill: "Install", then "Troubleshoot" |
| Show Mini Games, or fetch Mini Games data | skill `lucra-android-minigames` |
| A user's recent matchups on a custom screen | [Headless Interactions → User Matchups](../../1.2.9_headless_interactions.md) |
| Tournaments UI (tournaments home) | `LucraFlow.HomePage(locationId:)` — see [Tournaments Flows](../../3.1_tournaments_flows.md) |
| Auth / user linking / demographics | [Init](../../1.2.0_initialize_client.md), [Headless Interactions](../../1.2.9_headless_interactions.md) |

## Install (do this first, always)

Add the SDK from Maven Central — pin `6.8.0` or later (verified to include the Mini Games flows
and catalog APIs used by these skills):

- `implementation("com.lucrasports.sdk:sdk-core:<version>")` — headless APIs only.
- `implementation("com.lucrasports.sdk:sdk-ui:<version>")` — SDK screens; transitively includes
  `sdk-core`. You need this for any `LucraFlow`.
- Repositories: `google()`, `mavenCentral()`, and `https://jitpack.io`.

Then satisfy the **host contract** — each item fails at a different, non-obvious time if skipped:

- **Auth0 manifest placeholders** (`auth0Domain`/`auth0Scheme`) — required even if your app does not
  use Auth0; without them the manifest merge fails.
- **Manifest permissions + GeoComply receivers/services** — copy the block from the setup doc.
- **`Application` implements `ImageLoaderFactory`** returning `LucraCoilImageLoader.get(this)` —
  without it SDK images/SVGs fail to render.
- **Any Activity hosting a Lucra flow must extend `FragmentActivity`** (or `AppCompatActivity`). A
  pure-Compose `ComponentActivity` compiles and runs, but silently fails to present the
  device-security prompt in Add/Withdraw Funds.
- **Publishing:** disable Google Play's Automatic Integrity Protection — it conflicts with the
  GeoComply runtime protection and crashes the app.

Full detail (exact gradle/manifest blocks, proguard rules): [Project Setup](../../1.0.0_project_setup.md).

## Initialize

1. Call `LucraClient.initialize(...)` once, with `apiKey` + `environment` (use
   `Environment.SANDBOX` for integration). **The key must match the environment it was issued
   for** — a mismatched pair fails with unauthorized errors, not a helpful message.
2. Pass `lucraUiProvider = LucraUi(lucraFlowListener = ...)` from `sdk-ui`. The parameter defaults
   to a **no-op provider**: headless calls work, but launching any flow silently does nothing.
3. Delegate auth: present `LucraFlow.Login` (or use headless phone auth). Do not build a login
   screen. If the user is already signed in, `Login` dismisses immediately — that is expected.
4. Gate headless calls behind `LucraClient.waitForLucraClient()` — they fail if invoked before
   initialization completes.

Detail + exact initializer: [LucraClient Initialization](../../1.2.0_initialize_client.md). If your
app uses Lucra deep links, [Deep Links](../../1.2.2_deeplinks.md) covers the full setup.

### Initialization checkpoints — verify BEFORE building features

- [ ] The app builds and launches with both SDK artifacts and the host contract in place.
- [ ] `LucraClient.waitForLucraClient()` completes (SDK constructed + initialized).
- [ ] The `environment` matches the one your API key was issued for.
- [ ] After presenting `LucraFlow.Login` and completing it, `observeSDKUserFlow()` emits
      `SDKUserResult.Success` with a populated `sdkUser`.

If any box fails, go to Troubleshoot before writing more code.

## Troubleshoot (symptom → check → doc)

| Symptom | First thing to check | Reopen |
|---|---|---|
| Gradle can't resolve `com.lucrasports.sdk:*` | `mavenCentral()` and `https://jitpack.io` present in repositories; version exists on Maven Central | [Project Setup](../../1.0.0_project_setup.md) |
| Kotlin compile fails: "class ... was compiled with an incompatible version of Kotlin. The binary version of its metadata is X.Y.Z" | App's Kotlin plugin is older than the SDK's toolchain — align your Kotlin + Compose plugin version with the SDK artifact's kotlin-stdlib dependency | [Project Setup](../../1.0.0_project_setup.md) |
| Manifest merger failure mentioning `auth0Domain`/`auth0Scheme` | Auth0 manifest placeholders missing from `defaultConfig` | [Project Setup](../../1.0.0_project_setup.md) |
| Launching a flow does nothing (no screen, no error) | `lucraUiProvider` was left at its no-op default — pass `LucraUi(...)`, and confirm `sdk-ui` is a dependency | [Init](../../1.2.0_initialize_client.md) |
| Add/Withdraw Funds never shows the device-security prompt | Host Activity is a `ComponentActivity` — must be `FragmentActivity`/`AppCompatActivity` | [Project Setup](../../1.0.0_project_setup.md) |
| SDK images/SVGs render blank | `Application` doesn't provide `LucraCoilImageLoader` via `ImageLoaderFactory` | [Project Setup](../../1.0.0_project_setup.md) |
| Headless call fails immediately / `NotInitialized` | Called before init finished — gate with `waitForLucraClient`; also requires a signed-in user for user-scoped calls | [Init → asynchronously](../../1.2.0_initialize_client.md) |
| Auth/unauthorized-style failures | Key/environment mismatch (e.g. sandbox key with `Environment.PRODUCTION`) | [Init](../../1.2.0_initialize_client.md) |
| `observeSDKUserFlow()` never emits `Success` | Are you collecting the Flow (not reading once)? Did the `Login` flow actually complete? Network reachable? | [Headless Interactions](../../1.2.9_headless_interactions.md) |
| A Mini Games / Tournaments flow shows nothing or reports unavailable | The feature must be enabled for your tenant on the backend | skill `lucra-android-provisioning` |

This table covers **setup-time** failures. Once initialization has succeeded: runtime `Failure`
results (`UserStateError`, `LocationError`, `APIError`, geo/session errors) are diagnosed in
skill `lucra-android-errors`; features that render empty or no-op with **no** error are tenant
provisioning gaps — skill `lucra-android-provisioning`.

## Definition of "initialized successfully"

Proceed to feature work only once the app launches against both artifacts,
`waitForLucraClient()` completes, and you have observed `observeSDKUserFlow()` emit
`SDKUserResult.Success` after the login flow. State this back to the user before continuing.
