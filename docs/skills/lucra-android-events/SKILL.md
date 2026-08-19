---
name: lucra-android-events
description: >
  How to observe change in the Lucra Android SDK — when to use the LucraEvent listener vs
  re-fetching state vs server-side webhooks, what each event actually fires for (and the ones
  with known caveats), and what every matchup status value means. Use when wiring
  setEventListener, reacting to matchup/tournament changes, debugging a listener that never
  fires, or interpreting matchup status values.
---

# Lucra Android — Events & Observing State

> **Portability:** all relative links in this skill (e.g. `../../1.2.0_initialize_client.md`)
> resolve against the `docs/` folder of the public repo
> `https://github.com/Lucra-Sports/lucra-android-sdk`. If this file is not sitting inside that
> docs folder (e.g. it was installed as a standalone agent skill), clone the public repo (shallow
> is fine) and resolve links there. Use only these published docs — never private Lucra checkouts
> that may exist elsewhere on the machine.

**Scope:** after the `lucra-android-start` "initialized successfully" bar is met. Behavior and
caveats verified against SDK `6.8.0`.

## The observation model (read this before wiring anything)

The SDK gives you three ways to learn that something changed, and they are not interchangeable:

1. **`LucraEvent` listener** (`LucraClient().setEventListener(...)`) — an **in-process,
   device-local** signal. An event fires when *your* user performs an action inside an SDK
   screen, in this app session. Nothing is pushed from the server. Events are not replayed:
   anything emitted before your listener is registered is gone.
2. **Fetched state** — `getUserMatchups`, matchup/tournament details, `observeSDKUserFlow()`.
   This is the only source of truth for what a matchup *is* right now, including changes made by
   the other participant or the backend.
3. **Webhooks** (server-side, self-serve in the tenant console) — the only reliable notification
   for settlement and other backend-driven transitions. Your backend subscribes; your app then
   learns via your own push/refresh.

The classic mistake — for humans and coding agents alike — is treating the event bus as a state
feed and reconstructing matchup state from event sequences. On a local-only, no-replay bus that
reconstruction is guaranteed wrong. **Events are UX hints about your user's own actions; state
lives in what you fetch.**

## Decision table

| You want to know… | Use | Not |
|---|---|---|
| My user just created / accepted / canceled / joined something in an SDK flow | `LucraEvent` listener → [Event Listener](../../1.2.10_lucra_event_listener.md) | — |
| The opponent acted, or a matchup changed remotely | Re-fetch on flow dismissal (`LucraFlowListener.onFlowDismissRequested`) or on resume → [Headless Interactions](../../1.2.9_headless_interactions.md) | Events — they never fire for remote actions |
| A matchup settled / money moved | Webhooks server-side; client re-fetch for display | Events — no settlement event exists |
| Where a matchup is in its lifecycle | Its status field (glossary below) | Inferring from which events you have seen |
| A mini game session ended | `LucraEvent.MiniGame.Finished` — **only when the session ran through the SDK's `MiniGame` flow**; a fully-headless session in your own WebView emits nothing, detect the end yourself | Waiting on `Finished` for headless sessions |

## What each event fires for

All events fire from actions taken in SDK-rendered screens on this device. Payload IDs are
nullable — null-check before use.

| Event | Fires when (this device, SDK UI) |
|---|---|
| `GamesContest.Created` | Your user completes a recreational-games matchup create flow |
| `GamesContest.Accepted` | Your user accepts a games matchup from a matchup **details** screen (includes IRL contests). ⚠ Not emitted when accepting from contest **list** screens — known gap in `6.8.0` |
| `GamesContest.Canceled` | Your user cancels a games matchup |
| `GamesContest.Started` | Your user (the creator) starts the matchup |
| `GamesContest.StartedActive` | The creator started while your (non-creator) user was viewing the matchup details; payload carries the full `LucraMatchup` |
| `SportsContest.Created` | Your user completes a sports matchup create flow |
| `SportsContest.Accepted` | Your user accepts a sports contest from the public feed component. ⚠ Type-hierarchy caveat below |
| `SportsContest.Canceled` | Your user cancels a sports matchup |
| `Tournament.Joined` | Your user joins a tournament from tournament details |
| `Tournament.AutoJoinedTournaments` | The SDK auto-joined tournaments at launch (tenant-gated — see `lucra-android-provisioning`) |
| `MiniGame.Finished` | An SDK-flow mini game session ended; `amount` is the payout |

### Known caveats in 6.8.0 (re-check on newer versions)

- **`SportsContest.Accepted` has the wrong parent type**: it extends `GamesContest`, not
  `SportsContest`. A `when` branch on `is LucraEvent.SportsContest -> ...` will NOT receive it —
  it lands in your `GamesContest` branch. Match it explicitly with
  `is LucraEvent.SportsContest.Accepted` (and expect a fix to move it in a future release —
  check release notes when upgrading).
- **List-screen accepts don't emit** `Accepted` events; details-screen accepts do. If accept
  tracking matters, also re-fetch on flow dismissal rather than relying on the event alone.

## Listener mechanics

- `setEventListener` holds **one** listener — the last call wins. Install a single app-level
  listener and fan out internally; don't let two modules each register.
- Register **before** presenting any flow. Zero replay: an event emitted while no listener is
  attached is dropped silently.
- The listener survives across flows; you don't re-register per flow.

## Matchup status glossary

Fetched matchups expose `MatchupType.Status` (full lifecycle) while `getUserMatchups` sections
use the simplified `ProfileMatchupStatus`. They are different vocabularies — never compare them
directly.

**`ProfileMatchupStatus`** (4 values): `PENDING` (awaiting acceptance or outcome submission),
`UPCOMING` (joined, not started — tournaments), `IN_PROGRESS`, `RESULTS` (settled — there is no
`COMPLETED` value).

**`MatchupType.Status`** (15 values):

| Status | Meaning |
|---|---|
| `OPEN` | Created, awaiting an opponent |
| `CONFIRMED` | Opponent accepted; not yet started/locked |
| `LOCKED` | Underlying live event in progress (sports) |
| `PENDING_OUTCOMES` | Play finished, result not yet settled — a real state your UI must handle |
| `CLOSED` / `CLOSED_TIE` | Settled with a winner / settled as a tie |
| `DISPUTE` | Outcome under dispute |
| `CANCELED_NOT_ACCEPTED` / `CANCELED_TIMEOUT` | Expired without acceptance — "nobody joined" |
| `CANCELED_BY_OWNER` | The creator canceled — user intent |
| `CANCELED_GAME_CANCELED` | The underlying game/event was called off |
| `CANCELED_THROUGH_API` / `CANCELED_DISPUTE` / `CANCELED_PLAYER_INACTIVE` | Backend-initiated cancellations |
| `UNKNOWN` | Unrecognized value — treat as non-actionable |

Don't collapse the `CANCELED_*` variants into one message: "no one accepted" and "the game was
called off" deserve different copy, and backend-initiated cancellations shouldn't be presented
as user actions.

## Backend-defined — do not infer client-side

The following are decided server-side and are **not observable as client events or documented
timings** — write a re-fetch (or webhook) instead of inventing a mechanism, and ask Lucra when
the answer matters: when funds are debited and refunded, escrow behavior, what triggers
`PENDING_OUTCOMES → CLOSED`, tie payout splits, and dispute resolution. Payout amounts appear on
fetched matchup details (`individualPayout`) after settlement.
