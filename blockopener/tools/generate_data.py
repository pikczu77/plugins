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

# Ores hide structure chests.
group("coal_ore", table(pool(table_ref("minecraft:chests/simple_dungeon"))))
group("copper_ore", table(
    pool(table_ref("minecraft:chests/village/village_fisher")),
    pool(table_ref("minecraft:chests/village/village_plains_house")),
))
group("iron_ore", table(pool(table_ref("minecraft:chests/village/village_weaponsmith"))))
group("gold_ore", table(pool(table_ref("minecraft:chests/trial_chambers/reward"))))
group("redstone_ore", table(
    pool(table_ref("minecraft:chests/stronghold_corridor")),
    pool(item("minecraft:ender_pearl", count=(1, 2), chance=0.6)),
))
group("diamond_ore", table(pool(
    table_ref("minecraft:chests/ruined_portal", 55),
    table_ref("minecraft:chests/bastion_treasure", 25),
    table_ref("minecraft:chests/bastion_other", 20),
)))
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
for block in ["chest", "trapped_chest", "barrel"]:
    opening(block, table(pool(table_ref("minecraft:chests/village/village_plains_house"))))

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

HANDHELD = ["block_opener", "bee_drill", "dripstone_sword", "piston_launcher", "copper_magnet"]
GENERATED = ["pumpkin_boots", "anvil_chestplate", "diamond_leggings", "bedrock_bucket", "sculk_helmet", "mossphere"]
for name in HANDHELD + GENERATED:
    parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
    write(f"assets/{NS}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{name}"}})
    write(f"assets/{NS}/items/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:item/{name}"}})

for name, layer in [("pumpkin_boots", "humanoid"), ("anvil_chestplate", "humanoid"),
                    ("diamond_leggings", "humanoid_leggings"), ("sculk_helmet", "humanoid")]:
    write(f"assets/{NS}/equipment/{name}.json", {"layers": {layer: [{"texture": f"{NS}:{name}"}]}})

write(f"assets/{NS}/blockstates/void_water.json", {"variants": {"": {"model": f"{NS}:block/void_water"}}})
write(f"assets/{NS}/models/block/void_water.json", {"textures": {"particle": f"{NS}:block/void_water_still"}})

print("data generated")
