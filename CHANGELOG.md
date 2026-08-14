# Changelog

## [1.0.1] - 2025-08-14

### Fixed

- Prevent item duplication for support-dependent blocks (torches, rails, lanterns, ...):
  when their supporting block is destroyed by an explosion, the neighbor-update break no
  longer drops their items, since the blocks are healed back.
  (Mirrors the fix for upstream issue ArkoSammy12/creeper-healing#6.)

## [1.0.0] - 2025-08-14

## [1.0.0] - 2025-08-14

Initial release — NeoForge 1.21.1 port of creeper-healing (by ArkoSammy12).

### Features

- Four explosion healing modes: default, daytime, difficulty-based, blast-resistance-based.
- Per-source healing toggles and mob blacklists (mobs, blocks/beds/end crystals, TNT, triggered, other).
- Per-source item drop control with mob blacklist.
- Configurable explosion heal delay and block placement delay (min 0.05 s).
- Block-entity NBT restoration with optional forced healing of NBT blocks.
- Falling blocks (sand, gravel, ...) can stay in place until a neighbor update.
- Block whitelist and replace map (heal blocks as other blocks, preserving properties).
- Placement sound effects and cloud particles.
- Healing / Regeneration splash potions speed up healing.
- In-game command tree (/creeper-healing) for every setting plus config hot-reload.
- Per-dimension persistence of scheduled healings (survives restarts).
- Overlapping explosion merging and support-loss (indirect) block detection.

### Compatibility

- Minecraft 1.21.1, any NeoForge 21.1.x, Java 21.
- Runtime dependency on NeoForge is `[21.1,)`; Minecraft is pinned to 1.21.1.

### Notes

- Config file `config/creeper-healing.toml` keeps the original mod's layout.
- Licensed under LGPL-2.1; this is an unofficial port of creeper-healing by ArkoSammy12.
