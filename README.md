<br/>
<div align="center">
<a href="https://github.com/senseiwells/PuppetPlayers">
<img src="./src/main/resources/assets/puppet-players/icon.png" alt="Logo" width="80" height="80">
</a>
<h3 align="center">Puppet Players</h3>
<p align="center">
A fabric mod for fake 'puppet' players in Minecraft
</p>
</div>

## About The Project

Modern, simple implementation of the fake players implemented originally in Carpet Mod.

There are some fundamental differences to how these fake players are implemented in comparison to the original implementation in Carpet Mod.

This mod can be used alongside Carpet Mod, carpet's fake player actions will work on these puppet players; however, it's recommended to use the provided actions instead.

The aim of this mod is to keep the behaviour of the puppet players as accurate as possible to a real player:

- All the player ticking is done in the correct tick phase.
- The client code for player actions is simulated on the server.
- Puppet players send packets to interact with the server, like a real player.
- Maintaining compatibility with all mods.
## Getting Started

The mod can be installed from modrinth:

[![Modrinth download](https://img.shields.io/modrinth/dt/puppet-players?label=Download%20on%20Modrinth&style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbDpzcGFjZT0icHJlc2VydmUiIGZpbGwtcnVsZT0iZXZlbm9kZCIgc3Ryb2tlLWxpbmVqb2luPSJyb3VuZCIgc3Ryb2tlLW1pdGVybGltaXQ9IjEuNSIgY2xpcC1ydWxlPSJldmVub2RkIiB2aWV3Qm94PSIwIDAgMTAwIDEwMCI+PHBhdGggZmlsbD0ibm9uZSIgZD0iTTAgMGgxMDB2MTAwSDB6Ii8+PGNsaXBQYXRoIGlkPSJhIj48cGF0aCBkPSJNMTAwIDBIMHYxMDBoMTAwVjBaTTQ2LjAwMiA0OS4yOTVsLjA3NiAxLjc1NyA4LjgzIDMyLjk2MyA3Ljg0My0yLjEwMi04LjU5Ni0zMi4wOTQgNS44MDQtMzIuOTMyLTcuOTk3LTEuNDEtNS45NiAzMy44MThaIi8+PC9jbGlwUGF0aD48ZyBjbGlwLXBhdGg9InVybCgjYSkiPjxwYXRoIGZpbGw9IiMwMGQ4NDUiIGQ9Ik01MCAxN2MxOC4yMDcgMCAzMi45ODggMTQuNzg3IDMyLjk4OCAzM1M2OC4yMDcgODMgNTAgODMgMTcuMDEyIDY4LjIxMyAxNy4wMTIgNTAgMzEuNzkzIDE3IDUwIDE3Wm0wIDljMTMuMjQgMCAyMy45ODggMTAuNzU1IDIzLjk4OCAyNFM2My4yNCA3NCA1MCA3NCAyNi4wMTIgNjMuMjQ1IDI2LjAxMiA1MCAzNi43NiAyNiA1MCAyNloiLz48L2c+PGNsaXBQYXRoIGlkPSJiIj48cGF0aCBkPSJNMCAwdjQ2aDUwbDEuMzY4LjI0MUw5OSA2My41NzhsLTIuNzM2IDcuNTE3TDQ5LjI5NSA1NEgwdjQ2aDEwMFYwSDBaIi8+PC9jbGlwUGF0aD48ZyBjbGlwLXBhdGg9InVybCgjYikiPjxwYXRoIGZpbGw9IiMwMGQ4NDUiIGQ9Ik01MCAwYzI3LjU5NiAwIDUwIDIyLjQwNCA1MCA1MHMtMjIuNDA0IDUwLTUwIDUwUzAgNzcuNTk2IDAgNTAgMjIuNDA0IDAgNTAgMFptMCA5YzIyLjYyOSAwIDQxIDE4LjM3MSA0MSA0MVM3Mi42MjkgOTEgNTAgOTEgOSA3Mi42MjkgOSA1MCAyNy4zNzEgOSA1MCA5WiIvPjwvZz48Y2xpcFBhdGggaWQ9ImMiPjxwYXRoIGQ9Ik01MCAwYzI3LjU5NiAwIDUwIDIyLjQwNCA1MCA1MHMtMjIuNDA0IDUwLTUwIDUwUzAgNzcuNTk2IDAgNTAgMjIuNDA0IDAgNTAgMFptMCAzOS41NDljNS43NjggMCAxMC40NTEgNC42ODMgMTAuNDUxIDEwLjQ1MSAwIDUuNzY4LTQuNjgzIDEwLjQ1MS0xMC40NTEgMTAuNDUxLTUuNzY4IDAtMTAuNDUxLTQuNjgzLTEwLjQ1MS0xMC40NTEgMC01Ljc2OCA0LjY4My0xMC40NTEgMTAuNDUxLTEwLjQ1MVoiLz48L2NsaXBQYXRoPjxnIGNsaXAtcGF0aD0idXJsKCNjKSI+PHBhdGggZmlsbD0ibm9uZSIgc3Ryb2tlPSIjMDBkODQ1IiBzdHJva2Utd2lkdGg9IjkiIGQ9Ik01MCA1MCA1LjE3MSA3NS44ODIiLz48L2c+PGNsaXBQYXRoIGlkPSJkIj48cGF0aCBkPSJNNTAgMGMyNy41OTYgMCA1MCAyMi40MDQgNTAgNTBzLTIyLjQwNCA1MC01MCA1MFMwIDc3LjU5NiAwIDUwIDIyLjQwNCAwIDUwIDBabTAgMjUuMzZjMTMuNTk5IDAgMjQuNjQgMTEuMDQxIDI0LjY0IDI0LjY0UzYzLjU5OSA3NC42NCA1MCA3NC42NCAyNS4zNiA2My41OTkgMjUuMzYgNTAgMzYuNDAxIDI1LjM2IDUwIDI1LjM2WiIvPjwvY2xpcFBhdGg+PGcgY2xpcC1wYXRoPSJ1cmwoI2QpIj48cGF0aCBmaWxsPSJub25lIiBzdHJva2U9IiMwMGQ4NDUiIHN0cm9rZS13aWR0aD0iOSIgZD0ibTUwIDUwIDUwLTEzLjM5NyIvPjwvZz48cGF0aCBmaWxsPSIjMDBkODQ1IiBkPSJNMzcuMjQzIDUyLjc0NiAzNSA0NWw4LTkgMTEtMyA0IDQtNiA2LTQgMS0zIDQgMS4xMiA0LjI0IDMuMTEyIDMuMDkgNC45NjQtLjU5OCAyLjg2Ni0yLjk2NCA4LjE5Ni0yLjE5NiAxLjQ2NCA1LjQ2NC04LjA5OCA4LjAyNkw0Ni44MyA2NS40OWwtNS41ODctNS44MTUtNC02LjkyOVoiLz48L3N2Zz4=)](https://modrinth.com/mod/puppet-players)

## Usage

After installing the mod the `/puppet` command should become available to operators.
The `/puppet` command can also be made available to all players by changing the config,
see [the config section](#config) for more details.

We can make a puppet player join the world using one of the following commands:
```mcfunction
/puppet <username> join
/puppet <username> spawn
/puppet <username> spawn at <position> facing <rotation> in <dimension> in <gamemode>
/puppet <username> shadow
```
- The `join` subcommand spawns the specified puppet at its previous log-off position, this essentially simulates
as if the player were joining the server themselves.
- The `spawn` subcommand spawns the specified puppet at either a specified position or the position of the
command executor if the position is not specified.
- The `shadow` subcommand makes a puppet player join in the place of a currently online player.

Once a player has joined the world, we can make the player leave by running the following command:
```
/puppet <player> leave
```

### Actions

Puppet players are able to run actions mimicking real player behaviour.
Certain actions can also be run on real players.

Here is a list of all available actions:

| Action                          | Description                                          | Arguments                                                                 | Works stand-alone | Puppet only |
|---------------------------------|------------------------------------------------------|---------------------------------------------------------------------------|-------------------|-------------|
| `"minecraft:attack"`            | Makes the player attack (left click).                | `<once\|hold\|release>`                                                   | Yes               | No          |
| `"minecraft:delay"`             | Adds a delay between chained actions.                | `<delay>`                                                                 | No                | No          |
| `"minecraft:drop"`              | Makes the player drop their selected item.           | `<entire_stack>`                                                          | Yes               | No          |
| `"minecraft:interrupt_look_at"` | Interrupts the current look at target.               | None                                                                      | Yes               | Yes         |
| `"minecraft:interrupt_move_to"` | Interrupts the players current pathfinding.          | None                                                                      | Yes               | Yes         |
| `"minecraft:jump"`              | Makes the player jump.                               | `<once\|hold\|release>`                                                   | Yes               | Yes         |
| `"minecraft:look"`              | Makes the player look in a direction.                | `<rotation>`                                                              | Yes               | No          |
| `"minecraft:look_at"`           | Makes the player look towards the specified target.  | `position <pos> <lock?>` or `entity <entity> <lock?>`                     | Yes               | Yes         |
| `"minecraft:move_to"`           | Makes the player pathfind to a position or entity.   | `position <pos> <sprint?> <jump?>` or `entity <entity> <sprint?> <jump?>` | Yes               | Yes         |
| `"minecraft:offhand"`           | Makes the player swap their item with their offhand. | None                                                                      | Yes               | No          |
| `"minecraft:sneak"`             | Makes the player sneak.                              | `<sneaking>`                                                              | Yes               | Yes         |
| `"minecraft:sprint"`            | Makes the player sprint.                             | `<sprinting>`                                                             | Yes               | Yes         |
| `"minecraft:swap_slot"`         | Makes the player swap to a slot.                     | `<slot>`                                                                  | Yes               | No          |
| `"minecraft:use"`               | Makes the player use (right click).                  | `<once\|hold\|release>`                                                   | Yes               | No          |

To run an action, we can run it stand-alone with the following command:
```mcfunction
/puppet <player> actions run <action> ...
```
This makes the player run the action immediately.

For example, to make a player hold attack, you would run:
```mcfunction
/puppet <player> actions run minecraft:attack hold
```

We can also chain multiple actions together by running the following command:
```mcfunction
/puppet <player> actions chain add <action> ...
```
Once you've added actions to the chain, we can also run the following commands to
control the chained behaviour:
```mcfunction
/puppet <player> actions chain loop <true|false> # Whether to infinitely loop the chained actions
/puppet <player> actions chain pause # Pause execution of the chained actions
/puppet <player> actions chain resume # Resume execution of the chained actions
/puppet <player> actions chain restart # Restart execution of the chained actions (from the first action)
/puppet <player> actions chain stop # Stops execution of the chained actions (and clears all actions)
```

For example, to make a player hold attack for 5 seconds, then let go for 2 seconds, then repeat we would run:
```mcfunction
/puppet <player> actions chain add minecraft:attack hold
/puppet <player> actions chain add minecraft:delay 5s
/puppet <player> actions chain add minecraft:attack release
/puppet <player> actions chain add minecraft:delay 2s
/puppet <player> actions chain loop true
```

### Config

The config is located in `./config/puppet-player-config.json` and by default should look like this:

```json
{
  "reload_puppet_players": true,
  "respawn_puppet_players": true,
  "puppet_player_death_delay": 0,
  "operator_required_for_puppets": true,
  "can_players_puppet_themselves": true
}
```
- `"reload_puppet_players"` - Whether puppets will rejoin if the server stopped with them last online
- `"respawn_puppet_players"` - Whether to respawn puppets after they die, if disabled they will leave the game instead
- `"puppet_player_death_delay""` - The delay after the puppet dies to either respawn/leave (determined by `respawn_puppet_players`)
- `"operator_required_for_puppets"` - Whether players need operator permissions to run the `/puppet` command
- `"can_players_puppet_themselves"` - Whether non-op players are able to run the `/puppet` command on themselves

### Developers

The puppet players mod is built on-top of [Arcade's NPC library](https://github.com/CasualChampionships/arcade).

You can depend on puppet players and add more actions, read the documentation in `PuppetPlayerAction` and 
`PuppetPlayerActionProvider` for more details!

Add the following to your `build.gradle.kts`:
```kts
repositories {
    maven("https://maven.supersanta.me/snapshots")
}

dependencies {
    implementation("me.senseiwells:puppet-players:1.8.0+26.2")
}
```

## License

Distributed under the MIT License. See [MIT License](https://opensource.org/licenses/MIT) for more information.
