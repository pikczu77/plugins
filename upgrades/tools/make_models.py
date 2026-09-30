#!/usr/bin/env python3
"""Writes the item models of the body parts (block-model cuboids) and their item definitions.

Run from the upgrades/ folder: python3 tools/make_models.py
"""
import json
import os

ASSETS = "src/client/resources/assets/upgrades"
ALL = ["north", "south", "east", "west", "up", "down"]


def box(frm, to, tex, faces=ALL, light=None, uv=None):
    element = {"from": frm, "to": to, "faces": {}}
    for face in faces:
        entry = {"texture": "#" + tex}
        if uv:
            entry["uv"] = uv
        element["faces"][face] = entry
    if light is not None:
        element["light_emission"] = light
    return element


def write(name, textures, elements):
    model = {"textures": dict(textures, particle=list(textures.values())[0]), "elements": elements}
    os.makedirs(f"{ASSETS}/models/item", exist_ok=True)
    os.makedirs(f"{ASSETS}/items", exist_ok=True)
    with open(f"{ASSETS}/models/item/{name}.json", "w") as f:
        json.dump(model, f, indent="\t")
        f.write("\n")
    with open(f"{ASSETS}/items/{name}.json", "w") as f:
        json.dump({"model": {"type": "minecraft:model", "model": f"upgrades:item/{name}"}}, f, indent="\t")
        f.write("\n")


def tex(name):
    return "upgrades:item/" + name


# Iron golem arm: a long limb with a fist at the bottom (y = 0); the shoulder end is the top.
write("golem_arm", {"arm": tex("golem_arm"), "fist": tex("golem_fist")}, [
    box([5, 3, 5], [11, 16, 11], "arm"),
    box([4, 0, 4], [12, 4, 12], "fist"),
])

# Mini mannequins (the clone levels): a stand wearing iron, diamond or netherite armour.
for material in ("iron", "diamond", "netherite"):
    write(f"mannequin_{material}", {"metal": tex(f"mannequin_{material}"), "wood": tex("stand_wood")}, [
        box([4, 0, 4], [12, 1, 12], "wood"),
        box([7.5, 1, 7.5], [8.5, 12, 8.5], "wood"),
        box([5.5, 1, 6.5], [7.5, 3, 9.5], "metal"),
        box([8.5, 1, 6.5], [10.5, 3, 9.5], "metal"),
        box([5.5, 3, 7], [7.5, 7, 9], "metal"),
        box([8.5, 3, 7], [10.5, 7, 9], "metal"),
        box([5, 7, 6], [11, 12, 10], "metal"),
        box([3.5, 8, 7], [5, 12, 9], "metal"),
        box([11, 8, 7], [12.5, 12, 9], "metal"),
        box([5.5, 12, 5.5], [10.5, 16, 10.5], "metal"),
    ])

# The bed carried on the back: frame against the back (z = 0), mattress, blanket and pillow sticking out.
write("back_bed", {"wood": tex("bed_wood"), "sheet": tex("bed_sheet"), "blanket": tex("bed_blanket")}, [
    box([1, 0, 0], [15, 16, 1.5], "wood"),
    box([1.5, 0.5, 1.5], [14.5, 15.5, 3.5], "sheet"),
    box([1.2, 0.3, 1.4], [14.8, 10.5, 3.9], "blanket"),
    box([3, 12, 3.5], [13, 15, 4.5], "sheet"),
])

# Villager-like nose: hangs down from y = 5, sticks out along +z.
write("villager_nose", {"skin": tex("nose_skin")}, [
    box([6.5, 0, 0], [9.5, 5, 3], "skin"),
])

# Small round shield: the face on both sides, a boss in the middle.
write("mini_shield", {"face": tex("mini_shield"), "rim": tex("bed_wood")}, [
    box([2, 2, 7.5], [14, 14, 8.5], "face", faces=["north", "south"], uv=[0, 0, 16, 16]),
    box([6.5, 6.5, 6.5], [9.5, 9.5, 9.5], "rim", faces=ALL),
])

# Obsidian horn: three shrinking cubes.
write("obsidian_horn", {"obsidian": tex("horn_obsidian")}, [
    box([5, 0, 5], [11, 6, 11], "obsidian"),
    box([6, 6, 6], [10, 11, 10], "obsidian"),
    box([7, 11, 7], [9, 15, 9], "obsidian"),
    box([7.5, 15, 7.5], [8.5, 17, 8.5], "obsidian"),
])

# Pocket nether portal: obsidian frame with a glowing swirl inside.
write("pocket_portal", {"frame": tex("horn_obsidian"), "portal": tex("portal_swirl")}, [
    box([4, 2, 7], [6, 14, 9], "frame"),
    box([10, 2, 7], [12, 14, 9], "frame"),
    box([6, 12, 7], [10, 14, 9], "frame"),
    box([6, 2, 7], [10, 4, 9], "frame"),
    box([6, 4, 7.9], [10, 12, 8.1], "portal", faces=["north", "south"], light=15, uv=[4, 2, 12, 14]),
])

# Dragon wing: a thin membrane (root at x = 0), anchored at its upper root.
write("dragon_wing", {"wing": tex("dragon_wing")}, [
    box([0, 0, 7.75], [16, 16, 8.25], "wing", faces=["north", "south"], uv=[0, 0, 16, 16]),
    box([0, 13, 7.5], [16, 14.5, 8.5], "wing", faces=["up", "down", "east"], uv=[0, 1, 16, 2]),
])

print("models written")
