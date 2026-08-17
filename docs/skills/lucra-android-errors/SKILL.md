---
name: lucra-android-errors
description: >
  Diagnose runtime errors from the Lucra Android SDK — any LucraClient call that returns a
  Failure, a sealed error result (UserStateError, LocationError, APIError), a flow that
  misbehaves after successful setup, or a mini game session that dies. Maps each structured
  error case to its remediation, and each opaque error message to its real underlying cause.
  For build-time or first-launch failures use lucra-android-start instead.
---

# Lucra Android — Runtime Error Diagnosis

> **Portability:** all relative links in this skill (e.g. `../../1.2.0_initialize_client.md`)
> resolve against the `docs/` folder of the public repo
> `https://github.com/Lucra-Sports/lucra-android-sdk`. If this file is not sitting inside that
> docs folder (e.g. it was installed as a standalone agent skill), clone the public repo (shallow
> is fine) and resolve links there. Use only these published docs — never private Lucra checkouts
> that may exist elsewhere on the machine.

**Scope:** failures at runtime, *after* the `lucra-android-start` "initialized successfully" bar
is met. If the app doesn't build, doesn't launch, or never produced a signed-in user, stop and go
to `lucra-android-start` → Troubleshoot instead. Message fingerprints below verified against SDK
`6.8.0`.

## How SDK errors surface

Every headless `LucraClient()` call returns a sealed result with a `Failure` case. Most wrap
`LucraError`, which has exactly three shapes:

- **`UserStateError`** — structured sealed cases about the *user's* state. Match on the case;
  each has one correct remediation (table below).
- **`LocationError(message)`** — a geolocation/GeoComply problem, flattened to a message string.
- **`APIError(message)`** — everything else (auth, backend, network), flattened to a message string.

A few older calls (`getMatchup`, phone auth) use their own failure types
(`FailedRetrieveMatchup.LocationError/APIError`, `PhoneAuthError.*`) — the same diagnosis applies.
`SDKUserResult.Error` carries only a raw `Throwable`; diagnose from its message the same way.

**Diagnostic procedure:** capture the exact sealed case and full message text first. Then:
structured case → first table; opaque message → fingerprint table. Never guess from the
user-facing wording alone — several friendly messages share one cause and vice versa.

## Structured cases → remediation

| Case | Actual meaning | What to do |
|---|---|---|
| `UserStateError.NotInitialized` | Client not done initializing, or no signed-in user for a user-scoped call | Gate on `waitForLucraClient()`; confirm `observeSDKUserFlow()` emits `Success`. → [Init](../../1.2.0_initialize_client.md) |
| `UserStateError.Unverified` | User has not completed KYC and the action requires it | Present `LucraFlow.VerifyIdentity`. Not an error to retry. |
| `UserStateError.NotAllowed` | User is blocked/ineligible per backend rules | Terminal client-side. Show messaging; do not retry. |
| `UserStateError.InsufficientFunds` | Wallet balance below the attempted stake | Present `LucraFlow.AddFunds` (host Activity must be a `FragmentActivity` — see start skill). |
| `UserStateError.DemographicInformationMissing` | Tenant defers KYC but required demographic fields are missing | Present `LucraFlow.DemographicForm` or collect headlessly. → [Headless Interactions](../../1.2.9_headless_interactions.md) |
| `PhoneAuthError.InvalidPhoneNumber` / `InvalidCode` / `PhoneNumberNotSubmitted` | User input problem in headless phone auth | Re-prompt the user; each case names the field to fix. |
| `PhoneAuthError.AlreadyLoggedIn` | A user session already exists | Not a failure — proceed, or log out first if switching users. |
| `PhoneAuthError.NetworkError(message)` | Connectivity during auth | Retryable. |

## Opaque message fingerprints (`APIError` / `LocationError`)

The SDK internally distinguishes far more causes than the public error types express — HTTP
status, GraphQL error codes, GeoComply subtypes are all flattened into these message strings
before you see them. This table restores the mapping. **Match messages for diagnosis only; in
production partner code, branch on the sealed types, never on message text** (wording can change
between SDK/backend releases).

