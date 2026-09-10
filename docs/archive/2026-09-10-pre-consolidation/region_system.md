# Shared region system — proposed specification

Status: a shared region/biome foundation, global bone-meal disable, slower tree growth outside woodland and no additional region gate for fed livestock are agreed. The architecture and remaining assignments below are proposals, not implemented features.

Related: [regional_geography.md](regional_geography.md), [everyday_play.md](everyday_play.md), [systems_blueprint.md](systems_blueprint.md).

## Purpose

Use one geographic foundation to answer where productive activities work. Farming, forestry, wildlife and resource generation should not each invent a different meaning of river valley, forest or mineral country. Terrain should communicate those opportunities to players.

Separate three concepts:

1. **Terrain and visual biomes:** the shape, vegetation and materials that make a place recognizable.
2. **Regional properties:** stable geographic classifications used by gameplay, such as floodplain, woodland or mineral upland. Properties may overlap.
3. **Finite deposits:** individual reserves with their own location, size and remaining quantity. A region permits generation of a deposit; it is not itself an unlimited resource source.

Existing biome information may supply some classifications; floodplain extent and vertical reach may need additional generated data. Investigate available world-generation data before selecting an implementation. Neither wholesale custom world generation nor a new biome for every rule is assumed necessary.

## Proposed first consumers

| Consumer | Regional input | Rule to define | Status |
| --- | --- | --- | --- |
| Field crops | Natural floodplain plus vertical envelope | Eligible land, then ordinary irrigation/growth and fertilizer rules | Geographic requirement agreed; dimensions and crop coverage open |
| Trees | Woodland suitability | Substantially faster growth in woodland; slower growth on other suitable ground | Agreed direction; rates and local conditions open |
| Natural wildlife | Habitat properties | Species, spawn conditions and bounded populations | Open; do not create renewable metal drops |
| Fishing and wild foods | Suitable water or habitat | Available foods and effort/yield | Economic role agreed; mechanisms open |
| Mineral deposits | Resource-bearing regional properties | Deposit candidates, rarity and finite reserve generation | Finite metals/fuels agreed; distributions open |
| Future regional instruments | Eligible land plus temporary effect | Bounded enhancement, duration and overlap | Capability intended; effect details open |

Spawn rules here refer to natural spawning. Breeding, planted saplings and transported animals need explicit rules of their own; do not infer that moving an animal into another region despawns it or prevents breeding.

## Proposed simple rule model

### First region/activity matrix

This matrix is the first concrete planning draft. Existing commitments are identified in the final column; new restrictions and advantages are recommendations. It is not an implementation or approval of every row.

| Activity | Regional rule | Additional local requirement | Result and feedback | Decision status |
| --- | --- | --- | --- | --- |
| Field crops | Natural floodplain, within its vertical envelope | Suitable planting surface; irrigation if retained | Staple crops; explain unsuitable farmland at planting | River eligibility agreed; crop list and measurements open |
| Timber production | Trees can grow on suitable ordinary ground, but grow substantially faster in woodland regions | Species-appropriate planting and clearance | Logs; show ordinary versus favorable forestry conditions | Agreed; rates and detailed local requirements open |
| Livestock production | No additional region gate for kept livestock | Farm-produced feed at breeding/production interactions | Meat and animal products; explain missing feed | Feed relationship and location independence agreed; yields open |
| Fishing | Proposed: designated natural river/lake/coastal water habitats | Suitable water at the fishing location | Local food; explain unsuitable fishing water | Food role agreed; natural-water restriction and yields proposed |
| Wild gathering and natural wildlife | Proposed: habitat-specific foods and animals | Appropriate local spawn/gather conditions | Subsistence and variety, with bounded replenishment | Economic role agreed; habitat assignments and mechanics open |
| Common quarrying | Common geological materials; uplands offer convenient access | Exposed material and appropriate tool | Physical building materials | Abundant extracted building material agreed; no uplands-only stone rule |
| Coal and ordinary metals | Mineral-bearing terrain improves prospecting opportunities | An actual finite ore occurrence and appropriate tool | Finite Tier 1 resources | Finite supply agreed; exact terrain distribution open |
| Oil and uranium | Rare suitable geological regions | An actual finite deposit and required extraction infrastructure | Higher-tier feedstocks | Rarity/infrastructure agreed; terrain cues and geological properties deferred |
| Workshops and storage | Proposed: no regional production gate | Valid site, structure, rights and operating inputs | Useful production where supplies and routes justify it | Route-driven towns agreed; universal site eligibility proposed |

Use **floodplain**, **woodland suitability** and **mineral-bearing terrain** as the first land properties. Water habitat can reuse the world's river/lake/coast classification where reliable; it is not another land rank. Ordinary terrain needs no special label. A town or crossing is a player opportunity, not a productivity tag.

