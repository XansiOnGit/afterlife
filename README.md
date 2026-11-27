# Afterlife 2.5.1

Afterlife server side, can and will be subject to change.

## Features

- **Cooldowns:** the mace smash, spear Lunge and Riptide trident each get a configurable cooldown, which only applies if you're in combat (mace smashes always trigger it).
- **Shields** can't be enchanted with Unbreaking.
- **Maces** can't get Breach or Density
- **Ghost Bell:** a bell tolls for nearby players when someone dies. Players can turn it off in Options > Accessibility Settings.
- **8:00 Weaving:** brew a Potion of Weaving with redstone for an 8 minute version. Gunpowder turns it into a splash potion, in either order.
- **HUD heart:** new hud icon of a heart display on client side requires a server however to talk and repeat back to itself.

## Commands

All commands need operator permission.

| Command | What it does |
|---|---|
| `/afterlife cooldown` | Shows the current cooldowns |
| `/afterlife cooldown mace <seconds>` | Sets the mace cooldown (0 = off) |
| `/afterlife cooldown lunge <seconds>` | Sets the Lunge cooldown (0 = off) |
| `/afterlife cooldown riptide <seconds>` | Sets the Riptide cooldown (0 = off) |
| `/afterlife cooldown get <player>` | Shows a player's remaining cooldowns |
| `/afterlife cooldown reset <player>` | Clears a player's cooldowns |
| `/afterlife tag godmace [player]` | Makes the held mace the one and only god mace |
| `/afterlife tag clear` | Removes the god mace status |

## Config

Saved in `config/afterlife.json`. Defaults:

- `maceCooldownSeconds`: 30
- `lungeCooldownSeconds`: 3
- `riptideCooldownSeconds`: 60
- `bellRadius`: 100 (blocks)

## Installing

1. Install [Fabric Loader](https://fabricmc.net/use/) and Fabric API for 1.21.11.
2. Put the Afterlife jar in your `mods` folder.

## Issues

If something isn't working as intended, please open a report in the Issues tab.

Proprietary License - All Rights Reserved
