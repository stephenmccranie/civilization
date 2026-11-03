# Firearms

The first firearm is the five-chamber **Colt Paterson**. Find it and **.36 Paterson Ammunition** in the Civilization creative tab, or use `/give @s civilization:colt_paterson` and `/give @s civilization:paterson_36_ammunition 20`; neither has a recipe. The model keeps the traced reference handle with separate cylinder, hammer, trigger, barrel and wedge.

## Controls and ammunition

Hold the gun in the main hand. **Tap R to cock** the hammer and unfold the trigger before each shot (six ticks); cocking indexes the cylinder to the next loaded chamber. **Left-click fires** the cocked shot and lowers the hammer; a loaded but uncocked gun cannot fire. While held, the gun suppresses melee and block mining; right-click does not fire. **R reloads an empty gun; hold R for 0.6 seconds to reload a partly loaded cylinder** (releasing sooner cocks instead). The key is rebindable in Minecraft's controls; inventory screens send no firing or reload actions.

One ammunition item loads one chamber; powder and cap are abstracted into it. A reload takes eight seconds by default: the animation withdraws the wedge, slides the barrel off the arbor, removes and rotates the cylinder and seats it again. It fills up to five chambers from inventory ammunition, consumed only when the reload completes (partial supplies give a partial load), and keeps rounds already loaded. Switching away or dying cancels without spending ammunition. Creative players also need and consume ammunition. Reloading lowers the hammer and leaves the gun uncocked; it cannot start with a full gun or no ammunition, and it blocks firing.

## Shots and damage

The server checks the held item, a living player, ammunition, reload state and cooldown before spawning a bullet. Each shot consumes one chamber, plays the firing sound and, after confirmation, kicks the aim up about 1.2 degrees and briefly moves the held model back. Shots are at least 16 ticks apart. The shooter hears one centered sound that cannot shift with movement or view; other players hear it spatially at the firing location.

Bullets fly with gravity and drag, and swept collision checks each tick's whole segment, so fast shots hit targets between endpoints and a solid block stops a bullet before anything behind it. An entity hit deals eight health points (four hearts before armor and damage rules) and removes the bullet; bullets never break or penetrate blocks. Claim access and vanilla player/team damage rules apply. Bullets expire after 60 ticks or on entering unloaded space, never loading chunks; their damage, drag, gravity and age are saved.

`serverconfig/civilization-firearms.toml`:

| Setting | Default |
| --- | --- |
| `damage` | 8 health points |
| `blocksPerTick` | 5 |
| `gravityPerTick` | 0.025 |
| `velocityRetention` | 0.995 |
| `shotCooldownTicks` | 16 |
| `reloadTicks` | 160 |
| `bulletLifetimeTicks` | 60 |

These are game tuning values, not ballistic calibration. There is no headshot multiplier, penetration, crafting route or internal lockwork simulation.

## Presentation

The firing sound is a 1.91-second mono 48 kHz Vorbis clip of the shot and decay from [Shooting the Colt Paterson Revolver.mov by duelist1954](https://www.youtube.com/watch?v=PVV1ksu-2kU) (about 9:42.4–9:44.3), level-normalized with short fades. Held first- and third-person views are 30% larger than inventory scale. The model uses smooth cylinder and rear-shield curves, a rounded walnut grip, shaped frame contours and slotted fasteners in weathered gray steel at 64 pixels per block; faces buried within the same moving bone are removed, while surfaces exposed during reload remain.
