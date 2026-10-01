# Bedrock feasibility research

Research date: September 10, 2026. Primary sources: Mojang/Microsoft documentation and the maintainers of Geyser and PocketMine. This is a documentation-based assessment, not a running prototype or performance benchmark.

## Recommendation

Test **official Bedrock Dedicated Server (BDS) + one focused behavior pack with scripts + a resource pack** before committing to Java. Our economic design fits a small set of authored facilities and bounded jobs better than a large general-purpose physics simulation. Bedrock is therefore a plausible first candidate, not a proven 200-player platform for this specific world.

This changes the development model: much of the machinery becomes custom add-on work rather than assembling Java mods. No server is installed and no platform choice is final.

## Edition, clients, and release choice

Bedrock is the edition for Windows PCs, Android, and iOS/iPadOS cross-play. Official desktop Bedrock support is Windows; do not promise native macOS/Linux Bedrock clients. Java's official desktop coverage includes those operating systems. [Minecraft edition comparison](https://www.minecraft.net/en-us/store/minecraft-java-bedrock-edition-pc)

Mojang documents joining addressed servers and downloading required resources. A self-hosted server does not have to be a featured Marketplace server. Target a clean Windows client and a clean mobile client for the first joining test. [Server joining guide](https://www.minecraft.net/en-us/article/how-play-minecraft-server)

Console cross-play and easy access to arbitrary private servers are separate matters. Geyser documents console workarounds; do not advertise consoles as having the same simple joining experience as Windows/mobile. [Console access](https://geysermc.org/wiki/geyser/using-geyser-with-consoles/)

The newest retail changelog found is 26.44/45, updated August 20, 2026. It notes platform rollout timing. Start from the current mutually compatible retail client/server releases available on our actual test devices, not Preview/Beta. Do not confuse game version numbers with Script API versions. [Retail hotfix](https://feedback.minecraft.net/hc/en-us/articles/48149564061965-Minecraft-Bedrock-Edition-26-44-45-Hotfix-Changelog)

## Hosting options

| Route | What it means for this project |
| --- | --- |
| Official BDS with add-ons | Recommended first test; official game simulation with documented customization |
| Realms Plus | Too small for the target: owner plus ten other simultaneous players |
| PocketMine-MP | Separate server implementation with plugins; not a drop-in replacement for BDS add-ons |
| Java server with Geyser | Possible alternate direction for compatible server-side gameplay, not a way to run a normal client-modded Java pack on phones |

BDS is available for Windows and Ubuntu/Linux. Its setup guide describes behavior/resource pack installation and server administration. Hosting on Linux does not imply a Linux Bedrock game client. [Download](https://www.minecraft.net/en-us/download/server/bedrock), [BDS setup](https://learn.microsoft.com/en-us/minecraft/creator/documents/bedrockserver/getting-started?view=minecraft-bedrock-stable)

Realms' small simultaneous-player allowance rules it out for production here. [Realms comparison](https://www.minecraft.net/en-us/realms)

PocketMine's authors warn that it lacks vanilla features including redstone and mob AI. Its documentation explicitly says behavior packs are unsupported. A claim of high player capacity is not a compatibility guarantee. [PocketMine overview](https://doc.pmmp.io/en/rtfd/), [Pack support](https://doc.pmmp.io/en/rtfd/resourcepacks.html)

Geyser's FAQ says servers requiring client-installed mods are unsupported. It translates protocols; it does not run Java client mod code on a Bedrock phone. A Geyser route would require its own deliberate content/interaction design. [Geyser FAQ](https://geysermc.org/wiki/geyser/faq/)

## What we can customize

Add-ons provide custom blocks, items, mobs, and recipes. Microsoft's custom-component example includes scripted crops and edible items. This directly supports investigating our agricultural loop. [Add-on capabilities](https://help.minecraft.net/hc/en-us/articles/24120525083533), [Custom components](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/components-tutorial?view=minecraft-bedrock-stable)

BDS supports JavaScript APIs; TypeScript can be compiled to JavaScript. Additional server-specific external-service APIs are experimental, so an external database or web integration must not be assumed to be a stable built-in dependency. [BDS scripting](https://learn.microsoft.com/en-us/minecraft/creator/documents/bedrockserver/scripting?view=minecraft-bedrock-stable)

Player/world dynamic properties persist across restarts. This is a starting point for ownership, energy balances, and jobs, not proof of crash-safe inventory transactions. [Multiplayer scripts](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/multiplayer-scripts?view=minecraft-bedrock-stable)

The UI module offers buttons and modal forms. Prefer those and world interactions for the first mobile UI; do not assume arbitrary desktop-style interfaces are free to implement. [UI API](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server-ui/minecraft-server-ui?view=minecraft-bedrock-stable)

## Mapping our design to Bedrock

Assessments in this table are engineering inferences, not features demonstrated in a prototype.

| Our feature | Assessment | First implementation to investigate |
| --- | --- | --- |
| Calories tied to work | Plausible | Hunger attribute plus action events and measured movement; check interaction with vanilla exhaustion/saturation |
| Crops and fertilizer | Good documented starting point | Custom crop component, edible staple, bounded treatment bonus |
| Finite fuel and universal industrial energy | Plausible custom logic | Fuel-consuming controller and small explicit energy network; audit vanilla fuel bypasses |
| Claims and organizations | Plausible, significant correctness work | Persistent permissions and intervention in relevant actions; test containers, explosions, pistons, automation and indirect griefing |
| Monumental construction | Plausible in stages | Persistent job places supplied blocks gradually and respects permissions |
| Freight | Terminal transfer is plausible; moving machinery needs a separate prototype | Accounted cargo with source/destination permissions, travel time and visible route presentation |
| Regional weather control | Native API is insufficient for spatial weather | Local crop effect plus bounded visual/audio treatment, rather than a new climate engine |
| Sky estates | Static builds are the easiest path | Build within existing dimension bounds and test ground-to-sky visibility on phones |
| Whole moving cities or arbitrary assembled vehicles | High-risk scope | Do not make a general rigid-body/contraption engine a launch dependency |

The documented hunger component extends the attribute component, which exposes value setters. That supports a feasibility test, not unrestricted changes to every survival rule. [Hunger](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/entityhungercomponent?view=minecraft-bedrock-stable), [Attributes](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/entityattributecomponent?view=minecraft-bedrock-stable)

The Dimension API exposes block placement, dimension height bounds, and weather control. `setWeather` applies to the dimension, with no regional radius argument. Block operations can fail outside bounds or unloaded chunks. Therefore local rain and distant construction need explicit designs rather than direct one-call implementations. [Dimension API](https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/minecraft/server/dimension?view=minecraft-bedrock-stable)

Custom dimensions are now documented, but the reviewed tutorial explicitly marks the registration APIs experimental. A “stable” documentation URL can still include experimental members. A separate sky dimension also loses the literal shared view of the ground below. Prefer a same-world test first; verify actual height and render constraints before designing the final terrain. [Custom dimensions tutorial](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/custom-dimension-api-tutorial?view=minecraft-bedrock-stable)

## Scale and maintenance limits

BDS `max-players` accepts a positive integer; its default is ten and the documentation warns that higher values affect performance. Thus 200 is configurable, not demonstrated capacity. The same properties document covers simulation distance and requiring client texture packs. [Server properties](https://learn.microsoft.com/en-us/minecraft/creator/documents/bedrockserver/server-properties?view=minecraft-bedrock-stable)

Microsoft recommends splitting long-running script work and provides `system.runJob` for incremental work under a time budget. Scripts can hit watchdog limits. A construction queue fits this approach better than mass block changes in one tick. A job scheduler alone does not make inventories, restarts, or claims correct. [Scheduling guide](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/system-run-guide?view=minecraft-bedrock-stable)

Stable Script APIs use semantic versioning; beta APIs may break. Prefer stable dependencies, inspect individual experimental flags, and maintain a staging upgrade test. Some documentation examples and version mapping tables lag newer retail releases, so pin against actual installed runtime/type definitions rather than copy arbitrary tutorial versions. [API versioning](https://learn.microsoft.com/en-us/minecraft/creator/documents/scripting/versioning?view=minecraft-bedrock-stable)

Project-specific implications:

- A populated network of several servers is not proof of 200 players sharing one simulated industrial world.
- Profile concentrated crowds and spread-out towns separately; include farms, active machines, freight and construction together.
- Bound active facilities and work per tick. Avoid one entity per decorative machine part or persistent simulation across the whole map.
- Test mobile client frame rate, memory, resource download time, touch interaction and usable render distance separately from server tick time.
- The generated artwork sets an aesthetic target. It does not establish that a phone can render the entire civilization simultaneously.
- Autonomous industrial systems, ownership and transactions are maintenance responsibilities even if their UI is simple.
- A focused add-on may be manageable for one maintainer; a universal physics system plus bespoke MMO infrastructure remains too broad to accept without evidence.

## Proposed decision gate

Run a disposable BDS test using retail Windows and a representative phone/tablet. Do not purchase a large hosting plan or rewrite the whole design first.

1. Join with required custom resources and no separately installed client mod loader.
2. Eat custom food; mine and build while work costs are recorded correctly.
3. Fuel a custom facility; operate it from a mobile-friendly menu.
4. Build a supplied structure gradually, with material accounting and restart recovery.
5. Exercise a protected container and freight transfer concurrently; verify no unauthorized access, duplication or disappearance across reconnect/restart.
6. Demonstrate a bounded agricultural weather effect and a static elevated estate visible from an intended ground viewpoint.
7. Profile representative load and expand toward the 200-concurrent target. Choose numerical budgets from measured results.

Proceed with Bedrock if the interfaces, visuals, core mechanics and maintenance surface pass these tests. Reconsider Java if the indispensable experience turns out to be deeply general moving machinery or if the required Bedrock systems demand unstable engine-level work. No documentation reviewed establishes the full 200-player result in advance.
