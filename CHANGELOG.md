# Changelog

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
