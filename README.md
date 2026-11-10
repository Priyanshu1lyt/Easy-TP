# Easy TP

Client-side Fabric mod for **Minecraft 1.21.11**: a GUI dashboard for server operators to manage players,
colour-coded teams and teleports. No server-side install, no typed commands.

Press **P** (rebindable in Controls) to open the dashboard.

## Features
- PLAYERS: live online list with heads, search, multi-select, assign/remove teams
- TEAMS: unlimited colour-coded teams (hex or palette), edit, delete, multi-team select
- TELEPORT: players/teams/all -> a destination player OR XYZ coordinates (+ optional dimension)
- Confirmation screen, results screen (CONFIRMED / FAILED / UNCONFIRMED / DISCONNECTED), retry of failed only
- Settings (all stored locally in `config/easy_tp.json`, teams separated per server/world)

## How teleports work
The mod sends normal `/tp` (and `/execute in <dim> run tp ...` for a chosen dimension) commands through the
regular chat-command channel. **You need OP / command permission on the server** (or cheats in singleplayer).
Success is only shown as CONFIRMED when the server's own feedback message confirms it; otherwise the result is
UNCONFIRMED. The mod never bypasses server permissions.

## Build
Requires Java 21. Either push to GitHub and download the JAR from the **Actions -> build -> Artifacts**, or locally:

```
gradle build
```
The JAR is written to `build/libs/`. (Gradle 9.2.1; the wrapper JAR is not included, so use a local Gradle or run
`gradle wrapper` once.)

## Install
1. Install Fabric Loader for Minecraft 1.21.11 and Fabric API.
2. Put `easy_tp-x.y.z.jar` in your `mods` folder.

## Status
Written against Yarn `1.21.11+build.4` / Fabric API `0.141.1+1.21.11`. It has not yet been compiled or tested
in-game; see the build log of the first GitHub Actions run for any API mismatches.
