---
name: lucra-android-provisioning
description: >
  Diagnose Lucra Android SDK features that render empty, hide options, or silently do nothing
  with NO error — the signature of a tenant-provisioning gap rather than a bug. Also the
  pre-flight checklist to run before building a feature, so provisioning gaps surface before
  code does. Maps each silent symptom to the capability that gates it and the exact request to
  send Lucra support. For explicit Failure results use lucra-android-errors instead.
---

# Lucra Android — Tenant Provisioning

> **Portability:** all relative links in this skill (e.g. `../../1.2.0_initialize_client.md`)
> resolve against the `docs/` folder of the public repo
> `https://github.com/Lucra-Sports/lucra-android-sdk`. If this file is not sitting inside that
> docs folder (e.g. it was installed as a standalone agent skill), clone the public repo (shallow
> is fine) and resolve links there. Use only these published docs — never private Lucra checkouts
> that may exist elsewhere on the machine.

**Scope:** empty-but-successful symptoms after the `lucra-android-start` "initialized
successfully" bar is met, and pre-flight provisioning checks before feature work. If you have an
explicit `Failure`/error result in hand, go to `lucra-android-errors`. Verified against SDK
`6.8.0`.

## The mental model

`LucraClient.initialize(apiKey, environment)` resolves which **tenant** you are. From then on,
everything the SDK shows or allows is shaped by three layers you cannot see or change from the
integration:

1. **Backend configuration** for your tenant (which contest types exist, payment methods, limits,
   supported states, KYC policy).
2. **Per-tenant feature gates** (e.g. real-money actions, convert-to-credit).
3. **Out-of-band provisioning** Lucra performs for you (mini game catalog, push notifications,
   payment processor credentials, geolocation licensing).

None of these are self-serve. The tenant console lets you manage tournaments, achievements,
rewards, and webhooks, and *view* your API keys — every gate and configuration in this skill is
changed by Lucra on request. **Silent emptiness is the designed failure mode:** an unprovisioned
feature returns an empty `Success`, hides its UI, or no-ops gracefully. Nothing errors. So the
diagnostic question is never "what exception was thrown" — it is "which capability is this tenant
missing, and is it provisioning or my own wiring?"

## Rule out your own wiring first (30 seconds)

Four partner-side causes mimic provisioning gaps. Check these before contacting support:

- [ ] **Flows show nothing at all** → `lucraUiProvider` left at its no-op default. Pass
      `LucraUi(...)` from `sdk-ui`. → [Init](../../1.2.0_initialize_client.md)
- [ ] **User-scoped lists are empty** → no signed-in user, or a fresh user with no history.
      Confirm `observeSDKUserFlow()` emits `Success` first.
- [ ] **Free-to-play options missing** → your app never registered a reward provider via
      `LucraClient().setRewardProvider(...)`. Without it, FTP contest options disappear even on a
      fully provisioned tenant. → [Free to Play](../../1.2.6_free_to_play_support.md)
- [ ] **Reward sheet never auto-shows** → `allowRewardSheetToDisplay = false` was passed at
      `initialize` (it defaults to `true`), or the tenant gate
      `disable_reward_sheet_outside_sdk` is set intentionally.

**The gate-timing gotcha:** feature gates resolve only after the SDK's flag service initializes —
up to ~10 seconds on a cold start. A gate check made too early returns the default, not the real
value. Gate-dependent UI in your app must therefore **default to hidden and reveal when the gate
resolves true** — never default to visible. Defaulting visible leaks real-money UI to users whose
tenant has it disabled, then snatches it away.

## Symptom → capability → what to request

Fixer legend: **Lucra** = request via support (nothing to change in your code); **You** = partner
code or console. Internal identifiers are in parentheses — include them in support requests so
Lucra can act without translation.

