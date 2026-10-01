# Boats and vehicle physics


## Airship prototype

The **Airship Controller (Prototype)** is in the Civilization creative tab/search, or available with `/give @s civilization:airship_controller`. It is a Creative/operator testing block with no recipe or nuclear fuel requirement. The controller and cabinet reuse existing art as a placeholder; the whole vessel is made from the player's blocks.

### Build and fly

1. Build a connected structure around one controller, clear of terrain, water and other builds. A temporary support pillar must be removed before inspection. Start with a small wooden deck and controller on top; the controller front points forward.
2. Right-click the controller with an empty hand. Type available power in **prototype watts**, then **Apply** (or Enter while editing). Start at `1e7`; `0`, ordinary decimals and scientific notation such as `1e9` or `1e250` are accepted. Any finite non-negative Java double is allowed, up to approximately `1.7976931348623157e308`; negative values, NaN and infinity are rejected. There is no fuel use or gameplay power ceiling. Applied power is displayed and persists.
3. Choose **Inspect** and review the connected block count, then **Assemble**. A changed block selection requires another inspection. Assembly is limited to 1,024 blocks, within 12 blocks of the controller on each axis. All selected blocks must be accessible and loaded; terrain connections that exceed the bounds fail without moving anything. The scan checks six-face connectivity, not diagonal contact.
4. Right-click the assembled controller and choose **Pilot**. Hold **W/S** for forward/reverse thrust, **A/D** to turn (twice the initial yaw rate), **Space** to climb and the sprint key (**Ctrl** by default) to descend. **Shift** leaves the controls. Release movement keys to brake and hover with available power. Stand within eight blocks to acquire control. Piloting creates a transient invisible attachment at your current position relative to the hull; Sable carries you with the craft instead of relying on deck friction. You remain standing, with no visible seat or extra block. Leaving the controls removes the attachment. Once piloting, temporary rider/ship separation, missing heartbeats and chunk-loading gaps retain the session. Shift or opening a normal screen releases it; a terrain-loading screen does not. Climb/descent command targets are now 9 blocks/s (three times the initial 3), subject to power, mass and drag.

Normal blocks, cut blocks, chests and barrels are supported. Fixed-world controllers, other boat helms, a second airship controller, unsupported block entities, fluids/waterlogged blocks, falling blocks, pistons, TNT, bedrock and portals are rejected. Chest/barrel contents transfer with the craft. Hull placement/breaking is locked while assembled. Stop, leave piloting and open the controller, then choose **Disassemble** to restore ordinary editable blocks at the current controller position, snapped to the nearest cardinal direction. Speed must be at most 0.5 blocks/s, angular speed at most 0.05 radians/s, and the hull nearly level. Every destination block must be loaded, inside the world border/build height, accessible to the owner/operator and air; an obstruction rejects the entire operation before placement. Cargo, controller power, block orientation and mixed cut-cell materials are preserved. Destination copies are completed before original blocks are removed, with destination rollback if copying fails. The vessel and its preparation tickets are removed afterward; Sable returns plot-local entities to world coordinates. Inspect and assemble again after editing. Disassembly does not require ground beneath the ship. Production damage/recovery remains unimplemented. Chest access and power changes remain available while assembled.

### What is simulated

Sable owns the rigid body, collisions, its current block mass and inertia. The drive applies timestep-scaled forces and angular impulses. Upward support has a mass-dependent cost, with leftover power feeding altitude correction, stabilization and propulsion. Underpowered craft cannot sustain hover; **zero power permits falling**. Hover assistance levels the craft, holds an altitude target and brakes horizontal motion gradually. Horizontal force response uses eight times the raw Sable block mass as prototype effective inertia, affecting both thrust and aerodynamic drag while leaving vertical lift/weight and yaw tuning unchanged. Signed propulsion ramps from neutral to full thrust over three seconds; full-forward to full-reverse thrust takes six seconds, passing through neutral. Momentum is retained throughout: reversing thrust does not directly reverse velocity. Releasing movement ramps drive down before hover braking takes over. Normal powered braking has an approximately four-second velocity decay time constant before settling, rather than the former half-second response. This is prototype handling tuning, not a material-mass calibration. Terrain/height safety holds still stop immediately. After braking below 0.5 blocks/s horizontally, it captures a resting position and counteracts known wind force plus position/velocity error within the available propulsion budget. Adequately powered unpiloted ships therefore hold station instead of slowly drifting with the test wind. New thrust releases that resting target; insufficient power still permits drift/falling. Release/disconnect clears thrust/steering/climb, leaving powered hover assistance active. Stale input also clears movement commands and the thrust ramp, but retains the pilot session and existing momentum. Temporary vessel unloads retain that session with neutral inputs; fresh commands resume after reload. Death, dimension changes, lost pilot eligibility/ownership or confirmed controller removal end the session. Pilot sessions are transient and do not survive server restarts.

