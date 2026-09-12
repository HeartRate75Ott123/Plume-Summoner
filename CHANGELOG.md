# Changelog

## 1.1.3

### Added
- Per-summon count limit for listed mobs (data-driven): a new datapack file at `data/<namespace>/summon_limits/*.json`, format `{ "modid:entityid": number }`. The server clamps a single summon request to that mob's limit when the feature is on, and tells the player when a request was clamped. Shipped with a 183-entry boss list (`data/plume_summoner/summon_limits/bosses.json`), all set to 1. Hot-reloads with `/reload`; invalid entries are logged and skipped instead of crashing.
- Toggle button in the top-right corner of the summon menu, switching the list limit on/off per player (green = on, grey = off), with a tooltip showing the loaded list size. The state is server-authoritative, stored in the player's NBT (save-level) and re-synced on login, dimension change and death, so it survives death/respawn.

### Fixed
- Entity models could overflow their grid cell and cover neighbouring cells (especially wide/long/giant bosses). Models are still rendered whole, but each cell is now clipped to its own bounds, so overflow is cut off. The clip is released in a `finally` block so a rendering failure cannot leave scissoring active for the rest of the UI.
- The summon menu's top-bar scissor now uses the same `GuiGraphics` scissor stack as the per-cell clipping, so nested clipping is popped correctly.
- The new toggle button was laid out with `this.width` during `Screen` construction (when it is still 0), which placed it off-screen at a negative x. Its position is now applied in `init()`, once the real width is known.
- The list entry count shown in the toggle tooltip was always 0: the client parsed the datapack itself, but client reload listeners are not re-run on `/reload`. The count is now sent by the server together with the toggle state, and the client-side parser was removed.

## 1.0.1

### Added
- In-game configuration screen: open via Mod List -> Config (NeoForge's built-in `ConfigurationScreen`), replacing the broken custom GUI
- New config options in `config/plume_summoner-common.toml`:
  - `killsToUnlock` (default 1): how many kills are required to unlock a mob for summoning
  - `blacklist` (default empty): entities hidden from the summon menu, format `modid:entityid`; takes effect immediately on save
- Search overhaul powered by Searchables + PinIn:
  - Pinyin search with fuzzy initials/finals (e.g. `jhushi` matches 僵尸)
  - Component syntax: `name:`, `categories:` (mod filter), `favorites:` (favorites filter)
  - Auto-complete dropdown with component/value suggestions while typing
- Favorite any mob via the star icon on its grid entry (persisted locally); filter the list with `favorites:`
- Translations for the configuration screen (entry names and tooltips)

### Fixed
- Empty blacklist entries could not be removed in the config UI: the NeoForge list delete button depends on the spec validator, which rejected empty strings and dead-locked the list. Empty strings now pass validation (non-empty entries are still strictly checked), so empty entries can be edited/deleted.
- Missing config-screen translation keys: added `plume_summoner.configuration.*` keys (title, entries, tooltips).
- Auto-complete dropdown and hover tooltips in the summon menu were partially hidden by entity models: entity GUI rendering writes depth buffer entries (at z=50), causing later GUI text (z=0) to fail the depth test. The depth buffer is now cleared after the entity grid is rendered and blending/color state restored, so all popup text renders on top.
- Summoned mobs always faced a fixed direction instead of the player: `moveTo()` only sets `getYRot()`, while `LivingEntity` rendering is driven by `yBodyRot`/`yHeadRot` (both default to 0, i.e. south). All three rotations are now aligned with the player at summon time.
- Newly unlocked mobs were not sorted to the front of the unlocked section: the server stored kill counts in a `HashMap` whose iteration order is arbitrary, giving the client's "newest first" ordering no valid timestamp basis. The server now uses a `LinkedHashMap`, so unlock order follows first-kill order end to end.

## 1.0.0

Initial release:
- Kill any mob (monster/animal/boss) to permanently unlock its summon, persisted in the player's NBT data
- Open the summon menu with `G` (rebindable); 7-column grid with live 3D entity model previews, locked entries greyed out
- Search with pinyin/Chinese/English via Searchables + PinIn, including `name:` / `categories:` / `favorites:` component syntax and an auto-complete popup
- Favorite any mob via the star icon; filter favorites with `favorites:`
- Click an unlocked entry to summon the real mob 3 blocks in front of you (with AI), with no summon count limit
- Config (in-game via Mod List -> Config, or `config/plume_summoner-common.toml`): `killsToUnlock` (default 1) and `blacklist` (`modid:entityid`, hot-reloaded)
