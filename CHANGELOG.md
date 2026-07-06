# Changelog

All notable changes to KindredValeMod are documented in this file.

## [0.1.1] - 2026-07-03

### Changed

- **Hub build mode toggle** — Players with `hexvane.kindredvalemod.hub.build` (including `hytale:Admin` via `*`) must run `/hubbuild` to edit blocks in protected hub worlds. Protection stays on by default so staff can hang out in the hub without accidentally breaking blocks. Build mode is per-session and clears on disconnect.

## [0.1.0] - 2026-07-03

### Added

- **Hub spawn protection** — Whole-world protection for configured hub worlds. Players cannot break, place, or punch blocks unless they have build permission. PvP is disabled on protected worlds by default.
  - Permission node: `hexvane.kindredvalemod.hub.build`
  - Virtual group: `kindredvale:HubBuilder` (assign via `/setgroup` or `permissions.json`)
  - `hytale:Admin` bypasses automatically via `*`
  - Config file: `HubProtection.json` in the plugin data folder (written on first start if missing)
    - `ProtectedWorlds` — list of hub world names (default: `["KindredVale"]`)
    - `DisablePvpInProtectedWorlds` — disable PvP on hub worlds (default: `true`)
  - Implemented via ECS block event systems (`BreakBlockEvent`, `PlaceBlockEvent`, `DamageBlockEvent`); no tick-based Store writes

### Notes

- When assigning groups with `/setgroup`, Hytale may log a `PlayerRef.getComponent(Player) called async` SEVERE message. This comes from vanilla world-map/permissions code, not KindredValeMod, and does not usually prevent the group change from applying.
