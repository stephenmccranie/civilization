# Colt Paterson prototype

The first playable firearm is the five-chamber Colt Paterson. Its detailed model retains the traced reference handle and separate cylinder, hammer, trigger, barrel and wedge. Find **Colt Paterson** and **.36 Paterson Ammunition** in the Civilization Creative tab or use `/give @s civilization:colt_paterson` and `/give @s civilization:paterson_36_ammunition 20`. Both currently have no crafting recipe.

## Controls and ammunition

Hold the gun in the main hand. **Left-click fires one shot**; release and click again for another. The firearm suppresses ordinary melee and block mining while held. Right-click does not fire. **R reloads**, and the binding is configurable in Minecraft's controls menu. Inventory screens do not send firing or reload actions.

The ammunition icon follows the supplied .36 conical lead projectile photograph, with shallow base bands and no metallic cartridge case. The user selected **one complete ammunition item per chamber**: powder and percussion cap are represented by that item rather than separate inventory ingredients. This is a gameplay abstraction, not a claim that the historical projectile alone was a complete load.

A reload takes eight seconds by default. Its animation withdraws the wedge, moves the barrel off the arbor, removes and rotates the cylinder, then seats the assembly. It fills up to five chambers from available inventory ammunition. Items are consumed only when the reload completes; partial ammunition supplies produce a partial load. Switching away or dying cancels an inventory reload without spending ammunition. Creative players also need and consume inventory ammunition. Existing loaded rounds are retained. Reloading blocks firing; a full gun or an empty ammunition inventory cannot start a reload.

## Projectile behavior and damage

The server validates the held item, living player, ammunition, reload state and cooldown before spawning a bullet. Each accepted shot consumes exactly one chamber, advances the cylinder, broadcasts the extracted firing sound and gives the shooter a small confirmed recoil kick. The default cadence is 16 ticks (0.8 seconds) between shots. Recoil raises the aim approximately 1.2 degrees and briefly moves the held model back; it is applied after server confirmation.

Bullets travel through the world with gravity and drag. Swept collision checks the entire movement segment each tick, so a fast shot can hit targets between endpoints. A solid block stops the bullet before a target behind it. An entity hit deals eight health points (four hearts before ordinary armor and damage rules) by default and removes the bullet. Shots neither break blocks nor penetrate them. Existing claim access checks and vanilla player/team damage rules apply. Bullets expire after 60 ticks or when entering unloaded space; they do not force chunks to load. Their damage, drag, gravity and lifetime are saved with the entity.

Prototype values live in the world's `serverconfig/civilization-firearms.toml`:

| Setting | Default |
| --- | --- |
| `damage` | 8 health points |
| `blocksPerTick` | 5 |
| `gravityPerTick` | 0.025 |
| `velocityRetention` | 0.995 |
| `shotCooldownTicks` | 16 |
| `reloadTicks` | 160 |
| `bulletLifetimeTicks` | 60 |

These are Minecraft tuning values, not a real-world ballistic calibration. This prototype has no headshot multiplier, penetration, crafting progression or full internal lockwork simulation. Production balance, sustained multiplayer load and firing against moving Sable vessels remain unreviewed.

## Sound and verification

The firing sound comes from the user-selected [Shooting the Colt Paterson Revolver.mov by duelist1954](https://www.youtube.com/watch?v=PVV1ksu-2kU), within 9:41–9:45. yt-dlp obtained the audio, then FFmpeg extracted the four-second window. The shot and decay at approximately 9:42.39–9:44.30 became a 1.91-second mono 48 kHz Vorbis sound, with restrained level normalization and short edge fades. Original media, source metadata, preparation scripts and hashes stay in the [local asset](../art/assets/colt_paterson/asset.json); only the game sound is published.

The `paterson` hidden Photon/Faithful scene checks actual R-key reload and left-click packets, inventory conservation, recoil, sound dispatch, held/reload rendering and inventory appearance. Server tests cover partial reloads, cancellation, cadence, swept entity damage, wall obstruction, bullet drop and saved flight age. [Status](../docs/status.md) owns actual results and build labels.
