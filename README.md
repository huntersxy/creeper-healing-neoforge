# Creeper Healing

A server-side, highly customizable **NeoForge** mod that automatically and naturally heals Creeper explosions — and other types of explosions — restoring the terrain of your world.

This is a **NeoForge 1.21.1 / 1.21.8 / 1.21.10 port** of [creeper-healing](https://github.com/ArkoSammy12/creeper-healing) by **ArkoSammy12** (Fabric, LGPL-2.1). All code was rewritten for the NeoForge API (Stonecutter multi-version); the original feature set is preserved and several parts were optimized for the NeoForge platform.

> ⚠️ This is an unofficial port. If you run into problems, please report them here rather than on the original project's issue tracker.

## Requirements

| Minecraft | NeoForge | Java |
|---|---|---|
| 1.21.1 | **21.1.x** (21.1.244) | 21 |
| 1.21.8 | **21.8.x** (21.8.54) | 21 |
| 1.21.10 | **21.10.x** (21.10.64) | 21 |

## Features

### Healing modes

- **Default mode**: waits for the configured delay, then heals the destroyed blocks one by one.
- **Daytime healing mode**: explosions wait until sunrise to begin healing, and only heal while and where there is light.
- **Difficulty-based healing mode**: healing is sped up or slowed down depending on the world difficulty.
- **Blast-resistance-based healing mode**: blocks with higher blast resistance take longer to heal, with a randomized offset so blocks heal in bursts.

A block is only healed where a player would be able to place it.

### Explosion sources

Mob explosions (Creepers, Ghasts, …), block explosions (beds, respawn anchors, end crystals), TNT (blocks and minecarts), triggered explosions (wind charges) and other sources can each be enabled or disabled independently, with a blacklist for mob sources.

### Item drop control

Configure per source type whether explosions drop items. Blocks that are healed back never drop
their items (drops plus restoration would duplicate every destroyed block), so these settings only
affect explosion sources whose healing is disabled. Container contents can also be kept (and
restored together with the block's NBT data) via `restore_block_nbt`.

### More

- Configurable explosion heal delay and per-block placement delay (minimum 0.05 s).
- Restore block-entity NBT data; optionally force blocks with NBT to always heal.
- Make healed falling blocks (sand, gravel, …) stay in place until a neighbor update.
- Optional block whitelist.
- Replace map: heal certain blocks as other blocks (e.g. diamond blocks as stone), preserving block properties.
- Block placement sound effect and cloud particles.
- Splash potions of **Healing** heal explosions instantly; **Regeneration** potions start them early.
- Fully configurable in-game via commands; config file hot-reload.
- Scheduled healings survive server restarts (saved per dimension).

## Installation

1. Install the matching [NeoForge](https://neoforged.net/) for your Minecraft version and run it once.
2. Put the matching `creeperhealing-1.1.0+<mc>.jar` (`+1.21.1`, `+1.21.8` or `+1.21.10`) into the `mods/` folder.
3. Start the game. The configuration file `config/creeper-healing.toml` is generated on first launch.

## Configuration

All settings live in `config/creeper-healing.toml` (same layout as the original mod, so existing configs can be carried over). Edit the file and run `/creeper-healing reload_config`, or change settings in-game with commands.

| Section | Setting | Default | Description |
|---|---|---|---|
| `[delays]` | `explosion_heal_delay` | `3.0` | Seconds an explosion waits before healing starts. |
| `[delays]` | `block_placement_delay` | `1.0` | Seconds between each healed block. |
| `[explosion_item_drops]` | `drop_items_on_mob_explosions` | `false` | Drop items from mob explosions. |
| `[explosion_item_drops]` | `drop_items_on_block_explosions` | `true` | Drop items from bed / end crystal explosions. |
| `[explosion_item_drops]` | `drop_items_on_tnt_explosions` | `true` | Drop items from TNT explosions. |
| `[explosion_item_drops]` | `drop_items_on_triggered_explosions` | `true` | Drop items from wind-charge explosions. |
| `[explosion_item_drops]` | `drop_items_on_other_explosions` | `true` | Drop items from other explosion sources. |
| `[explosion_item_drops]` | `drop_items_on_mob_explosions_blacklist` | `["minecraft:placeholder"]` | Mob ids that never drop items. |
| `[explosion_sources]` | `heal_mob_explosions` | `true` | Heal mob explosions. |
| `[explosion_sources]` | `heal_block_explosions` | `false` | Heal bed / end crystal explosions. |
| `[explosion_sources]` | `heal_tnt_explosions` | `false` | Heal TNT explosions. |
| `[explosion_sources]` | `heal_triggered_explosions` | `false` | Heal wind-charge explosions. |
| `[explosion_sources]` | `heal_other_explosions` | `false` | Heal other explosion sources. |
| `[explosion_sources]` | `heal_mob_explosions_blacklist` | `["minecraft:placeholder"]` | Mob ids whose explosions are never healed. |
| `[explosion_healing_mode]` | `mode` | `"default_mode"` | `default_mode`, `daytime_healing_mode`, `difficulty_based_healing_mode`, `blast_resistance_based_healing_mode`. |
| `[preferences]` | `restore_block_nbt` | `false` | Restore block-entity NBT when healing. |
| `[preferences]` | `force_blocks_with_nbt_to_always_heal` | `false` | Always heal blocks that carry NBT data. |
| `[preferences]` | `make_falling_blocks_fall` | `true` | Let healed sand/gravel fall; if `false`, they stay until a neighbor update. |
| `[preferences]` | `block_placement_sound_effect` | `true` | Play a placement sound when healing. |
| `[preferences]` | `block_placement_particles` | `true` | Spawn cloud particles when healing. |
| `[preferences]` | `heal_on_healing_potion_splash` | `true` | Healing potions finish healing instantly. |
| `[preferences]` | `heal_on_regeneration_potion_splash` | `true` | Regeneration potions start healing early. |
| `[preferences]` | `enable_whitelist` | `false` | Only heal whitelisted blocks. |
| `[whitelist]` | `whitelist` | `["minecraft:placeholder"]` | Block ids allowed to heal. |
| `[replace_map]` | `"old_block" = "new_block"` | `"minecraft:diamond_block" = "minecraft:stone"` | Heal `old_block` as `new_block`. |

> The same replace-map key must not appear twice, otherwise the mod will refuse to load the configuration.

## Commands

All commands require operator permission. Query a value by running the command without arguments.

```
/creeper-healing reload_config
/creeper-healing mode [mode]
/creeper-healing explosion_heal_delay [seconds]
/creeper-healing block_placement_delay [seconds]
/creeper-healing <setting> [true|false]        # any boolean setting, e.g. heal_tnt_explosions
/creeper-healing <list> add <id>               # e.g. whitelist add minecraft:stone
/creeper-healing <list> remove <id>
/creeper-healing <list> list
/creeper-healing replace_map add <old> <new>
/creeper-healing replace_map remove <old>
/creeper-healing replace_map list
```

Boolean settings: `drop_items_on_mob_explosions`, `drop_items_on_block_explosions`, `drop_items_on_tnt_explosions`, `drop_items_on_triggered_explosions`, `drop_items_on_other_explosions`, `heal_mob_explosions`, `heal_block_explosions`, `heal_tnt_explosions`, `heal_triggered_explosions`, `heal_other_explosions`, `restore_block_nbt`, `force_blocks_with_nbt_to_always_heal`, `make_falling_blocks_fall`, `block_placement_sound_effect`, `block_placement_particles`, `heal_on_healing_potion_splash`, `heal_on_regeneration_potion_splash`, `enable_whitelist`.

List settings: `heal_mob_explosions_blacklist`, `drop_items_on_mob_explosions_blacklist`, `whitelist`.

## Building from source

Requirements: **JDK 21** (e.g. [Eclipse Temurin 21](https://adoptium.net/)).

```
./gradlew build                          # active version (1.21.1)
./gradlew :1.21.8-neoforge:build         # 1.21.8
./gradlew :1.21.10-neoforge:build        # 1.21.10
./gradlew buildAndCollect                # all three, jars in build/libs/<mc>/
./gradlew :1.21.1-neoforge:runGameTestServer  # headless smoke test (also 1.21.8 / 1.21.10)
```

Artifacts are at `build/libs/<mc>/creeperhealing-<ver>+<mc>.jar` (e.g. `creeperhealing-1.1.0+1.21.1.jar`).

## License & Credits

- Licensed under the **GNU Lesser General Public License v2.1** — see [LICENSE](LICENSE).
- This mod is a port of [creeper-healing](https://github.com/ArkoSammy12/creeper-healing) by [ArkoSammy12](https://github.com/ArkoSammy12) (LGPL-2.1).
- Mod icon by [Kioku](https://github.com/takoyakioku) (as credited by the original mod).
- The NeoForge MDK template (MIT) was used as the project skeleton.
