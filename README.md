# Easy TP

A **client-side** Fabric mod for **Minecraft 1.21.11**: an operator dashboard for players, colour-coded
teams and teleports. No server-side install, no plugins, no typed commands — everything happens in a
dark, red-accented GUI.

Press **P** (rebindable in *Options → Controls → Easy TP*) to open the dashboard.

---

## Features

**Dashboard** — live counters (online, selected, teams), the server/dimension you are on, quick
actions and a summary of the last teleport.

**Players** — every player from the client player list, with head, name, team chip, latency and game
mode. Search by username *or* team, multi-select with checkboxes, select/deselect all, refresh,
assign to a team and remove from teams. Disconnected players are dropped from selections
automatically; team membership is kept.

**Teams** — unlimited teams, four defaults (GREEN / BLUE / RED / YELLOW). Create, rename, recolour
(palette or `#RRGGBB`), add/remove members (online *and* offline ones), delete, and select one or many
teams. Clicking a team selects all of its online members. One team per player by default, with an
optional multi-team mode.

**Teleport console** — pick any combination of individual players, entire teams, multiple teams or
everyone, then choose a destination:

* **Player destination** — the destination is highlighted in gold and can be changed at any time.
* **Coordinate destination** — X / Y / Z fields, *use my current location*, clear, validate, and an
  explicit destination dimension (current, Overworld, Nether, End).

Every operation goes through a **review screen** (revalidates online state, coordinates and the
"destination is never its own target" rule) and ends on a **results screen**: requested, confirmed,
failed, unconfirmed and disconnected counts plus a per-player status list. *Retry failed* only
resends operations that were not confirmed.

**Settings** — animations, background opacity, GUI density, compact list, player heads, online
status, selection colours, auto refresh, sorting, search preferences, confirm-before-teleport,
result screen, auto-save, multi-team mode and command spacing. All of it is stored locally.

---

## How teleports actually work

Easy TP never fakes an inventory-menu teleport and never touches packets directly. It sends the same
commands you would type yourself, through the normal chat-command channel:

| Destination | Command sent |
| --- | --- |
| Another player | `/tp <player> <destination>` |
| Coordinates (same dimension) | `/tp <player> <x> <y> <z>` |
| Coordinates (other dimension) | `/execute in <dimension> run tp <player> <x> <y> <z>` |

Consequences, stated plainly:

* **You need OP / command permission on the server** (or cheats in singleplayer). If the server
  rejects a command, Easy TP shows the server's own error text and marks the operation **FAILED**.
* The client **cannot force** a server to move a player. If the server silently ignores a command,
  Easy TP does not invent success: the result stays **UNCONFIRMED**.
* Commands are spaced out (`Command spacing`, default 2 ticks) to avoid throttling, and each player
  receives exactly one command per operation.
* Success is marked **CONFIRMED** only when your own client can see the target at the destination
  after the operation. Targets in another dimension (or outside loaded chunks) stay **UNCONFIRMED**.

Statuses: **GREEN = confirmed**, **RED = failed**, **GOLD = unconfirmed/pending**, **orange = disconnected**.

---

## Install

1. Install **Fabric Loader** for Minecraft **1.21.11**.
2. Install **Fabric API** for 1.21.11 (0.141.x or newer).
3. Put `easy_tp-1.0.0.jar` into `.minecraft/mods`.
4. Launch the game — the mod is client-only; servers do not need it.

## Build

Requires **Java 21**.

```bash
./gradlew build
```

The jar is written to `build/libs/easy_tp-1.0.0.jar`. (A pre-generated Gradle wrapper is included;
use `gradle build` if you prefer your own Gradle 9.x.)

CI: every push builds the mod with GitHub Actions (`.github/workflows/build.yml`) and uploads the jar
as the `easy-tp-jar` artifact.

---

## Data storage

Everything lives in `config/easy_tp.json` next to your `mods` folder:

* teams (name, colour, member UUIDs and last known usernames) — **separated per server/world**, so two
  servers never share a roster,
* all settings and status colours,
* last used coordinates and dimension.

Writes are automatic (toggle *Save teams automatically*, or use *Save configuration now*). A corrupted
file is backed up to `easy_tp.json.corrupt` and replaced with defaults instead of crashing the game.
No passwords, tokens or account data are ever stored.

---

## Project layout

```text
com.nebrion.easytp
├── EasyTP.java              constants, singletons, server identity
├── EasyTPClient.java        client entrypoint (config, keybind, tick, chat hook)
├── keybind/KeybindManager   P keybind + single-instance GUI opening
├── config/                  EasyTpConfig, ModSettings, ColourSettings, ConfigManager
├── player/                  PlayerData, PlayerManager (online list + selection)
├── team/                    TeamData, TeamManager (teams, membership, storage)
├── teleport/                Destination, CoordinateManager, CommandBuilder,
│                            TeleportManager, TeleportResult, CommandExecutor, ChatWatcher
├── util/                    Ticker, Toasts, ColourUtil
└── gui/                     Theme, EasyTpScreen (chrome + widget toolkit) and one screen per
                             section: Dashboard, Players, Teams, TeamEditor, Teleport,
                             Destination, Coordinate, ConfirmTeleport, Results, Settings
```

No mixins are used; the mod only relies on public Fabric API events and vanilla screens.

---

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| "Server rejected the command: ..." | You are not OP / lack command permission, or the server blocks `/tp`. |
| Result stays UNCONFIRMED | The target is in another dimension or out of view; the client cannot verify it. |
| Nothing happens, no error | The server silently ignored the command; check your permissions. |
| Keybind does nothing | Rebind it in *Controls → Easy TP*; the mod is client-side and only opens in-game. |
| Teams are gone on another server | By design: team rosters are per server/world. |

---

## Status

* Compiles cleanly against Minecraft 1.21.11 (Yarn `1.21.11+build.3`, Fabric API `0.141.6+1.21.11`,
  Fabric Loader `0.17.3`, Java 21) — see the CI badge/artifacts.
* In-game behaviour has been implemented to spec but could not be verified in a running client in this
  environment; the results screen is deliberately honest about anything the client cannot confirm.

## License

MIT — see `LICENSE`.