| Message (exact or prefix) | Real cause | Retryable? | Fix |
|---|---|---|---|
| `Unauthorized Access` | HTTP 401/403 or GraphQL `UNAUTHORIZED`/`FORBIDDEN` — API key invalid **or key/environment mismatch** (e.g. sandbox key with production env) | No | Fix key/environment pairing. → [Init](../../1.2.0_initialize_client.md) |
| `Sorry, we're having issues. Please try again.` | Backend HTTP 500/503 | Yes | Retry with backoff; if persistent, Lucra-side outage. |
| `Hey! We're having some issues right now, please try back soon.` | Generic fallback — an unrecognized client-side exception or unmapped GraphQL error | Maybe | Check Logcat for the underlying exception; often network or serialization. |
| `This feature does not operate outside of the USA.` (+ VPN text) | CloudFront geo-block at the CDN edge — device egress IP outside the US, commonly a VPN | No (until location changes) | Disable VPN / confirm US egress. Not a GeoComply issue — this blocks before the API is reached. |
| `Location permissions aren't granted!` | Runtime location permission missing (or GeoComply lacks network) | After user action | Request `ACCESS_FINE_LOCATION` at runtime; manifest entry alone is not enough. |
| `Location services are disabled on this device. Please turn them on and try again.` | Device-level location toggle off | After user action | Send user to location settings. |
| `Error while retrieving location. Please try again.` | Transient GeoComply location-fix failure | Yes | Retry; ensure device has GPS/network fix (emulators: set a mock location). |
| `GeoComply invalid license` | Tenant's GeoComply license misconfigured or expired | No | Contact Lucra support — nothing to fix in the integration. |
| `Request already in progress` | A second geolocation request started before the first finished | Yes (after first completes) | Serialize calls that trigger geo checks; don't fire `startMiniGame`-style calls concurrently. |
| `An unexpected error has occurred. Please try again, or contact Customer Support…` | GeoComply internal error | Yes | Retry once; then treat as Lucra-side. |
| Multi-line list of geo reasons (varies) | Backend `GEOCOMPLY_RULE_ERROR` — user positively located somewhere the action is prohibited; message lists the actionable reasons | No (until location changes) | Show the reasons verbatim — they are written to be user-actionable. |
| `Failed to start minigame session.` | `startMiniGame` failed with no server message at all | Maybe | Check connectivity and Logcat; escalate with `sessionId`/timestamps if reproducible. |

Two auth-shaped causes are easy to conflate: `Unauthorized Access` is your *integration's*
credentials (key/env — fix config), while `UserStateError.NotInitialized`/`NotLoggedIn` is the
*user's* session (re-run login). Check which one you actually have before "fixing" the API key.

## Mini game sessions: where error reporting ends

- **Before web content loads** — `startMiniGame` / `LucraFlow.MiniGame` failures are structured
  (`StartMiniGameResult.Failure(LucraError)`); diagnose with the tables above.
- **After the web content is running** — the SDK emits **no error signal**. There is no failure
  variant of `LucraEvent.MiniGame` (only `Finished`), and no error callback. A blank or dead
  game screen mid-session is a WebView-layer problem the host must observe itself: if you host
  the `iframeUrl` in your own WebView, wire `WebViewClient.onReceivedError`/HTTP error callbacks;
  a stale geo token on a long-delayed launch also lands here (pre-warm with `preloadGeoToken`).
  → [Mini Games Headless](../../6.1_mini_games_headless.md), [Events](../../6.2_mini_game_events.md)

## When it's not an error at all

A flow that presents nothing (no screen, no error) or a list that is empty-but-successful is
usually **not** a runtime error: no-op `lucraUiProvider`, feature not enabled for the tenant, or
a user with no history. That class of symptom is skill `lucra-android-provisioning`'s territory —
go there before hunting a phantom exception (setup-time cases also appear in
`lucra-android-start` → Troubleshoot).

## Escalation bar

Escalate to Lucra support (with the exact sealed case, full message, timestamp, environment, and
`sessionId`/`matchupId` where relevant) when: `GeoComply invalid license`; persistent 500/503-class
failures; `GEOCOMPLY_RULE_ERROR` reasons that look wrong for the user's actual location; or any
`Unauthorized Access` that survives a verified-correct key/environment pairing.