Directional drag uses projected occupied block columns, air-relative velocity and a quadratic drag term. Columns are cached at assembly; partial blocks currently occupy a whole projected cell. A smooth, gentle global test wind varies with world time; it is not yet regional weather integration. Hull density remains Sable's current properties, not the shared material registry; inventory/tank/passenger mass, fractional cut geometry, calibrated streamlining and pressure torque remain future work. These limitations are shown in the controller where useful.

**No configured speed cap or fixed thrust/drive-acceleration ceiling.** Higher entered power continues to increase propulsion against drag. A finite-power implicit drive step avoids the zero-speed singularity and prevents extreme input from overflowing the normal thrust calculation; altitude and attitude corrections remain bounded for controllability. Full-authority yaw now targets about 1 radian/s (57 degrees/s), double the initial 0.5 radian/s. The former 48-block/s speed cap and 20-block/s² drive clamp are removed.

Fast flight uses an immutable forward terrain snapshot, published once per server tick and consumed consistently for each physics step. A 9-by-9 core and padded forward corridor cover the complete prototype hull. Preparation uses a 28-block half-width along current velocity, with a predicted turn branch widening to 36 blocks after the first 128 blocks. The forecast uses current momentum, facing, yaw rate, steering and available drive acceleration; it is a constant-speed approximation, not a second physics simulation. Turning reserves the first 640 cells for current momentum before adding the curved branch within the same total budget. Straight flight can spend the entire budget on its narrower corridor. Lookahead grows with speed, launch power and observed request-to-ready delay (bounded to 0.15-0.6 seconds), retaining a 0.5-second lead allowance plus 128 blocks. Geometry remains capped at 4,096 blocks. Primitive-long hash collections keep packed chunk coordinates from causing long hash-probe chains; snapshots remain defensive and immutable. Our expiring FULL-status tickets prepare terrain without ticking the entire corridor: at most 1,024 direct tickets per ship and 4,096 per world, renewed every 20 ticks with a 40-tick lease. Minecraft's dependency chunks are additional. At most 128 new direct tickets per ship are admitted per tick, in corridor order; existing desired tickets remain and obsolete predictions are released. A full initial corridor therefore takes multiple ticks to admit. Turning replaces the predicted path; release/unload removes our tickets and pending latency observations. Forecast cells alone never qualify as ready. Existing player, simulation and C2ME coverage remains intact. Reads use already available chunks, without synchronous chunk loads or waiting on futures.

As ready terrain runs short, the drive anticipates the boundary and eases speed down, recovering as coverage arrives. This is a readiness governor, not a configured cruising-speed cap. Geometry queries stop at 4,096 blocks and all work has finite budgets. Unexpected missing coverage, invalid numerics, height limits or exhausted collision-work budgets retain the emergency **unloaded-terrain / height hold**. Ordinary thrust and inertia remain unchanged when clearance is sufficient.

Sable 2.0.5 predicts terrain vertically, and its custom terrain dispatcher does not implement continuous shape casts. Civilization therefore prepares horizontal collision sections through Sable's public ticket API and checks continuous translation of the ship's world bounding box against loaded block collision shapes. It shortens a step before a solid shape can be skipped; Sable still handles existing contacts. This conservative hull box can brake beside terrain that would fit into an irregular hull's empty space. Per-step limits are 128 nonempty sections and 32,768 inspected blocks; exceeding them holds safely. Empty air sections require no block scans. This does not certify extreme-speed rotation, moving-vessel collisions or complex modded collision shapes; dense terrain and multiplayer require separate trials. It is not infinite loaded distance or a guarantee of 5,000-block/s rendering.

Access is owner/operator-based for piloting and onboard manual containers, with Creative/operator access required for the test controller. Claim queries transform airship positions into world space. This is not a multiplayer security release: fragmentation, explosions, automation, crew policy and adversarial interactions still need the broader vehicle audit.

### Verification

