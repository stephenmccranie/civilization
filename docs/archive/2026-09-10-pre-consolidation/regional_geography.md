# Regional geography — terrain explains productive value

Status: regional specialization, the landscape roles below and the natural floodplain farming model are agreed. Exact dimensions, crop coverage, complementary food mechanics and technical implementation remain open. No world-generation or farming changes are implemented by this document.

Context: [systems_blueprint.md](systems_blueprint.md), [everyday_play.md](everyday_play.md), [design_direction.md](design_direction.md).

## Confirmed organizing principle

Rivers are now the agreed early transport backbone as well as agricultural geography. Later trains serve ground freight at greater expense; airships offer the most speed and bulk at the highest cost. See [transport.md](transport.md). Sample terrain must therefore include practical navigable routes and landings; a river label alone does not establish navigability.

Different places support different useful activities. Players should be able to recognize promising terrain before consulting a menu. Regional advantages create reasons to settle, trade, build routes and value particular land. These are geographic opportunities, not assigned player professions.

The first concrete rule is river-dependent farming. Productive agricultural land occupies a band near a river; planting anywhere with a bucket is not sufficient for this intended model. The radius, meaning of river, vertical reach and relevant food systems need explicit definitions before implementation.

## Agreed first landscape vocabulary

| Landscape | Visible cue | Economic role | Dependency |
| --- | --- | --- | --- |
| River valley | Natural watercourse and recognizable floodplain | Crop production and agricultural settlement | Tools, fertilizer, access to buyers |
| Mineral uplands | Exposed stone, ridges and identifiable mineral terrain | Mining and quarrying | Food and freight capacity |
| Woodland | Forest cover | Timber and biological materials | Tools and access to construction demand |
| Junction or crossing | Meeting of productive regions and practical routes | Workshops, storage and exchange | Supplies and customers from the surrounding regions |

These landscape roles are agreed direction, not an exhaustive map of allowed activities. Towns benefit from routes and neighbors rather than a special biome that permits industry. Higher-tier oil/uranium locations need separate terrain cues later; no exact geology or distribution is chosen here.

## Agreed river-farming model and implementation questions

Agriculture occupies a natural floodplain band around rivers. Player-placed water can irrigate eligible fields but cannot create eligibility elsewhere. Fertilizer improves production within the band. Include a vertical bound so ordinary terraces can work without automatically allowing a sky platform above a river to farm. Width, height allowance and crop coverage remain unspecified.

Use a stable natural-river/floodplain designation derived from world generation, with a bounded horizontal agricultural band and a vertical envelope around the valley. Exact representation is a technical investigation, not an assumed existing Minecraft feature. Evaluate the designation when planting or growing crops; avoid searching for rivers continuously.

Player-placed water provides local irrigation but does not create new eligible land. Proposed implementation: filling or draining blocks does not silently relocate the productive region. Water availability and river-region eligibility are separate checks if both are retained. The stable geographic representation and river-editing behavior still need specification.

Outside eligible land, proposed behavior is to reject planting with a short explanation rather than allow a crop to grow invisibly slowly or fail without feedback. A simple inspection hint should identify suitable farmland and explain failure. Natural terrain should carry the primary cue; overlays supplement it.

A horizontal radius alone would also permit farms far above a river. Decide the height allowance and terrace behavior explicitly so ordinary floodplain terraces work without accidentally making a sky farm eligible. Canals, pumped irrigation and artificial expansion of farmland are deferred candidates, not approved bypasses to regional specialization.

## Connections and unresolved boundaries

- **Subsistence:** accessible uncooked food must still sustain independent play. A household can settle by a river; living elsewhere can involve provisions or trade. Specify distribution and discovery so new arrivals can find viable food without owning advanced infrastructure. Emergency morsels remain recovery, not the normal food economy.
- **Crop scope:** decide whether the river rule covers field crops only or also trees, mushrooms, berries, sugar cane, aquatic plants and other biological production. Animal breeding, fishing and wild-food collection require separate review before claiming that river agriculture supplies a large market.
- **Nutrition:** river eligibility is a place constraint, not an extra biological meter. Do not add a detailed soil or climate simulation merely to justify it.
- **Fertilizer and weather:** proposed relationship is to improve output on eligible farmland, not remove the geographic requirement. Exact bonuses remain open.
- **Property:** valuable river frontage creates differentiated land demand. Claim price, tenure and market allocation are separate unresolved mechanisms.
- **Logistics:** the river visually explains agriculture; whether it is also a navigable freight route depends on terrain generation and the transport rules. Do not assume every watercourse is suitable for boats.
- **World generation:** test a sample region before fixing river spacing, agricultural band width or reserve abundance. Avoid every useful place being so distant that trade becomes mostly waiting.
- **Migration:** existing dev farms may fall outside future eligible terrain. Define development-world migration or replacement explicitly before enforcing the new rule.

## Next specification

**Agreed food roles:** river crops provide the main staple calories. Livestock consumes farm-produced feed to produce meat and other foods, connecting animal producers to agriculture. Fishing and wild gathering support local subsistence and variety, with enough effort that purchasing provisions remains attractive. These are intended economic roles, not yet demonstrated balance.

Use feed costs at breeding or production interactions rather than continuously simulating hunger for every animal. Exact interaction coverage, yields, animal location requirements, wild spawning and aquatic food production remain open. Audit renewable animal products and unfed breeding/growth/harvest paths so they do not bypass the feed relationship. Do not require every fishing trip or wild animal harvest to use purchased feed. These decisions are planned, not implemented.

Next planning task: a sample region connecting a farm valley, livestock producer, mineral uplands, woodland and crossing town. Specify imports, exports and routes before numerical yields or detailed recipes.

The user accepted this sample-region direction: nearby livestock producers buy farm feed; mineral uplands import provisions; woodland supplies building timber; a crossing town concentrates storage, processing and exchange. Opportunities may overlap, and town value comes from convenient routes rather than a special city biome or automatic production bonus. Distances remain open. The agreed one-log/one-plank conversion in [everyday_play.md](everyday_play.md) increases the physical timber volume needed by those settlements.

Draw one river valley with adjacent uplands, forest, a crossing town and candidate routes. Mark where farming is possible, what each settlement imports and exports, and how terrain communicates it. Compare a self-sufficient homestead with a commercial farm and a mine settlement.

Then choose the agricultural band, crop coverage and small set of complementary regional specialties together. Keep prices and travel distances provisional until transport and land allocation are specified.
