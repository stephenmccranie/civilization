# Vehicles

Boats and the prototype airship are [Sable](#sable) rigid bodies built from ordinary blocks. Boats are the Survival vehicle; the airship is a Creative test platform for fast flight, which also drives the optional [Distant Horizons](#distant-horizons-detail) and [C2ME](#directional-chunk-loading) integrations.

## Boats

Survival boats are freeform Sable vessels powered by physical [Hot-Bulb Engines](industry.md#hot-bulb-engine) burning refined fuel and lubricating oil.

### Build and launch

Assemble a **Small Boat Helm** at the Assembly Bench from **4 Steel Plates + 3 Machine Parts + 1 plank** (ten seconds). It places a two-block-wide wood-and-brass console with an upright wheel and a four-position engine lever; holding it previews both halves where they would go. The wheel side is the assembly root and its facing sets the bow.

Build any face-connected hull around the Helm at the waterline and empty-hand right-click the wheel to launch. The scan needs one complete Helm, at least eight supported blocks, at least four blocks with water two blocks deep below them, loaded chunks and land access. It refuses fluids and waterlogged blocks, falling blocks, pistons, TNT, bedrock, portals, extra helms, civic machinery and unsupported block entities. A **4,096-block cap** catches hulls still touching terrain or docks. There is no hull template or size band.

Supported blocks are ordinary blocks, cut pieces, chests, barrels, Hot-Bulb Engines, Coal Bunkers and Cargo Tanks. After launch the owner may place supported blocks against any vessel face and remove any vessel block except the Helm, so the vessel can grow in any direction. Detached fragments without the Helm are ignored by boat controls.

### Controls and power

- Empty-hand right-click the wheel to pilot. **W/S** move the lever one setting per press through **reverse → neutral → slow → fast**; the gear holds when keys are released and the lever model shows it. **A/D** steer, and steering recenters when released. **Shift** leaves the helm without changing gear, as does opening a screen; shift to **neutral** first to stop. Clicking either console half can take the helm but never changes gear. Sneak-right-click with an empty hand inspects reserves without piloting.
- The action bar shows gear, engine count, combined output and vessel mass. Leaving, disconnecting, dying, moving away or stale control packets release the pilot and center the steering without changing gear. The gear is saved in the console block state; pilot and steering input are not saved.
- Every complete, running engine aboard answers the same demand and adds its condition-adjusted output; the Helm holds no fuel or state. Full demand costs each engine 2 mB fuel and 0.1 mB lubricant per working second; slow uses 6/14 and reverse 3/14 of that. Neutral brakes without engine work, though each started engine still idles on 0.2 mB fuel/s. Loaded, water-supported boats keep moving and consuming in their gear without a pilot; unloaded boats neither travel nor burn fuel.

A 31-block hull with one full-condition engine runs roughly **14 blocks/s fast, 6 slow and 3 reverse** unobstructed. Acceleration, terminal speed and steering authority fall as Sable block mass rises; extra engines add thrust linearly. Cargo inside inventories and tanks adds no mass.

### Physics and custody

A water-surface spring, upright stabilization and drag give simple handling. Without water below there is no buoyant support. Shore contact uses Sable collision. Propulsion pauses if surrounding chunks are not loaded; boat code never force-loads.

Engine state, owner and helm metadata and supported cargo persist in Sable's vessel storage; chest and barrel contents are ordinary block-entity inventories. Engine and bulk-store menus measure reach at transformed world positions. Manual onboard access follows boat ownership, and automated claim checks use transformed positions. Decommissioning, crew access, ownership transfer, detached-fragment recovery and adversarial multiplayer cases (explosions, every automation path) are not covered.

## Airship prototype

The **Airship Controller (Prototype)** is a Creative/operator test block (Civilization creative tab or `/give @s civilization:airship_controller`) with no recipe or fuel; the vessel is made of the player's blocks.

1. Build a connected structure around one controller, clear of terrain, water and other builds (remove temporary supports before inspecting). The controller front points forward.
2. Empty-hand right-click the controller, type available power in **prototype watts** and press **Apply** (or Enter). Any finite non-negative double up to about `1.7976931348623157e308` is accepted, including scientific notation such as `1e9`; negative values, NaN and infinity are rejected. `1e7` is a good start. Power persists; there is no fuel use or power ceiling.
3. Choose **Inspect**, review the block count, then **Assemble**; any change needs a new inspection. Assembly takes at most 1,024 six-face-connected blocks within 12 blocks of the controller on each axis, all loaded and accessible; over-limit terrain connections fail without moving anything.
4. Right-click the assembled controller and choose **Pilot** from within eight blocks. **W/S** thrust, **A/D** turn, **Space** climbs and the sprint key (**Ctrl**) descends; **Shift** or opening a normal screen leaves. Releasing keys brakes and hovers on available power. Piloting attaches you invisibly to the hull at your current position, so Sable carries you standing. Brief separation, missing heartbeats and chunk-loading gaps keep the session; death, dimension change, lost ownership or controller removal end it, and sessions do not survive restarts.

Normal blocks, cut blocks, chests and barrels are supported; chest contents travel with the craft. Fixed-world controllers, boat helms, a second airship controller, other block entities, fluids and waterlogged blocks, falling blocks, pistons, TNT, bedrock and portals are rejected. Hull placement and breaking are locked while assembled; chest access and power changes remain.

**Disassemble** (from the controller screen, not while piloting) restores editable blocks at the controller's position snapped to the nearest cardinal direction. Speed must be at most 0.5 blocks/s, rotation at most 0.05 rad/s and the hull nearly level; it need not be grounded. Every destination must be loaded, in bounds, accessible and empty, or nothing moves. Cargo, power, orientation and mixed cut cells are preserved; copies complete before originals are removed, with rollback on failure.

### Flight model

Sable owns the rigid body, collisions, block mass and inertia. Upward support has a mass-dependent cost; leftover power feeds altitude hold, stabilization and propulsion, so underpowered craft sink and **zero power falls**. Horizontal response uses eight times Sable's block mass as effective inertia for thrust and drag. Thrust ramps from neutral to full over three seconds (six from full forward to full reverse), momentum is kept, and powered braking decays velocity with about a four-second time constant. Below 0.5 blocks/s the craft captures a resting position and holds station against the test wind within its propulsion budget. Full yaw authority targets about 1 rad/s; climb and descent target 9 blocks/s, subject to power, mass and drag.

Drag is quadratic over projected occupied block columns cached at assembly (partial blocks count as whole cells) using air-relative velocity. A smooth global test wind varies with world time. Hull density is Sable's; inventory, passenger and fractional cut mass are not included. There is no speed cap: more power keeps raising speed against drag, and an implicit finite-power drive step avoids overflow while altitude and attitude corrections stay bounded.

Piloting and onboard containers are owner/operator-only; claim queries use transformed world positions. This is a test platform, not a multiplayer security boundary.

### Fast flight and terrain readiness

The server publishes an immutable forward terrain snapshot once per tick, used consistently by each physics step. It covers a 9×9 core and a corridor along current velocity (28-block half-width, widening to 36 along a predicted turn after the first 128 blocks); turning reserves the first 640 cells for current momentum. Lookahead grows with speed, power and observed load latency (0.15–0.6 s) plus a 0.5-second lead and 128 blocks, capped at 4,096 blocks of geometry. Civilization prepares that terrain with expiring FULL-status tickets — at most 1,024 per ship and 4,096 per world, renewed every 20 ticks on a 40-tick lease, admitting at most 128 new tickets per ship per tick in corridor order — and releases them on turns, unload and release. Reads use only already-available chunks, never synchronous loads.

As ready terrain runs short the drive eases speed down and recovers as coverage arrives; this readiness governor is not a cruising cap. Sable 2.0.5 predicts terrain vertically and has no continuous shape casts, so Civilization sweeps the ship's world bounding box against loaded collision shapes and shortens any step that would skip a solid; the conservative box can brake beside terrain an irregular hull would fit past. Per step it inspects at most 128 nonempty sections and 32,768 blocks. Missing coverage, invalid numerics, height limits or exhausted collision budgets trigger an emergency **unloaded-terrain / height hold**.

**Measured limits.** At very high speed the governor waits mainly on server lighting: about 70% of missing forward chunks were waiting for the light stage, while reading them took well under a millisecond. Client time goes mostly to Distant Horizons (about 60%, chiefly mesh building and chunk-to-LOD conversion); airship server code is under 0.5%. Smaller lighting batches, a corridor lighting priority, larger buffers and a second DH worker either did not help or traded away frame rate, terrain or heap, so the governor stays as it is. An 8 GiB client heap removed the 100 ms stalls seen at 4 GiB. Sable declares ScalableLux incompatible.

## Distant Horizons detail

With a verified Distant Horizons on the client ([version gates](README.md#optional-mods)), fast travel lowers distant detail and restores it afterward without modifying DH's JAR, saved LOD data or configured render distance. Camera speed is smoothed with a 0.25-second time constant against one **warp threshold of 80 blocks/s**; rotation alone never lowers quality. In warp DH requests exactly two horizontal detail levels: level 2 (4-block cells) within about 512 blocks and level 6 (64-block cells) beyond, by physical distance regardless of zoom. The boundary has 32 blocks of hysteresis (existing near nodes stay near to 544 blocks; far nodes become near at 480), and parents intersecting the near radius are subdivided by their bounding circle. Node history is per rendering tree, bounded to 16,384 entries and cleared outside warp.

Normal quality returns after one continuous second below 80 blocks/s; crossing again cancels the timer. Pausing, changing worlds, sample gaps over half a second and single-sample camera jumps over 4,096 blocks reset the policy (this never limits ship speed). DH keeps its own section handoff; in warp its zoom-transition fallback also accepts finer sections until the replacement is ready. No global quality overrides, cache flushes or queue cancellation are applied, and section replacements still cost work.

### Warp terrain rendering

With verified Sodium as well, warp skips ordinary terrain draw calls and defers new terrain mesh and sort jobs; Sable vessels, entities, block entities, chunk transfer, simulation and in-flight mesh work continue, and render and simulation distances are unchanged. DH's near clip drops to 0.5 blocks. With verified Iris, a warp uniform added to the DH shader programs' vanilla-distance fade lets Photon draw LODs over the normally vanilla-rendered area; normal flight restores the fade. Terrain is suppressed only after DH's LOD pass completed for the frame (mesh scheduling requires a successful DH render within the last 250 ms in the same world), so disabled or canceled DH rendering falls back to ordinary terrain. Missing LOD data is not generated, and terrain that changed during warp rebuilds on exit.

## Directional chunk loading

With the verified C2ME build on the server, piloted airships moving horizontally at **80 blocks/s or more** reorder pending no-tick chunk requests along their velocity: newly created source iterators near the ship request the central 5×5 chunks first, then a forward corridor with a 2.5-chunk half-width, then the rest by distance, with direction quantized to 16 headings. Only order changes: footprint, retention, concurrency limits, round-robin sharing, generation dependencies, simulation tickets and collision checks are untouched, and this integration issues no tickets of its own.

The server publishes immutable position and velocity hints keyed by chunk map; worker threads never touch live ships or players. Hints expire after one second without publication and clear on server stop. A source within three chunks of a piloted ship uses the nearest ship's heading; other sources keep C2ME's spiral order. Cached orders are bounded to 32 radius/heading combinations, and view radii above 64 chunks use the original iterator. It can shorten the time until useful chunks arrive; it does not raise total generation throughput, and because C2ME retains a square footprint, reordering cannot release chunks behind the ship.

## Sable

Sable NeoForge **2.0.5** for Minecraft 1.21.1 (Modrinth version `U678xqle`) is a required dependency; Gradle compiles against it and `dev.ps1 Deploy` installs the hash-checked release JAR beside Civilization. It bundles Sable Companion 1.6.0, Veil 4.3.2 and the Sable Rapier native module 2.0.5. Create and Aeronautics are not used. Windows x64 is exercised; Linux native loading is unverified. Without Create, Sable logs an absent `create:flywheel` physics-property reference at startup; it is harmless.

Integration rules for `BoatSystem` and the airship drive:

- Vessel blocks live in plot-local coordinates; transform with the ship's `logicalPose().transformPosition(...)` for world-space reach, claims and boarding. The two spaces are never interchangeable.
- Apply forces in `ForgeSablePrePhysicsTickEvent` using its timestep. `RigidBodyHandle.applyLinearImpulse` and `applyAngularImpulse` take local-frame impulses: convert world corrections and multiply force and torque by the timestep. Never apply a full per-tick impulse once per substep.
- Store vessel metadata with `getUserDataTag`/`setUserDataTag`; Sable persists the plot and its block entities. Saves are graceful, not crash-atomic.
- Ignore Sable objects with absent or foreign metadata, such as untagged fragments; never assign them engines or ownership.
- Sable owns mass and inertia; Civilization reads them ([physics](physics.md#inspector)).