`./dev.ps1 Verify -Scope Full -Scene airship` runs build/unit and server checks plus a hidden Photon scene. The scene operates the real menu, applies extreme numeric power, assembles a freeform deck, drives/climbs/turns through client inputs, checks hover braking and cargo, and captures the controller and flying platform. `./airship-check.ps1` runs two dedicated processes in disposable `runs/airship-server` on localhost port 25570, checking the same UUID, hull, owner, exact `1e250` power and 23 steel after graceful restart. Development fixtures are excluded from the production JAR. Actual results/build labels live in [status](../docs/status.md).

## Freeform motor vessels

Survival boats use **physical Hot-Bulb Engines**, refined fuel and lubricating oil. They are working Sable vessels made from ordinary movable blocks, cut blocks, supported machinery and cargo. Sable 2.0.5 is required; Create and Aeronautics are not used. Current validation belongs in [project status](../docs/status.md).

### Build and launch

Craft a **Small Boat Helm** with `SPS / PWP / S S`: S = Steel Ingot, P = Machine Parts, W = wooden planks. The item places a two-block-wide wood-and-brass console with an upright wheel and four-position engine lever. The wheel side is the assembly root and its facing determines the bow.

Build any face-connected hull around it at the waterline. Empty-hand right-click the wheel to launch. The connected scan requires one complete Helm, at least eight supported blocks, at least four blocks with water two blocks deep below them, loaded chunks and land access. It refuses fluids/waterlogged blocks, falling blocks, pistons, TNT, bedrock, portals, extra helms, civic machinery and unsupported block entities. A **4,096-block safety cap** catches boats still touching terrain or docks. There is no hull template or rectangular size band.

After launch, owners may place supported blocks against any vessel face and remove any vessel block except the Helm. Ordinary blocks, cut pieces, chests, barrels, Hot-Bulb Engines, Coal Bunkers and Cargo Tanks are supported. The vessel can grow in any direction within Sable/world limits. Detached fragments without the physical Helm are ignored by boat controls. Decommissioning/repacking, crew whitelists and ownership transfer remain open.

### Fuel, oil and controls

- Empty-hand right-click to pilot. **W/S** move the lever one setting up/down per press: **reverse → neutral → slow → fast**. The chosen gear holds when keys are released. **A/D** steer; **Shift** leaves the helm without changing throttle; opening a screen also releases control. Steering returns to center and the boat continues in its selected gear. Shift to **neutral** before leaving if you want to stop. Clicking either console half can acquire the helm but never changes gear. Only W/S changes the throttle; the lever model shows the current position. Sneak-right-click with an empty hand inspects reserves without piloting.
- The action bar shows selected gear, discovered engine count, combined output and vessel mass. Leaving, disconnecting, dying, moving away or stale control packets release the pilot and center steering without changing gear. The selected gear persists in the console block state and restores on reload; pilot/steering input does not persist. Loaded, water-supported boats keep moving and consuming from their running engines without a pilot. Unloaded boats do not simulate travel or fuel use.
- Build, fuel, lubricate and start one or more Hot-Bulb Engines aboard. Every complete enabled engine answers the same demand and adds its condition-adjusted output. The Helm contains no fuel, oil or condition state.

The original 31-block hull with one full-condition engine remains the tuning reference: roughly **14 blocks/s fast, 6 slow and 3 reverse** when unobstructed. Actual acceleration and terminal speed fall as physical block mass rises; steering authority also weakens with mass. Additional running engines add thrust linearly. Neutral brakes without engine work or powered steering. Each engine's existing work rates apply: full demand consumes 2 mB refined fuel and 0.1 mB lubricant per working second; slow uses 6/14 and reverse 3/14 of full demand. Cargo quantities inside inventories/tanks do not yet add dynamic mass.

### Physics, custody and limits

A water-surface spring, upright stabilization and drag provide simple handling. There is no diagnostic hover over land: without sampled water, the boat has no buoyant support. Shore contact uses Sable collision; detailed beaching, slope recovery and rough-water behavior still need broader testing. Vessel propulsion pauses if surrounding chunks are not already loaded; the gameplay code does not force-load them. Sable's own tracking/loading behavior remains part of load testing.

Boat scans and interaction lookup ignore Sable objects with absent or unrelated custom metadata, including untagged fragments; these are not assigned engines or fabricated ownership. This fixes the physics-tick crash observed while removing a boat block. Detached-fragment custody/recovery remains part of the unfinished vehicle lifecycle.