| Symptom (no error shown) | Capability that gates it | Fixer | What to ask Lucra |
|---|---|---|---|
| `getMiniGames` returns empty `Success`; `MinigamesHome` renders no games | Mini games are enabled per game, per tenant, **per environment** | Lucra | "Enable mini games [names] for tenant [X] in [sandbox/production]" |
| Cash contests hidden; Add/Withdraw Funds absent or no-op; only free entries offered | Real-money actions gate (`real_money_actions_enabled`) | Lucra | "Enable real-money actions for tenant [X] in [env]" |
| A contest type is missing from create flows (recreational games or sports) | Contest-type config (`GYP_ENABLED` for recreational games, `SYW_ENABLED` for sports contests) | Lucra | "Enable [recreational games / sports contests] for tenant [X]" |
| Free-to-play contest options missing (and `setRewardProvider` IS registered) | Free-to-play gate (`gyp_ftp_enabled`) | Lucra | "Enable free-to-play recreational games for tenant [X]" |
| Convert-to-credit option absent from Withdraw Funds | Credit conversion gate (`convert_to_credit`) | Lucra | "Enable convert-to-credit withdrawals for tenant [X]" |
| `autoJoinTournaments` returns `FailedTournamentCall.FeatureDisabled` | Auto-join gate (`auto_join_pool_tournaments_on_launch`); manual join still works | Lucra | "Enable tournament auto-join on launch for tenant [X]" |
| Tournaments home renders but lists nothing | Tournaments and their payout/reward structures are created per tenant by Lucra | Lucra | "Set up [sandbox test] tournaments and payout structures for tenant [X]" |
| Push token registration succeeds but Lucra pushes never arrive | Tenant push-notification setup is an out-of-band Lucra process; token inserts succeed regardless | Lucra | "Complete push-notification setup for tenant [X]" → [Push Notifications](../../1.2.3_push_notifications.md) |
| Feature geo-blocked in a state you legitimately operate in | Per-tenant supported-states registration (`SUPPORTED_STATES`) | Lucra | "Add states [list] to tenant [X]'s supported states" |
| `LocationError("GeoComply invalid license")` | GeoComply license provisioned by Lucra per tenant | Lucra | "Our GeoComply license appears invalid/expired for tenant [X] in [env]" — see `lucra-android-errors` |
| Expected payment methods missing from Add Funds | Per-tenant payment provider credentials + method config (`PAYPAL_VENMO_ENABLED`, `ADD_CREDIT_CARD_ENABLED`) | Lucra | "Enable [PayPal/Venmo / credit card / ...] deposits for tenant [X]" → [Payments](../../1.2.4_payments.md) |
| No API key for an environment, or key needs rotation | Keys are issued per environment at tenant creation; console shows them read-only | Lucra | "Issue/rotate the [Android] API key for tenant [X] in [env]" |

Note the last row's sibling failure: a key that *exists* but is paired with the wrong
`Environment` fails loudly (`Unauthorized Access`) — that's `lucra-android-errors` territory, not
provisioning.

## Pre-flight checklist (before writing feature code)

Confirm with Lucra, per feature area the integration will use — in sandbox first, then again for
production (enablement is per environment and does not carry over):

- **Always:** API key for each target environment; supported states registered; sandbox test
  users available on request for end-to-end testing.
- **Mini Games:** which games are enabled for your tenant (`getMiniGames` is also your runtime
  check — non-empty means provisioned).
- **Real money:** real-money actions gate on; payment methods you plan to offer enabled; KYC
  policy confirmed (`deferKycUntilWithdrawal` changes when users must verify).
- **Free-to-play:** FTP gate on, and your `LucraRewardProvider` implemented.
- **Tournaments:** tournaments + payout structures created for your tenant.
- **Push:** tenant push setup completed with Lucra.

State each confirmed item back before building on it; anything unconfirmed is a probable
silent-empty later.

## Support-request template

> Tenant: [name / tenant identifier]
> Environment(s): [sandbox / production]
> Platform: Android SDK [version]
> Please enable/confirm:
> - [ ] Mini games: [game names]
> - [ ] Real-money actions (`real_money_actions_enabled`)
> - [ ] Contest types: [recreational games (`GYP_ENABLED`) / sports (`SYW_ENABLED`) / free-to-play (`gyp_ftp_enabled`)]
> - [ ] Payment methods: [list]
> - [ ] Supported states: [list]
> - [ ] Tournaments + payout structures
> - [ ] Push-notification setup
> - [ ] Sandbox test users for integration testing

Delete rows you don't need. The parenthesized identifiers are Lucra-internal names — including
them is optional but removes a translation round-trip.

## What you can verify without Lucra

- **Console** (tenant login): manage tournaments, achievements, rewards, webhooks; view API keys
  (read-only — creation/rotation goes through support).
- **From the SDK at runtime:** `getMiniGames` (empty = not provisioned), presenting a flow and
  seeing content vs. empty, `autoJoinTournaments` returning `FeatureDisabled`.
- Everything else in the table above has **no partner-visible status check** — if the symptom
  matches and your wiring is ruled out, go straight to the support request rather than debugging
  further. Debugging cannot fix a provisioning gap.
