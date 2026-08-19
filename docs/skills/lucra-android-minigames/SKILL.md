---
name: lucra-android-minigames
description: >
  Integrate Lucra Mini Games on Android — both the SDK-rendered UI flows (Mini Games Home, game
  sessions, matchup details, profile, rewards) and the headless functions for fetching
  matchups/catalog and launching sessions to render your own UI. Use after the Lucra Android SDK is
  initialized and a signed-in user is confirmed. Points to the official docs for exact signatures.
---

# Lucra Android — Mini Games

> **Portability:** all relative links in this skill (e.g. `../../1.2.0_initialize_client.md`)
> resolve against the `docs/` folder of the public repo
> `https://github.com/Lucra-Sports/lucra-android-sdk`. If this file is not sitting inside that
> docs folder (e.g. it was installed as a standalone agent skill), clone the public repo (shallow
> is fine) and resolve links there. Use only these published docs — never private Lucra checkouts
> that may exist elsewhere on the machine.

**Prerequisite:** the `lucra-android-start` "initialized successfully" bar is met. If not, stop and
go there first. Requires SDK `6.8.0` or later (the published release that exposes the Mini Games
flows and `getMiniGames`); the `sdk-ui` artifact for the UI flows, headless calls need only
`sdk-core`.

**Tenant note:** Mini Games must be enabled for your tenant on the backend — per game and per
environment. If a Mini Games flow renders empty or reports unavailable, that is the first thing
to check; skill `lucra-android-provisioning` covers how to verify and what to request from Lucra.

**Experimental note:** the Mini Games flows and several related headless methods are annotated
experimental (some surface deprecation-style compiler warnings by design). They are not going
away — do not "fix" the warnings by avoiding the APIs.

## Choose your integration style

- Want Lucra's screens as-is, fastest path → **Non-headless (UI)**.
- Want your own landing/lists and only Lucra's data → **Headless**.
- Mixed is the norm and is supported: headless powers *your* lists, a flow handles the
  *interactive* part when the user taps in.

## Product vocabulary → API (avoid guessing)

| Product term | Maps to |
|---|---|
| "Battle Mode" / 1v1 | `LucraMiniGameMode.OneVsOne` (there is no "Battle" case in the mode enum) |
| "Minigames Profile" | `LucraFlow.MinigamesProfile` |
| "Completed" matchup | `ProfileMatchupStatus.RESULTS` (there is **no** `COMPLETED` case) |
| "In progress" matchup | `ProfileMatchupStatus.IN_PROGRESS` |
| "The games list / catalog" | `LucraClient().getMiniGames { ... }` → `List<MiniGameCatalogItem>` |

## Non-headless (UI)

You present an SDK flow; it renders and navigates itself. The flow cases:

- `LucraFlow.MinigamesHome` — the hub (game carousel, profile pill, achievement card, featured
  tournament; game taps route through mode selection). Supports unauthenticated launch.
- `LucraFlow.MiniGame(gameId, gameMode, amount, matchupId, handlePostNavigation)` — launches a
  specific session (loading screen + validation + WebView). Set `handlePostNavigation = true` to
  let the SDK route to matchup details on session end; the default `false` just dismisses.
- `LucraFlow.MatchupDetails(matchupId)` — a matchup's result/pending state. Prefer this generic
  router over `MinigameMatchupDetails`; it auto-routes to the minigames-themed screen when the
  matchup is a minigame.
- `LucraFlow.MinigamesProfile` — the user's Mini Games profile (stats, recent results, balance).
- `LucraFlow.MinigamesRewards` — achievement rewards grouped by game, with redeem sheet.

Present any of them via `LucraClient().getLucraDialogFragment(flow)` (full-screen),
`getLucraFragment(flow)` (embedded), or `GetLucraFlowComposable(flow)` (Compose) — always tagging
with `flow.toString()`. Exact signatures + examples: [Mini Games Flows](../../6.3_mini_games_flows.md),
[Lucra Flows setup](../../1.2.7_lucraflows.md).

### Non-headless checkpoints

- [ ] Presenting a flow shows Lucra UI, not nothing (`LucraUi` provider set? tenant provisioned?).
- [ ] Dismissal returns control to your app (`LucraFlowListener` wired per the flows doc).

## Headless (data only)

You call `LucraClient().*`, get a sealed result, render it yourself.

- **Recent matchups for a custom landing/profile screen:** `getUserMatchups(limit)` returns the
  user's matchups grouped by type (tournaments/games/sports) and status. Section on
  `ProfileMatchupStatus` (`IN_PROGRESS` vs `RESULTS`); page deeper with
  `getUserMatchupsByTypeStatus(type, status, limit, offset)`. Tapping a row →
  `LucraFlow.MatchupDetails(matchupId)`. There is **no aggregate-stats payload** (win rate,
  earnings) — derive anything like that client-side from the sections.
  → [Headless Interactions → User Matchups](../../1.2.9_headless_interactions.md)
- **Launch a session yourself (own WebView):** `startMiniGame(gameId, gameMode, amount, matchupId,
  onProgress, onResult)` returns a `MiniGameSession` whose `iframeUrl` you load. Pre-warm geo with
  `preloadGeoToken(...)`. → [Mini Games Headless](../../6.1_mini_games_headless.md)
- **Tenant catalog:** `getMiniGames { ... }` lists the tenant's enabled games + config — use it to
  build your own game picker instead of hardcoding game IDs.
  → [Mini Games Headless](../../6.1_mini_games_headless.md)
- **React to session end:** for sessions launched through the SDK's `MiniGame` flow, register a
  `LucraEventListener` and handle `LucraEvent.MiniGame.Finished` (refresh balance, surface
  rewards) rather than polling. **Fully-headless sessions in your own WebView emit no `Finished`
  event** — detect the session end yourself (see skill `lucra-android-events` for the observation
  model). → [Mini Game Events](../../6.2_mini_game_events.md)
- **Rewards:** minigame matchup rewards ride the tournament-reward methods —
  `getUserTournamentRewards(...)`, `claimReward(...)`, `markRewardViewed(...)`.
  → [Mini Games Headless → rewards](../../6.1_mini_games_headless.md)

### Headless checkpoints

- [ ] A signed-in user with history returns non-empty matchup sections.
- [ ] In-progress vs completed are distinguishable (`IN_PROGRESS` vs `RESULTS`).
- [ ] A headless-rendered row opens the correct matchup via `LucraFlow.MatchupDetails(matchupId)`.

## The bridge (the thing docs don't say in one place)

Headless and non-headless are designed to combine: `getUserMatchups` fills *your* recent-matchups
list; tapping a row presents `LucraFlow.MatchupDetails(matchupId)`; a "play" button presents
`LucraFlow.MinigamesHome` or a specific `LucraFlow.MiniGame(...)`; `LucraEvent.MiniGame.Finished`
tells you when to refresh the list. Wiring exactly this — a custom landing plus the flows and the
headless list — is the intended end state of an integration.
