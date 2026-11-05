"""Static biome tint and texture averages; no chunk generation or asset I/O per pixel."""
import json
from functools import lru_cache
from pathlib import Path

DATA=json.loads(Path(__file__).with_name('colors.json').read_text())
COLOR_VERSION='materials-1'
BIOMES=DATA['biomes']
TEXTURES=DATA['textures']

def multiply(texture,tint):
    return tuple(round(a*b/255) for a,b in zip(texture,tint))

@lru_cache(maxsize=16384)
def color(name,biome):
    name=(name or 'grass_block').split(':')[-1]
    tint=BIOMES.get(biome,BIOMES.get('minecraft:plains',{'grass':[145,189,89],'foliage':[119,171,47],'water':[63,118,228]}))
    if name in ('water','bubble_column','kelp','kelp_plant','seagrass','tall_seagrass'):
        return tuple(round(c*.78) for c in tint['water'])
    if name in ('lava',):return (227,98,20)
    if name in ('grass_block','short_grass','tall_grass','fern','large_fern','potted_fern'):
        return multiply(TEXTURES['grass_block_top'],tint['grass'])
    if name.endswith('_leaves') or name=='vine':
        leaf_tint={'spruce_leaves':(97,153,97),'birch_leaves':(128,167,85)}.get(name,tint['foliage'])
        # These leaf textures are already colored, unlike biome-tinted oak/acacia.
        if name in ('azalea_leaves','flowering_azalea_leaves','cherry_leaves'):
            return tuple(TEXTURES.get(name,(75,120,43)))
        return multiply(TEXTURES.get(name,TEXTURES['oak_leaves']),leaf_tint)
    aliases={'grass_path':'dirt_path_top','dirt_path':'dirt_path_top',
             'mycelium':'mycelium_top','podzol':'podzol_top','farmland':'dirt',
             'snow':'snow','snow_block':'snow','magma_block':'magma',
             'rooted_dirt':'rooted_dirt','smooth_stone':'stone',
             'smooth_sandstone':'sandstone_top','smooth_red_sandstone':'red_sandstone_top'}
    key=aliases.get(name)
    if key is None:
        key=next((k for k in (name+'_top',name,name.removesuffix('_slab'),name.removesuffix('_stairs')) if k in TEXTURES),None)
    if key in TEXTURES:return tuple(TEXTURES[key])
    # Conservative material fallback for unknown modded blocks.
    if 'leaves' in name or 'grass' in name:return multiply(TEXTURES['grass_block_top'],tint['grass'])
    if 'stone' in name or 'rock' in name:return tuple(TEXTURES['stone'])
    if 'dirt' in name or 'mud' in name:return tuple(TEXTURES['dirt'])
    if 'sand' in name:return tuple(TEXTURES['sand'])
    return tuple(TEXTURES['dirt'])

def surface_color(block,biome,height):
    # Elevation belongs to the relief layer, not an artificial material whitening.
    return color(block.get('Name') if isinstance(block,dict) else None,biome)
