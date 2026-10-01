# Vanilla Overworld climate audit — Minecraft 1.21.1

Measured from the registered vanilla biomes in the 0.33.72-dev server GameTest. Values are clear-weather background air temperatures in °F at Y=64, before local heat sources. Mountain terrain is colder at its actual higher elevation. Cave profiles have no day/night or direct rain response; room materials and neighboring air still exchange heat.

| Biome | Vanilla temperature index | Noon | Midnight |
| --- | ---: | ---: | ---: |
| badlands | 2.0 | 102.2°F | 69.8°F |
| bamboo jungle | 0.9 | 81.0°F | 70.2°F |
| beach | 0.8 | 71.2°F | 60.4°F |
| birch forest | 0.6 | 71.5°F | 53.5°F |
| cherry grove | 0.5 | 69.8°F | 51.8°F |
| cold ocean | 0.5 | 51.8°F | 41.0°F |
| dark forest | 0.7 | 73.2°F | 55.2°F |
| deep cold ocean | 0.5 | 51.8°F | 41.0°F |
| deep dark | 0.8 | 50.0°F | 50.0°F |
| deep frozen ocean | 0.5 | 23.0°F | 12.2°F |
| deep lukewarm ocean | 0.5 | 78.8°F | 68.0°F |
| deep ocean | 0.5 | 66.2°F | 55.4°F |
| desert | 2.0 | 102.2°F | 69.8°F |
| dripstone caves | 0.8 | 57.2°F | 57.2°F |
| eroded badlands | 2.0 | 102.2°F | 69.8°F |
| flower forest | 0.7 | 73.2°F | 55.2°F |
| forest | 0.7 | 73.2°F | 55.2°F |
| frozen ocean | 0.0 | 23.0°F | 12.2°F |
| frozen peaks | -0.7 | 7.9°F | -10.1°F |
| frozen river | 0.0 | 23.0°F | 5.0°F |
| grove | -0.2 | 18.7°F | 0.7°F |
| ice spikes | 0.0 | 23.0°F | 5.0°F |
| jagged peaks | -0.7 | 7.9°F | -10.1°F |
| jungle | 0.9 | 81.0°F | 70.2°F |
| lukewarm ocean | 0.5 | 78.8°F | 68.0°F |
| lush caves | 0.5 | 60.8°F | 60.8°F |
| mangrove swamp | 0.8 | 78.4°F | 67.6°F |
| meadow | 0.5 | 69.8°F | 51.8°F |
| mushroom fields | 0.9 | 72.9°F | 62.1°F |
| ocean | 0.5 | 66.2°F | 55.4°F |
| old growth birch forest | 0.6 | 71.5°F | 53.5°F |
| old growth pine taiga | 0.3 | 62.6°F | 44.6°F |
| old growth spruce taiga | 0.3 | 62.6°F | 44.6°F |
| plains | 0.8 | 74.8°F | 56.8°F |
| river | 0.5 | 69.8°F | 51.8°F |
| savanna | 2.0 | 89.6°F | 64.4°F |
| savanna plateau | 2.0 | 89.6°F | 64.4°F |
| snowy beach | 0.1 | 24.1°F | 13.3°F |
| snowy plains | 0.0 | 23.0°F | 5.0°F |
| snowy slopes | -0.3 | 16.5°F | -1.5°F |
| snowy taiga | -0.5 | 12.2°F | -5.8°F |
| sparse jungle | 0.9 | 81.0°F | 70.2°F |
| stony peaks | 1.0 | 65.6°F | 47.6°F |
| stony shore | 0.2 | 38.1°F | 27.3°F |
| sunflower plains | 0.8 | 74.8°F | 56.8°F |
| swamp | 0.8 | 78.4°F | 67.6°F |
| taiga | 0.3 | 62.6°F | 44.6°F |
| the void | 0.5 | 69.8°F | 51.8°F |
| warm ocean | 0.5 | 86.0°F | 75.2°F |
| windswept forest | 0.2 | 41.7°F | 23.7°F |
| windswept gravelly hills | 0.2 | 41.7°F | 23.7°F |
| windswept hills | 0.2 | 41.7°F | 23.7°F |
| windswept savanna | 2.0 | 87.8°F | 62.6°F |
| wooded badlands | 2.0 | 102.2°F | 69.8°F |

The audit excludes Nether and End biomes because the thermal field currently supports only the Overworld. Custom biomes use the default index-based temperate profile unless they reuse a named vanilla key.

