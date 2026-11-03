# Civilization — development plan

What is deployed, what is known to be missing, and what gets built next. Agreed rules live in [design_direction.md](design_direction.md); shipped behaviour lives in the system docs indexed by [civilization-mod/README.md](civilization-mod/README.md).

## Now

**Deployed.** The dedicated server and the owner's development instance run the current build (version in `civilization-mod/gradle.properties`). Players install the playtest 0.35.12 revision 1 Prism pack, whose updater follows approved GitHub releases. Older clients back to 0.35.0 still join but keep the old small-placeable inventory restrictions. Server-owned features such as the daily temperature cycle, Tab ping and shared crafting tables need no client update.

**World.** A fresh random-seed Terrain Diffusion world at scale 4, from the owner's fork with its performance and village patches, with climate-based deposits and Survival as the default game mode. Full-chunk and Distant Horizons pregeneration were running at last check; automatic generator startup is disabled.

**Built.** Calories, movement and recovery; the Tier 2 stove and a Creative baking-oven model; regional weather, soil water and crop cycles; finite coal and oil deposits, prospecting and the three-stage refinery; the manual coal prototype with Survival tools; town workshops, shared parts, graded equipment and repairs; half-grid cutting, Builder's Line and controller-fed multiblock guides; claims, whitelists, takeovers, trade counters and the Survey Table; the heat field and comfort; freeform engine boats, Coal Bunker and Cargo Tank; a development-power airship; the physical material foundation with exact stone accounting; wheelbarrows, pallets and offhand carrying; shared crafting tables; the Gladiator Pit; a Creative Colt Paterson; the shared cabinet interface with JEI.

**Known limits.**
- Not yet confirmed by real players: a fresh Prism import and in-launcher pack update, a remote join, the Tab ping view, shared crafting tables and the newer inventory controls in ordinary use, and comfort across the revised daily temperature cycle.
- Ordinary Survival and economy balance, crash recovery, multi-client contention and 200-player load are unverified; automated fixtures do not establish them.
- On the server world, pregeneration completion, elevated-river farming and deposit abundance still need broad-world play.
- Hauling: loaded balance, large pallet groups, remote contention, cross-chunk restarts and broader container adapters are unverified; supply-fed building is not built; ordinary chests are not migrated; vessel cargo has no dynamic mass. Legacy extra blocks are recovered with `/civilization recoverblocks`.
- The Coal Drill still ships alongside the manual coal prototype, whose swing feel and custody cases need review.
- Heat uses an intentionally coarse air-transport approximation; regional sleeping and city-scale load are untested.
- Flight optimisation is deferred: the owner considers about 1,000 blocks/s sufficient for now. Above that, readiness braking and frame stalls persist. Survival nuclear flight is not built.
- The stove, oven and Paterson are Creative-only. The Lacertan is a native model study, not a runtime entity.
- Art masters, editable models and review history live in the local `art/` and `concept_art/` folders, outside git.

## Work queue

Items 1–5 are in priority order; the rest are queued in rough dependency order. Each lists what "done" means.

1. **Hauling and local building.** Review loaded wheelbarrow handling and acquisition on the server world, then build supply-fed placement for ordinary building and Builder's Line, then audit automation and bulk-store adapters before migrating ordinary chests.
   *Done when:* a small home and a modest road or embankment are built with and without the wheelbarrow and compared for time, trips and inventory handling; full targets, competing transfers, lost access, destruction and save/reload each keep exactly one stock balance; nothing scans every frame or forces chunk loading.
2. **Powered and regional loop.** One powered extraction or loading upgrade and a useful loaded river voyage on the scale-4 world: a real ocean → river mouth → inland route, representative reserves by climate, settlement and landing sites, and generation cost.
   *Done when:* handling keeps pace with production on that route, and the evidence settles travel, river elevation and world-lifespan questions in the design.
3. **Manual coal replaces the drill.**
   - Custody: two miners on one face, pile and cart; repeated requests; access revocation; full or blocked targets; lost support; falling stock; tool or cart destruction; death; chunk unload; restart. *Done when* every case keeps exact quantities and presentation can never grant stock.
   - Survival: settle preparation power, hauling and work costs; prove small coal → tools → huge vein → prepared fuel with no Creative supplies; then adapt seam generation and tune yield, labour and route throughput together.
   - Presentation: refine pick, shovel, fracture, piles, cart and preparation machine in first and third person, with Photon and with shaders off; mining and loading audio; a bounded busy-face performance sample. The swing feel needs ordinary-play judgement.
   - Retirement: remove the drill recipe and operation, update JEI, prospecting, tooltips and progression, and apply an explicit legacy-ID and stock recovery policy that preserves saves.
4. **Cooking.** Try the oven in an ordinary kitchen and choose its first baking batch; continue stove play to judge cue timing, carryover, warmth and rewards.
5. **Lacertan in game.** Check Minecraft lighting, readability, full animation clearances and performance before publishing the entity. Encounter rules wait for owner decisions.
6. **Freight docking.** Stationary mooring and engine-driven port-to-port loading on land, then one extraction → loaded voyage → unloading loop. *Done when* disconnection, movement, full targets, lost access, chunk unload and competing transfers conserve stock, fuel cost and capacity are credible, and docking works between real peers. Offshore oil design follows route feasibility and keeps the onshore bootstrap.
7. **Playtest reviews.** Builder's Line length, pace and reach; Paterson cadence, reload and damage, then acquisition; the full first tool → carrier → mining and refinery chain in Survival with peers, including batch, labour and repair economics; Gladiator Pit Survival construction, wager UX, four real clients and crash consistency.
8. **Heat.** Section-local scheduling instead of the whole-field exchange, coherent capacity and energy scales, calibration, dynamic geometry, weather and unload transitions, longer equilibrium. *Done when* conservation, no overshoot, source-off cooling and insulation and ventilation ordering still hold.
9. **Physical foundation.** Prove per-position mass hooks against the pinned Sable, add dynamic cargo mass and vessel thermal state, then explicit fuel → work accounting on one Foundry and one Hot-Bulb Engine. *Done when* input energy equals work plus stored and released heat plus losses within tolerance, with existing throughput preserved.
10. **Airships.** Landing and contact coverage with exact hull, cargo and identity through assembly, disassembly and reload; dynamic mass and finite stabilisation; drag and wind calibration on equal-mass hulls; the Survival nuclear loop with exact finite consumption and visible lift margin; real-peer access, reconnects, walking aboard and controller art.
11. **Food and farming audit.** Non-farmland habitats and the food economy, especially berries and supplied animal food.
12. **Ownership, trade and recovery.** A real multi-client protection audit, owner-only death recovery edge cases, inaccessible claims and vehicle custody; external counter storage, item search and destination pins; competing-client transactions.
13. **Operations before public investment.** Production hosting setup; multi-client verification of state, permissions, transfers and reconnects; consistent backups with a demonstrated restore; compact diagnostics for energy, production, transfer failures and performance; a hosting budget with chunk, entity and machine limits; growing-load tests across exploration, hubs, factories, freight, saves and backups; economy evidence from repeated voluntary farm, forest and mine → carrier → buyer exchanges. Public access is staged by that evidence.
14. **Later.** Pickpocketing and the PvP flag; resource-site sieges and the coal control point; maker's marks; husbandry; broader oil and uranium industry, rail and large construction; sky power, regional towers and the Apogee Pyramid; immersion treatment for farming and boats.
