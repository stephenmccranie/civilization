---
name: civilization-art
description: Develop Civilization artwork, from generated concepts and texture masters to native Blockbench models, through a compact specification, actual asset refinement and verified native publication.
---

Make Civilization art in three stages: **Define → Build and refine → Verify and publish**. The visual rules are in [design](../../../design_direction.md#setting-and-material-style); setup, commands, the textured-model route and modelling craft are in [tools/modeling/README.md](../../../tools/modeling/README.md). Read the relevant system doc and look at neighbouring artwork in the game before designing. Any checkout works: asset folders, references and Codex live in the main checkout's local `art/` and `.tools/`, and `art.py` finds them there.

## Define

- Write the brief: purpose, recognisable target and functional constraints. Choose one authoritative source — a Blockbench project or a Python recipe — and give each reference a role (shape, material or style).
- Keep proposed dimensions and capacities separate from agreed gameplay. Decide routine choices yourself; ask the owner only about decisions that change gameplay or scope.
- Images come from ChatGPT image generation through Codex: `art.py record` draws a concept and `art.py texture` a texture master, each kept with its exact prompt and references; `--image` records one the owner supplies. Each generation spends the owner's ChatGPT plan usage. Established directions and routine repairs proceed from existing references. Competing concepts are opt-in. A concept is a visual target, not a dimensioned blueprint, and a good-looking concept proves nothing about the model.

## Build and refine

- Block out the whole object first, then finish one representative section — main material, a joint and a signature feature — and judge it on the actual model before extending it. For sprite items, judge the native-size export; for model items, the GUI icon at inventory size as well as the native views. For creatures, follow the creature route in the modelling README.
- Paint a model through the README's route: concept → texture master → atlas → recipe. Plan the faces at four texels per unit before writing the layout, describe each master cell's material, seams and wear in the texture prompt, and inspect `review/texture.png` after every reduction. Fix a weak panel with a new master or layout, never by painting the atlas.
- Paint discrete pieces such as stones, bricks and planks whole, as a catalogue of object panels, and lay the model from it on a solid bed ([assembled surfaces](../../../tools/modeling/README.md#assembled-surfaces)); masonry must read completely solid.
- Build the detail: real depth for frames, doors, hinges, latches and supports; fitted face islands with seams, fastener seats and purposeful wear; quiet areas between details. Reused material tiles are a base layer, not a finished surface. No generic noise, decorative machinery or overlapping surfaces.
- Keep one current comparison sheet of target and actual asset at matching angle and scale. Fix the visible cause — shape, missing functional detail, UV placement or material — and ask what became flatter, smoother or disappeared.
- Preserve manual edits and unrelated editor tabs.
- Keep the project you are working on open in Blockbench for the whole task and reuse it between steps rather than opening and closing it each time, saving work to the asset's source as you go. The `tools/modeling` scripts close the tabs they create. Once the task is done, save or close every project you opened directly through MCP, check `ModelProject.all` for leftovers, and quit an editor you started.

## Verify and publish

- Review character, materials, readability and function with specific defects and limits, and keep iterating until no blocking defect remains. Never accept a flaw because attempts plateaued; correct or simplify within scope. Technical validation, cube counts and favourable review text are not evidence — inspected images are.
- Prepare and preview, then run the relevant hidden Photon scene. Check neighbouring family fit, inventory and held views, working states, motion clearances, construction and collision, and performance for costly geometry; check Sable only for assets meant to work aboard vessels.
- For a model item, set its display transforms in Blockbench's display mode first, then check every slot the README's item section lists in the scene: icon, both first-person hands, third-person front and back, dropped and framed and, when worn, the contact face from behind, the side, over armour and as other players see it. Add the scene views a new item needs.
- Publish after current native and in-game reviews, update the affected docs, and commit only completed runtime resources and code. Masters, editable sources and review history stay in the local `art/` folder.

There are no routine owner approval gates. If asked only for concepts or planning, stop at that stage and say so. A requested restart starts a fresh direction; earlier iterations remain local provenance. Report real limitations in proportion to the asset's complexity.