Engine state, owner/helm metadata and supported cargo persist through Sable's vessel storage. Chest/barrel contents remain ordinary block-entity inventories. Engine and bulk-store menus measure reach at transformed world positions. Manual onboard access uses boat custody, while claim queries for automated boundaries also use transformed positions. This is not a completed adversarial multiplayer audit: explosions, every automation path, multiple clients, crew access and production-scale loading remain work.

### Verification

`./dev.ps1 Verify -Scope Full -Scene boat` runs the ordinary suite and a hidden Photon client driving, steering and stopping a loaded boat through real input packets. The scene places a real block beyond the former width band, checks its physical engine consumes both products and keeps chest cargo intact. `./boat-check.ps1` uses two dedicated processes on localhost port **25568**, in disposable `runs/boat-server`, to verify the same vessel UUID, exact engine reserves/condition and 23 steel in a chest after restart. These are graceful-save checks, not crash-atomic guarantees.

## Optional diagnostic laboratory

The older laboratory remains an optional diagnostic integration, separate from survival controls. Its classes and commands stay excluded from the production JAR. Its hover platform is not the survival boat. Dependency/license research remains in [open decisions](../open_decisions.md#vehicle-physics-dependency-research--2026-09-10).

## Reproduce

From the project root, with the existing `dev.local.json`:

```powershell
./civilization-mod/sable.ps1 Prepare
./civilization-mod/sable.ps1 Compile
./civilization-mod/sable.ps1 Verify
./civilization-mod/sable.ps1 Visual
./civilization-mod/sable.ps1 Visual -Graphics
```

`Verify` starts a dedicated server, drives/steers/brakes a platform, saves it, stops, and starts another server process to check the same vessel UUID, 23 Coal and mixed cut-block cells. Each phase must print an explicit pass marker. This is separate from the normal GameTest suite. It uses `runs/sable-server`, port **25567**, with localhost binding and a disposable `sable-lab` world. That world includes forced test chunks; this is test scaffolding, not our future vehicle chunk policy. The existing development server's EULA acknowledgement is reused.

Visual runs remain hidden at **1920×1080**, with mouse capture disabled. They copy the laboratory world once into separate baseline and graphics directories. Graphics mode copies Sodium, Iris, Distant Horizons, resource packs and shader packs from Prism read-only; it does not alter Prism. It uses the copied Iris selection. Screenshots and explicit results remain under `runs/sable-visual` or `runs/sable-visual-graphics`. Baseline and graphics runs never share a mods directory.

`Server` starts the isolated server interactively. An operator using a matching development client can use:

| Command | Purpose |
| --- | --- |
| `/civphysics spawn` | Create a 5×3 test deck, chest and mixed cut block in empty loaded space six blocks beside and three above the player |
| `/civphysics drive <throttle> <steer>` | Set the nearest loaded lab platform's inputs; both values range from -1 to 1 |
| `/civphysics brake` | Dampen motion and stop thrust |
| `/civphysics board` | Teleport onto the nearest lab platform for testing |
| `/civphysics status` | Report UUID, speed and body mass |

Commands exist only with `-PsableLab=true`; they are operator diagnostics, not player controls or a vehicle permission system. Inputs reset to braking when the process restarts. The example has artificial hover support to isolate movement and persistence. It consumes no fuel and does not demonstrate buoyancy, balanced propulsion or an approved airship lift mechanic.

The [shared physical foundation](physics.md) now provides a read-only coordinate/mass adapter and verified static-property proof. It does not yet override live boat or cargo mass.

## Dependency boundary

The launcher downloads the pinned official release and checks its SHA-512 before use:

- Sable NeoForge **2.0.5**, Minecraft **1.21.1**, Modrinth version `U678xqle`.
- Release SHA-512: `bf3d8c87bcc5efb99afffd50305fc978086ed48a63e106816e3a8a3901f8052f4480ea527dbd7d4003f7775ed4d020529098034f4e307aefac9cc42df2b4c19a`.
- Bundled Sable Companion **1.6.0**, Veil **4.3.2**, and Sable Rapier native module **2.0.5**. The outer release JAR loads these at runtime; extracted copies are compile-only where needed.
- JDK 21, NeoForge 21.1.250, Windows x64 exercised locally. Linux deployment/native loading remains unverified.

JARs are kept in ignored `.dev-libs`. The normal build now compiles against pinned Sable; `dev.ps1` prepares/hash-checks it and deploys its external release JAR alongside Civilization. `src/sableLab` is still opt-in. `BoatSystem` owns survival assembly/physics; `SableAdapter` remains the separate diagnostic example.

Sources: [official release](https://modrinth.com/mod/sable/version/U678xqle), [developer setup](https://github.com/ryanhcode/sable/blob/main/wiki/Home.md), [Companion coordinate guidance](https://github.com/ryanhcode/sable-companion). Actual compilation against the release JAR, rather than current upstream source alone, determines the example's API compatibility.

## Working code examples

Use [SableAdapter.java](src/sableLab/java/dev/civilization/lab/SableAdapter.java) as the executable reference rather than maintaining copied snippets:

- **Assembly:** `SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, bounds)` transfers a bounded explicit set of blocks and their block entities. The example creates only known test blocks; it cannot scoop up land controllers or arbitrary neighboring infrastructure.
- **Coordinates:** keep chest/cut positions in plot-local coordinates; transform a deck position with `ship.logicalPose().transformPosition(...)` for boarding. The two coordinate spaces are not interchangeable.
- **Physics:** respond to `ForgeSablePrePhysicsTickEvent`, using its timestep. `RigidBodyHandle.applyLinearImpulse` and `applyAngularImpulse` expect impulses in the local frame. Convert world corrections into that frame and multiply force/torque by the actual timestep. Do not apply the full per-tick impulse once per substep.
- **Persistence:** vessel metadata goes through `getUserDataTag`/`setUserDataTag`; Sable persists the platform and normal chest/cut block entities. Throttle is deliberately transient. This demonstrates ordinary graceful saves, not crash-atomic transactions.
- **Controls:** [SableLab.java](src/sableLab/java/dev/civilization/lab/SableLab.java) contains commands and the two-process check. [SableVisual.java](src/sableLab/java/dev/civilization/lab/SableVisual.java) contains boarding, survival movement observations, chest synchronization and screenshots.

## Remaining integration work

| Area | Required work |
| --- | --- |
| Calories | Initial survival observation passed: passive transport is effectively free, walking still costs calories. Keep the diagnostic fixture and broaden to sprinting, rotation and boarding transitions |
| Cargo | Deferred: normal chests/barrels are the candidate; liquid oil transport comes later. Decide whether cargo affects handling at all before implementing inventory-derived mass |
| Cut blocks | Mixed materials and occupied cells survive transfer/reload; verify collision. Fractional mass/volume is needed only if the chosen simplified vessel rules use it |
| Claims | Project moving interaction/collision positions into world coordinates; define ship custody separately from fixed land; keep controllers off ships |
| Other systems | Audit map markers, geography queries, multiblock guides and direct distance checks before allowing affected blocks aboard |
| Boats | Freeform water-supported vessels, built-in controls, physical engines and block-mass handling implemented above. Broaden shore/collision and route checks; content mass, docking and decommissioning remain |
| Multiplayer | Test several real clients, latency, disconnect/rejoin aboard, unload/reload and cargo exchanges |
| Scale | Bound vessel complexity and active physics; profile many ships and fast travel before claiming 200-player readiness |

Known upstream startup diagnostics include an absent `create:flywheel` physics-property reference with Create omitted. Baseline also logs an absent optional Iris class. Record these rather than claiming warning-free startup; they did not prevent the tested movement/save/load paths. Shader screenshots must be inspected: a successful client exit alone does not certify rendering, and Photon's cloud layer obscured the initial high-altitude fixture.

## Speed-adaptive Distant Horizons detail

Civilization optionally adjusts DH 3.3.1 section selection on the client, without modifying its JAR, saved LOD data or configured render distance. Normal travel preserves configured quality. Camera translation uses a short 0.25-second smoothing time constant and a single warp threshold at 80 blocks/s. Warp requests exactly two fixed horizontal detail levels: level 2 (4-block-wide cells) within approximately 32 chunks / 512 blocks, and level 6 (64-block-wide cells) farther away. There is no full-detail protected region and no progressive distance curve inside either band. Ordinary terrain rendering follows the warp rendering policy below. Warp detail uses physical horizontal distance, independent of camera zoom; outside warp DH retains its normal zoom behavior. Camera rotation alone does not lower quality.

The boundary has 32 blocks of hysteresis: an existing near node stays near until beyond 544 blocks, while a far node becomes near at 480 blocks or closer. New nodes use 512 blocks. Parent sections intersecting the near radius are subdivided conservatively using their bounding-circle extent so a coarse parent cannot hide the near band. This makes the visible boundary approximate rather than a precise circular cut. Node history is local to each rendering tree, retained across consecutive traversals, bounded to 16,384 entries per traversal, and cleared outside warp. DH still builds replacement sections and can retain previous/intermediate detail during handoff; two requested qualities do not make boundary crossings free.

Normal quality returns after one continuous second below the same 80 blocks/s threshold. Crossing it again cancels the recovery timer, avoiding repeated quality changes around the boundary. Pausing, changing worlds, sample gaps over half a second and single-sample camera jumps over 4,096 blocks reset the policy. The camera-jump rule also resets unusually large legitimate movement samples; it never caps ship velocity. Normal section replacements still cost work during transitions; this does not promise zero rebuilding or eliminate vanilla chunk-generation limits.

The compatibility mixin is client-only and activates only for DH 3.3.1 with the expected hook signatures. DH absence or an unverified version leaves normal behavior, with a diagnostic in the log. Quality policy is captured once per quadtree traversal; DH retains its section handoff logic. While warp mode is active, its existing zoom-transition fallback also accepts finer sections until the selected replacement is ready; this handles the discontinuity between the two requested levels without changing the camera itself. No global quality overrides, cache flushes, queue cancellation or biome-color patches are applied.

### Warp terrain rendering

With verified DH 3.3.1 and Sodium 0.8.13, warp mode skips ordinary terrain draw calls and defers submission of new ordinary terrain mesh/sort jobs. Sable vessel drawing and compilation use a separate renderer and continue, as do entities, players, block entities, chunk reception/unloading, server simulation and physics. Existing in-flight mesh work can finish and be cleaned up. Configured client/server render and simulation distances are unchanged.

DH's near clipping distance is reduced to 0.5 blocks during warp. Photon also has a separate shader fade that hides nearby LOD terrain and water based on vanilla render distance. With Iris 1.8.14, Civilization adds a runtime warp uniform to that exact fade expression in the DH shader programs: warp bypasses the fade, normal flight restores it. Other shader expressions, projection distances, and shader-pack files are unchanged. This lets existing LODs cover the normally vanilla-rendered area; it does not generate missing LOD data. Terrain suppression is permitted only after DH's normal LOD render call completes for the frame; disabled/cancelled DH rendering falls back to ordinary terrain. Mesh scheduling runs earlier in the frame, so it requires a successful DH render within the previous 250 ms in the same world; pausing/disconnecting/world changes clear this state. This checks the render path, not completeness of LOD data: newly visited areas may still have missing LODs until DH builds them. Normal terrain drawing and mesh submission resume with warp exit; terrain that changed during flight must rebuild. These hooks do not clear mesh caches or change saved DH settings. Unsupported renderer versions keep ordinary terrain behavior.

### Directional chunk request order

With C2ME NeoForge `0.4.0-alpha.0.122+1.21.1`, piloted airships moving horizontally at least 80 blocks/s prioritize pending no-tick chunk requests along their actual velocity. Newly created C2ME source iterators near the ship request the central 5-by-5 chunk area first, then a forward corridor with a 2.5-chunk half-width, then remaining chunks by distance. Direction is quantized into 16 headings. This changes request order only: the full square footprint, retention/unload predicate, concurrent-load limit, round-robin source sharing, generation dependencies, simulation tickets and airship collision checks remain unchanged. This ordering integration itself issues no extra tickets. The separate airship readiness corridor described above adds bounded FULL-status ahead tickets without narrowing C2ME or player coverage.

The server publishes immutable position/velocity hints keyed by its chunk map; worker threads do not access live ships or players. Hints expire after one second without publication and clear on server stop. A source within three chunks of a piloted ship uses the nearest ship's heading; shared sources still retain their complete footprint. New source iterators pick up changes as the ship crosses chunks. Existing iterators and already-submitted generation finish normally; stopping does not cancel that work. Normal sources retain C2ME's spiral order. Cached orders are bounded to 32 radius/heading combinations, and view radii above 64 chunks fall back to the original iterator.

This optional compatibility requires the exact inspected C2ME module version and both hook signatures. C2ME absence or an unsupported release leaves ordinary scheduling. It can improve time until useful chunks arrive, but does not guarantee lower total CPU or support unlimited generation throughput. Narrowing the requested footprint remains a separate [experiment](../open_decisions.md#directional-chunk-loading-for-warp-travel).
