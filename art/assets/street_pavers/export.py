"""Export the hand-painted eight-meter road texture at 64 pixels per block."""

from pathlib import Path
import json
import zipfile

from PIL import Image

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
ASSETS = ROOT / "civilization-mod/src/main/resources/assets/civilization"
MINECRAFT = Path.home() / ".gradle/caches/neoformruntime/artifacts/minecraft_1.21.1_client.jar"

PIXELS_PER_BLOCK = 64
ROAD_BLOCKS = 8
ROAD_SIZE = PIXELS_PER_BLOCK * ROAD_BLOCKS
SOURCE = HERE / "generated-road-large.png"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    source = Image.open(SOURCE).convert("RGB")
    if source.width != source.height:
        raise ValueError("The generated road source must be square")
    road = source.resize((ROAD_SIZE, ROAD_SIZE), Image.Resampling.BOX)
    # Items and exposed sides use a matching square from the same painting.
    side = road.crop((192, 192, 256, 256))
    textures = ASSETS / "textures/block"
    textures.mkdir(parents=True, exist_ok=True)
    for name, image in (("street_pavers_mosaic", road), ("street_pavers_0", side)):
        image.save(HERE / f"{name}.png")
        image.save(textures / f"{name}.png")

    write_json(ASSETS / "models/block/street_pavers_0.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "civilization:block/street_pavers_0"},
    })
    write_json(ASSETS / "models/block/street_pavers_mosaic.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"bottom": "civilization:block/street_pavers_0",
                     "top": "civilization:block/street_pavers_mosaic",
                     "side": "civilization:block/street_pavers_0"},
    })
    write_json(ASSETS / "blockstates/street_pavers.json", {
        "variants": {"": {"model": "civilization:block/street_pavers_mosaic"}},
    })
    write_json(ASSETS / "models/item/street_pavers.json", {
        "parent": "civilization:block/street_pavers_0",
    })

    # Native slab/stair item identities keep the existing saw/recombine integration.
    for name, parent in (("street_pavers_slab", "slab"), ("street_pavers_slab_top", "slab_top"),
                         ("street_pavers_stairs", "stairs"), ("street_pavers_stairs_inner", "inner_stairs"),
                         ("street_pavers_stairs_outer", "outer_stairs")):
        write_json(ASSETS / f"models/block/{name}.json", {
            "parent": f"minecraft:block/{parent}",
            "textures": {"bottom": "civilization:block/street_pavers_0",
                         "top": "civilization:block/street_pavers_mosaic",
                         "side": "civilization:block/street_pavers_0"},
        })
    for name, parent in (("street_pavers_slab", "slab"), ("street_pavers_stairs", "stairs")):
        write_json(ASSETS / f"models/item/{name}.json", {
            "parent": f"minecraft:block/{parent}",
            "textures": {key: "civilization:block/street_pavers_0" for key in ("bottom", "top", "side")},
        })
    with zipfile.ZipFile(MINECRAFT) as jar:
        for vanilla, ours in (("brick_slab", "street_pavers_slab"), ("brick_stairs", "street_pavers_stairs")):
            blockstate = jar.read(f"assets/minecraft/blockstates/{vanilla}.json").decode("utf-8")
            blockstate = blockstate.replace("minecraft:block/bricks", "civilization:block/street_pavers_0")
            blockstate = blockstate.replace(f"minecraft:block/{vanilla}", f"civilization:block/{ours}")
            if vanilla == "brick_slab":
                blockstate = blockstate.replace('"model": "civilization:block/street_pavers_0"',
                                                '"model": "civilization:block/street_pavers_mosaic"')
            write_json(ASSETS / f"blockstates/{ours}.json", json.loads(blockstate))

    road.crop((0, 0, PIXELS_PER_BLOCK * 6, PIXELS_PER_BLOCK * 6)).save(HERE / "road-review.png")


if __name__ == "__main__":
    main()
