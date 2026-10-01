#!/usr/bin/env python3
"""Generates the JSON resources of the Block Opener mod (loot tables, models, tags...).

Run from the project root:  python3 tools/generate_data.py
Everything it writes is committed, so this only needs to run again after editing it.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "blockopener"


def write(path, data):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ---------------------------------------------------------------- loot helpers

def item(name, weight=1, count=None, chance=None):
    entry = {"type": "minecraft:item", "name": name}
    if weight != 1:
        entry["weight"] = weight
    functions = []
    if count is not None:
        if isinstance(count, tuple):
            functions.append({"function": "minecraft:set_count",
                              "count": {"type": "minecraft:uniform", "min": count[0], "max": count[1]}})
        else:
            functions.append({"function": "minecraft:set_count", "count": count})
    if functions:
        entry["functions"] = functions
    if chance is not None:
        entry["conditions"] = [{"condition": "minecraft:random_chance", "chance": chance}]
    return entry


def table_ref(name, weight=1):
    entry = {"type": "minecraft:loot_table", "value": name}
    if weight != 1:
        entry["weight"] = weight
    return entry


def pool(*entries, rolls=1):
    return {"rolls": rolls, "entries": list(entries)}


def table(*pools):
    return {"type": "minecraft:chest", "pools": list(pools)}


def opening(block, data):
    namespace, path = block.split(":") if ":" in block else ("minecraft", block)
    write(f"data/{NS}/loot_table/opening/{namespace}/{path}.json", data)


def opening_extended(block, data):
    namespace, path = block.split(":") if ":" in block else ("minecraft", block)
    write(f"data/{NS}/loot_table/opening_extended/{namespace}/{path}.json", data)


def enchanted_book():
    return {"type": "minecraft:item", "name": "minecraft:book",
            "functions": [{"function": "minecraft:enchant_randomly"}]}


def group(name, data):
    write(f"data/{NS}/loot_table/opening/group/{name}.json", data)


def use_group(blocks, name):
    for block in blocks:
        opening(block, table(pool(table_ref(f"{NS}:opening/group/{name}"))))


# ---------------------------------------------------------------- opening loot

# Most blocks: "nothing more than a piece of iron", sometimes a little something else.
write(f"data/{NS}/loot_table/opening/default.json", table(pool(
    item("minecraft:iron_ingot", 50),
    item("minecraft:coal", 12, (1, 2)),
    item("minecraft:gold_nugget", 10, (1, 3)),
    item("minecraft:string", 8, (1, 2)),
    item("minecraft:torch", 8, (1, 3)),
    item("minecraft:gold_ingot", 6),
    item("minecraft:arrow", 4, (2, 4)),
    item("minecraft:bread", 2),
)))

# The 10 secret blocks.
SECRETS = {
    "pumpkin_boots": ["pumpkin", "carved_pumpkin", "jack_o_lantern"],
    "bee_drill": ["beehive", "bee_nest"],
    "anvil_chestplate": ["anvil", "chipped_anvil", "damaged_anvil"],
    "diamond_leggings": ["diamond_block"],
    "dripstone_sword": ["dripstone_block"],
    "piston_launcher": ["piston", "sticky_piston"],
    "copper_magnet": ["copper_block", "exposed_copper", "weathered_copper", "oxidized_copper",
                      "waxed_copper_block", "waxed_exposed_copper", "waxed_weathered_copper", "waxed_oxidized_copper"],
    "bedrock_bucket": ["bedrock"],
    "sculk_helmet": ["sculk_shrieker", "sculk_catalyst"],
    "mossphere": ["moss_block"],
}
for secret, blocks in SECRETS.items():
    count = (3, 6) if secret == "mossphere" else None
    for block in blocks:
        opening(block, table(pool(item(f"{NS}:{secret}", count=count))))

# Extended mode (/bo settings extendedItems true): 12 more secret items. These tables only apply
# in extended mode and win over the normal opening/ tables of the same block.
COPPER_STATES = ["", "exposed_", "weathered_", "oxidized_"]
CANDLES = ["candle_cake"] + [f"{color}_candle_cake" for color in [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]]
EXTENDED = {
    "size_mushroom": ["red_mushroom_block", "brown_mushroom_block", "mushroom_stem"],
    "weeping_totem": ["crying_obsidian"],
    "mob_cage": ["spawner", "trial_spawner"],
    "storm_hammer": [f"{wax}{state}lightning_rod" for wax in ["", "waxed_"] for state in COPPER_STATES],
    "glass_spyglass": ["glass", "tinted_glass"],
    "obsidian_shield": ["obsidian"],
    "ender_gloves": ["end_stone", "end_stone_bricks"],
    "slime_gloves": ["slime_block"],
    "ice_wand": ["blue_ice", "packed_ice"],
    "magma_fist": ["magma_block"],
    "cake_of_life": ["cake"] + CANDLES,
    "honey_blaster": ["honey_block"],
}
for secret, blocks in EXTENDED.items():
    for block in blocks:
        opening_extended(block, table(pool(item(f"{NS}:{secret}"))))

# Ores hide structure chests.
# "raining loot and so much wood as well... a golden apple"
group("coal_ore", table(
    pool(table_ref("minecraft:chests/village/village_taiga_house")),
    pool(item("minecraft:golden_apple", chance=0.2)),
))
group("copper_ore", table(
    pool(table_ref("minecraft:chests/village/village_fisher")),
    pool(table_ref("minecraft:chests/village/village_plains_house")),
))
group("iron_ore", table(pool(table_ref("minecraft:chests/village/village_weaponsmith"))))
group("gold_ore", table(pool(table_ref("minecraft:chests/trial_chambers/reward"))))
# "Ender pearls... Obsidian, I'll take. Don't need emeralds. Sharpness four."
group("redstone_ore", table(
    pool(table_ref("minecraft:chests/stronghold_corridor")),
    pool(item("minecraft:ender_pearl", count=(1, 2), chance=0.6)),
    pool(item("minecraft:obsidian", count=(1, 4), chance=0.4)),
    pool(item("minecraft:emerald", count=(1, 3), chance=0.4)),
))
# ruined portal loot, "netherite upgrade... gold blocks" (bastion) and "diamonds can drop ender pearls as well"
group("diamond_ore", table(
    pool(
        table_ref("minecraft:chests/ruined_portal", 55),
        table_ref("minecraft:chests/bastion_treasure", 25),
        table_ref("minecraft:chests/bastion_other", 20),
    ),
    pool(item("minecraft:ender_pearl", count=(1, 2), chance=0.25)),
))
group("lapis_ore", table(pool(
    table_ref("minecraft:chests/shipwreck_treasure", 60),
    table_ref(f"{NS}:opening/group/diamond_ore", 40),
)))
group("emerald_ore", table(pool(table_ref("minecraft:chests/desert_pyramid"))))

use_group(["coal_ore", "deepslate_coal_ore"], "coal_ore")
use_group(["copper_ore", "deepslate_copper_ore"], "copper_ore")
use_group(["iron_ore", "deepslate_iron_ore"], "iron_ore")
use_group(["gold_ore", "deepslate_gold_ore"], "gold_ore")
use_group(["redstone_ore", "deepslate_redstone_ore"], "redstone_ore")
use_group(["diamond_ore", "deepslate_diamond_ore"], "diamond_ore")
use_group(["lapis_ore", "deepslate_lapis_ore"], "lapis_ore")
use_group(["emerald_ore", "deepslate_emerald_ore"], "emerald_ore")

opening("nether_gold_ore", table(pool(table_ref("minecraft:chests/bastion_other"))))
opening("nether_quartz_ore", table(pool(table_ref("minecraft:chests/nether_bridge"))))
opening("ancient_debris", table(pool(table_ref("minecraft:chests/bastion_treasure"))))
opening("reinforced_deepslate", table(pool(table_ref("minecraft:chests/ancient_city"))))
opening("end_stone", table(pool(
    item("minecraft:iron_ingot", 70),
    table_ref("minecraft:chests/end_city_treasure", 30),
)))
for block in ["obsidian", "crying_obsidian"]:
    opening(block, table(pool(table_ref("minecraft:chests/ruined_portal"))))
opening("spawner", table(pool(table_ref("minecraft:chests/simple_dungeon"))))
for block in ["chest", "trapped_chest"]:
    opening(block, table(pool(table_ref("minecraft:chests/village/village_plains_house"))))

# Village workstations hide the chest of the house they belong to.
WORKSTATIONS = {
    "blast_furnace": "village/village_armorer",
    "grindstone": "village/village_weaponsmith",
    "smithing_table": "village/village_toolsmith",
    "fletching_table": "village/village_fletcher",
    "cartography_table": "village/village_cartographer",
    "smoker": "village/village_butcher",
    "loom": "village/village_shepherd",
    "cauldron": "village/village_tannery",
    "water_cauldron": "village/village_tannery",
    "lava_cauldron": "village/village_tannery",
    "powder_snow_cauldron": "village/village_tannery",
    "brewing_stand": "village/village_temple",
    "composter": "village/village_plains_house",
    "barrel": "village/village_fisher",
    "lectern": "stronghold_library",
}
for block, chest in WORKSTATIONS.items():
    opening(block, table(pool(table_ref(f"minecraft:chests/{chest}"))))

# A few more surprises.
opening("emerald_block", table(
    pool(item("minecraft:emerald", count=(2, 5))),
    pool(item("minecraft:villager_spawn_egg", chance=0.3)),
))
opening("enchanting_table", table(
    pool(enchanted_book()),
    pool(item("minecraft:lapis_lazuli", count=(3, 8))),
))
LEAVES = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "azalea", "flowering_azalea"]
for leaves in LEAVES:
    opening(f"{leaves}_leaves", table(
        pool(table_ref(f"{NS}:opening/default")),
        pool(item("minecraft:apple", chance=0.25)),
    ))

# The jokes from the video.
opening("iron_block", table(pool(item("minecraft:iron_ingot"))))
opening("gold_block", table(pool(item("minecraft:coal"))))
opening("melon", table(pool(item("minecraft:coal", count=(1, 2)))))
opening("hay_block", table(pool(item("minecraft:torch", count=(1, 3)))))
opening("bell", table(pool(item("minecraft:string", 6), item("minecraft:gold_ingot", 4))))
opening("cobweb", table(pool(item("minecraft:string", count=(3, 5)))))
opening("stonecutter", table(pool(item("minecraft:gold_nugget", 7, (1, 4)), item("minecraft:diamond", 3))))

# ---------------------------------------------------------------- tags & damage types

write(f"data/{NS}/tags/block/unopenable.json", {"values": [
    "minecraft:barrier", "minecraft:light", "minecraft:structure_void", "minecraft:structure_block",
    "minecraft:jigsaw", "minecraft:command_block", "minecraft:chain_command_block",
    "minecraft:repeating_command_block", "minecraft:nether_portal", "minecraft:end_portal",
    "minecraft:end_gateway", "minecraft:end_portal_frame", "minecraft:moving_piston",
    "minecraft:piston_head", "minecraft:test_block", "minecraft:test_instance_block",
    "#minecraft:fire",
]})

write(f"data/{NS}/damage_type/void_water.json", {
    "message_id": f"{NS}.void_water", "scaling": "never", "exhaustion": 0.0, "effects": "drowning"})
write(f"data/{NS}/damage_type/mossphere.json", {
    "message_id": f"{NS}.mossphere", "scaling": "never", "exhaustion": 0.0})

write("data/minecraft/tags/damage_type/bypasses_armor.json", {"values": [f"{NS}:void_water", f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/bypasses_enchantments.json", {"values": [f"{NS}:void_water", f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/bypasses_effects.json", {"values": [f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/bypasses_resistance.json", {"values": [f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/bypasses_shield.json", {"values": [f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/always_hurts_ender_dragons.json", {"values": [f"{NS}:mossphere"]})
write("data/minecraft/tags/damage_type/no_knockback.json", {"values": [f"{NS}:void_water"]})

# ---------------------------------------------------------------- assets

HANDHELD = ["block_opener", "bee_drill", "dripstone_sword", "piston_launcher", "copper_magnet",
            "storm_hammer", "ice_wand", "magma_fist"]
GENERATED = ["pumpkin_boots", "anvil_chestplate", "diamond_leggings", "bedrock_bucket", "sculk_helmet", "mossphere",
             "size_mushroom", "weeping_totem", "obsidian_shield", "ender_gloves", "slime_gloves", "cake_of_life",
             "honey_blaster"]
for name in HANDHELD + GENERATED:
    parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
    write(f"assets/{NS}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{name}"}})
    write(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

def model_ref(name):
    return {"type": "minecraft:model", "model": f"{NS}:item/{name}"}


# Mob Cage: shows the mob's glowing eyes once something is caught.
for name in ["mob_cage", "mob_cage_full"]:
    write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
write(f"assets/{NS}/items/mob_cage.json", {"model": {
    "type": "minecraft:condition", "property": "minecraft:has_component", "component": "minecraft:custom_data",
    "on_true": model_ref("mob_cage_full"), "on_false": model_ref("mob_cage")}})

# Glass Spyglass: flat icon in menus, the vanilla in-hand spyglass shape (with our texture) everywhere else.
write(f"assets/{NS}/models/item/glass_spyglass.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/glass_spyglass"}})
write(f"assets/{NS}/models/item/glass_spyglass_in_hand.json", {"parent": "minecraft:item/spyglass_in_hand", "textures": {
    "spyglass": f"{NS}:item/glass_spyglass_model", "particle": f"{NS}:item/glass_spyglass"}})
write(f"assets/{NS}/items/glass_spyglass.json", {"model": {
    "type": "minecraft:select", "property": "minecraft:display_context",
    "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": model_ref("glass_spyglass")}],
    "fallback": model_ref("glass_spyglass_in_hand")}})

for name, layer in [("pumpkin_boots", "humanoid"), ("anvil_chestplate", "humanoid"),
                    ("diamond_leggings", "humanoid_leggings"), ("sculk_helmet", "humanoid")]:
    write(f"assets/{NS}/equipment/{name}.json", {"layers": {layer: [{"texture": f"{NS}:{name}"}]}})

write(f"assets/{NS}/blockstates/void_water.json", {"variants": {"": {"model": f"{NS}:block/void_water"}}})
write(f"assets/{NS}/models/block/void_water.json", {"textures": {"particle": f"{NS}:block/void_water_still"}})

print("data generated")