Regions should indicate potential without revealing every reserve. Visible mineral country tells a player where prospecting may pay off; it does not guarantee oil beneath every hill. The scarcity of a deposit is separate from the size of the landscape containing it.

Natural-water eligibility is a proposal, not a fish-population simulation. Fishing, breeding and natural spawning must not depend on repeatedly scanning a connected river or counting animals across unloaded regions. Detailed mechanics remain to be chosen.

### A sample overlap

Consider one river passing woodland and the foot of rocky hills. Near the wooded bank, both crop and forestry conditions may apply; the player decides which use is worth more. Higher ground beyond the floodplain supports forestry or extraction without allowing field crops. A livestock yard near the crossing town buys feed from the valley rather than requiring a fourth special region. The town hosts storage and workshops because supplies converge there.

A sky platform directly above that valley does not inherit agricultural eligibility beyond the defined vertical envelope. A bucket-created pond does not become a river or, under the proposed fishing rule, a productive natural fishing habitat. Clearing a forest would preserve its forestry potential under the proposed stable-geography model.

This creates land-use competition within overlapping regions as well as trade between specialized ones. No regional monopoly, universal fallback supply or exact travel time is guaranteed by the example.

### Decisions to carry forward

Forestry outside woodland is now agreed to be slower rather than prohibited, and kept livestock has no additional regional gate beyond its feed and ordinary local requirements. Fishing and wildlife remain provisional. Next specify common location-inspection behavior; investigate terrain generation later before implementation. Exact rates need not block the broader systems plan.

### Agreed location feedback

Terrain supplies the primary visual clues. Use a shared, brief land-inspection interaction rather than a permanently visible regional dashboard. It reports current crop eligibility and forestry suitability in plain language, for example “Field crops: suitable” and “Tree growth: favorable.” Failed planting explains the specific problem. Control binding and presentation remain open. Local obstructions and missing irrigation are separate from regional suitability; show the actual reason an attempted action fails. These feedback rules are agreed but unimplemented.

Keep detailed land suitability distinct from prospecting: inspecting a surface does not reveal every buried deposit or its remaining reserves. Broad visible mineral terrain guides exploration; finding buried deposits requires separate prospecting. The interaction, tools and information revealed by prospecting remain unspecified.

A location exposes a small set of regional properties. A rule uses these properties plus ordinary local conditions to allow an action or choose a growth/spawn rate. Keep the first vocabulary small: floodplain, woodland and mineral upland are sufficient candidates to investigate. Do not fill a matrix with invented biomes before identifying useful activities.

Overlap allows a wooded valley to support multiple activities. Each consumer has one explicit eligibility rule and deterministic handling of overlaps; modifiers must not multiply repeatedly just because several labels match. Boundaries should be understandable, and failed planting should explain the requirement.

Recommend stable underlying geography: cutting trees does not erase forestry suitability, and placing water does not create a floodplain. Visible vegetation can change while land retains its productive character. This treatment of forestry and terrain modification is proposed, not confirmed. Distinguish visible forest cover from forestry potential in the eventual player guidance.

Artificial irrigation, terraforming and changes to regional suitability are later questions. Do not silently let a weather bonus, bucket or biome-changing mechanic create new eligibility.

## Representation and lifecycle

Prefer data-driven activity rules over per-biome code branches. Choose the minimal spatial data needed after inspecting world generation: categorical properties, river/floodplain boundaries and a vertical envelope where required. Avoid simulating climate, soil chemistry or hydrology to enforce simple location rules.

Generation and gameplay must use the same regional definition. A region lookup should not scan nearby terrain or force neighboring chunks to load every crop tick. Cache or store derived data at a suitable spatial resolution; define border consistency and validate costs before committing to server scale.

Finite deposit generation occurs once for a site. Remaining reserves persist independently of growth/spawn rules and must not reset when chunks reload or region definitions change. Rule/schema versions need an explicit migration policy; unexplored and already explored terrain must not silently develop contradictory geography.

Player inspection should give a concise statement such as suitable for field crops, rather than exposing internal tags or demanding a map overlay for every action. Ordinary scenery remains the main cue, with inspection for ambiguous edges.

## Next design work

1. Define the first regional property vocabulary and draw one sample region with overlapping areas.
2. Review the first activity matrix above: forestry and livestock boundaries are agreed; fishing and wildlife details remain open.
3. Specify concise land-inspection feedback and distinguish it from prospecting.
4. Specify natural wildlife, fishing and deposits at the role level before adding species or ores.
5. Investigate which required properties existing terrain generation can reliably expose; choose derived regional data versus custom generation based on evidence.

Before coding, test the design against a boundary-crossing farm, a cleared/replanted forest, an artificial pond, a sky platform above a river, moved livestock and an exhausted deposit after restart. No specific world generator or new technical dependency is selected here.
