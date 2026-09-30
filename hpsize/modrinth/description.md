![Mobs lined up from the smallest to the biggest health](https://raw.githubusercontent.com/pikczu77/plugins/claude/reckit-mod/hpsize/modrinth/gallery/gallery-1-lineup.png)

# Every mob is as big as its health

A mob with **20 HP** (as much as a player) keeps its normal size. Weaker mobs are smaller and stronger mobs are bigger. A chicken is tiny and an iron golem is a giant. **Every hit shrinks the mob**, and healing makes it grow back.

| Mob | HP | Size |
|---|---|---|
| Rabbit, fish | 3 | ×0.15 |
| Chicken | 4 | ×0.2 |
| Bat | 6 | ×0.3 |
| Sheep, silverfish, wild wolf | 8 | ×0.4 |
| Cow, pig, bee | 10 | ×0.5 |
| Spider | 16 | ×0.8 |
| Zombie, skeleton, creeper, villager | 20 | ×1 (normal) |
| Enderman, hoglin | 40 | ×2 |
| Piglin brute | 50 | ×2.5 |
| Elder guardian | 80 | ×4 |
| Iron golem, ravager | 100 | ×5 |
| Wither | 300 | ×15 |
| Warden | 500 | ×16 (the game's size limit) |

The mod uses the vanilla `minecraft:scale` attribute, so hitboxes grow and shrink with the model too. You can walk between a giant's legs, and a nearly dead zombie is tiny and hard to hit.

Inspired by the video [“Minecraft, but Mobs are as big as their health”](https://youtu.be/75mSpDQvTHo) by stuhpy. This is an independent mod written from scratch. It is not affiliated with or endorsed by stuhpy.

## Features

- **Size follows health**: sizes update live, either instantly or with a smooth shrink (default).
- **Presets**: `film` (exactly like the video), `smooth`, `fair` (bosses shrink from the first hit too) and `light` (smaller differences, less lag).
- **Everything is configurable per world**: formula, which HP counts as normal size, min/max size, players, suffocation, excluded mobs, a shrinking sound and a glow for tiny mobs you can't see.
- **Commands for setting up shots**: spawn a mob with an exact HP (so an exact size), change the HP of existing mobs, glow, mute.
- **Commands for recording videos**: a start countdown that freezes players, big on-screen titles, a camera mode, a recording mode that keeps command spam out of the chat, night vision and cleanup.
- **Clean HUD key** (client, optional): hides hearts, hotbar and crosshair but keeps your hand, the chat and titles.
- **Safe to remove**: the size is never saved into the mobs. Turn the mod off or uninstall it and every mob goes back to normal.
- **English and Polish**: every message follows your game language, even on a server where your client doesn't have the mod.

## Commands

All commands need operator permissions (cheats on in singleplayer).

### The mechanic: `/hpsize`

| Command | What it does |
|---|---|
| `/hpsize` | shows all settings |
| `/hpsize on` / `off` | turns the mechanic on / off (off brings every mob back to normal) |
| `/hpsize preset film\|smooth\|fair\|light` | ready-made settings (see Features) |
| `/hpsize mode health\|max\|percent\|sqrt` | size follows the current HP / the max HP (never shrinks) / the max HP and the % of health left / the square root of HP |
| `/hpsize normal <hp>` | how much HP means normal size (default 20) |
| `/hpsize min <x>` / `max <x>` | smallest / biggest size (0.0625–16) |
| `/hpsize smooth <0-100>` | how smoothly the size changes (0 = instantly) |
| `/hpsize players on\|off` | players change size too |
| `/hpsize suffocation on\|off` | whether giants suffocate in blocks (and shrink in caves) |
| `/hpsize exclude <mob>` / `include <mob>` | keep a mob type at its normal size |
| `/hpsize freeze` / `unfreeze` | freezes all sizes (for a still shot) |
| `/hpsize glowtiny on\|off`, `/hpsize glowtiny below <x>` | makes tiny mobs glow |
| `/hpsize sound on\|off` | a pufferfish “deflate” sound when a hit shrinks a mob |
| `/hpsize info [targets]` | HP and size of the mob you look at |
| `/hpsize reset` | default settings |

### Setting up shots

| Command | What it does |
|---|---|
| `/spawnsized <mob> <hp> [count]` | summons a mob with the given HP, e.g. `/spawnsized minecraft:chicken 200` (a ×10 chicken) |
| `/mobhp <targets> <hp>` | sets the HP (and so the size) |
| `/mobhp <targets> max <hp>` | sets the max HP and heals to full |
| `/heal [targets]` | heals players (and hunger) or mobs |
| `/glow <targets> [seconds]` | makes mobs glow (0 = off) |
| `/mute <targets>` / `/unmute <targets>` | silences mobs |

### Recording videos

| Command | What it does |
|---|---|
| `/go [seconds] [text]` | start of an episode: freeze players → countdown → START → unfreeze |
| `/countdown <seconds> [text]`, `/countdown cancel` | an on-screen countdown with sounds |
| `/freeze [players]` / `/unfreeze [players]` | keeps players in place (they can still look around) |
| `/announce <title>[\|subtitle]` | a big title for everyone, `&` colour codes work, e.g. `/announce &6Chapter 2\|The Nether` |
| `/recmode on\|off` | hides command messages in the chat, confirmations go to the action bar |
| `/cam` | camera mode (spectator); `/cam` again takes you back to the same spot and game mode |
| `/nv [players]` | night vision without particles |
| `/cleanup [items\|mobs\|all] [radius]` | removes dropped items, arrows, XP or hostile mobs |

### Client (optional)

| Key / command | What it does |
|---|---|
| `H` | **clean HUD**: hides hearts, hotbar, crosshair and more, but keeps the hand, chat and titles (unlike F1) |
| `/hud on\|off`, `/hud hide <element>`, `/hud show <element>`, `/hud list` | choose what the clean HUD hides |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.11.
2. Put this mod and [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods` folder.

**Servers**: install it on the server. Players can join without the mod and still see the sizes, countdowns and titles. Only the clean HUD key needs the mod on the client.

Settings are saved per world in `hpsize.json` inside the world folder.

---

## 🇵🇱 Po polsku

Każdy mob jest tak duży, jak dużo ma życia. Mob z **20 HP** (tyle co gracz) ma normalny rozmiar, słabsze są mniejsze, silniejsze większe. Kurczak (4 HP) jest malutki (×0,2), a żelazny golem (100 HP) to gigant (×5). **Każdy cios zmniejsza moba**, a leczenie go powiększa.

Do tego komendy ułatwiające nagrywanie filmów: odliczanie ze startem (`/go`), zamrażanie graczy, duże napisy na ekranie (`/announce`), tryb kamery (`/cam`), tryb nagrywania bez spamu komend na czacie (`/recmode`) i klawisz `H` z czystym HUD-em. Wszystkie komunikaty są po polsku, jeśli grasz w polskiej wersji językowej.

Instalacja: Fabric Loader dla 1.21.11 + [Fabric API](https://modrinth.com/mod/fabric-api) w folderze `mods`. Działa też na serwerze, a gracze nie muszą mieć moda u siebie.
