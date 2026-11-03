---
name: civilization-art
description: Develop Civilization artwork through a compact specification, actual asset refinement and verified native publication.
---

Make Civilization art in three stages: **Define → Build and refine → Verify and publish**. The visual rules are in [design](../../../design_direction.md#setting-and-material-style); setup, commands and modelling craft are in [tools/modeling/README.md](../../../tools/modeling/README.md). Read the relevant system doc and look at neighbouring artwork in the game before designing.

## Define

- Write the brief: purpose, recognisable target and functional constraints. Choose one authoritative source — a Blockbench project or a Python recipe — and give each reference a role (shape, material or style).
- Keep proposed dimensions and capacities separate from agreed gameplay. Decide routine choices yourself; ask the owner only about decisions that change gameplay or scope.
- When a concept would help, generate it with `art.py record --prompt` (Codex image generation; `--references` passes reference images), or record one the owner supplies with `--image`. Each generation spends the owner's ChatGPT plan usage. Established directions and routine repairs proceed from existing references. Competing concepts are opt-in. A concept is a visual target, not a dimensioned blueprint, and a good-looking concept proves nothing about the model.

## Build and refine

- Block out the whole object first, then finish one representative section — main material, a joint and a signature feature — and judge it on the actual model before extending it. For items, judge the native-size sprite export. For creatures, follow the creature route in the modelling README.
- Build the detail: real depth for frames, doors, hinges, latches and supports; fitted face islands with seams, fastener seats and purposeful wear; quiet areas between details. Reused material tiles are a base layer, not a finished surface. No generic noise, decorative machinery or overlapping surfaces.
- Keep one current comparison sheet of target and actual asset at matching angle and scale. Fix the visible cause — shape, missing functional detail, UV placement or material — and ask what became flatter, smoother or disappeared.
- Preserve manual edits and unrelated editor tabs.

## Verify and publish

- Review character, materials, readability and function with specific defects and limits, and keep iterating until no blocking defect remains. Never accept a flaw because attempts plateaued; correct or simplify within scope. Technical validation, cube counts and favourable review text are not evidence — inspected images are.
- Prepare and preview, then run the relevant hidden Photon scene. Check neighbouring family fit, inventory and held views, working states, motion clearances, construction and collision, and performance for costly geometry; check Sable only for assets meant to work aboard vessels.
- Publish after current native and in-game reviews, update the affected docs, and commit only completed runtime resources and code. Masters, editable sources and review history stay in the local `art/` folder.

There are no routine owner approval gates. If asked only for concepts or planning, stop at that stage and say so. A requested restart starts a fresh direction; earlier iterations remain local provenance. Report real limitations in proportion to the asset's complexity.
