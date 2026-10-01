#!/usr/bin/env python3
"""Checks that every prop in the catalog is complete: item definition, models, textures and names.

Run from the reckit/ folder: python3 tools/check_assets.py (also part of ./gradlew check).
Exits with 1 on errors; warnings (unused files, odd texture sizes) are only printed.
"""
import glob
import json
import os
import struct
import sys
import zipfile

RES = "src/main/resources"
ASSETS = f"{RES}/assets"
NS = "reckit"
LANGS = ("pl_pl", "en_us")
# Vanilla textures outside block/ and item/ must be added to an atlas, otherwise item models cannot use them.
ATLAS = f"{ASSETS}/minecraft/atlases/blocks.json"

errors = []
warnings = []
used_models = set()
used_textures = set()
atlas_extra = set()
vanilla = None  # file names in the client jar, when it is in the Gradle cache


def vanilla_has(path):
    return vanilla is None or f"assets/minecraft/{path}" in vanilla


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def split_id(ref, default_ns="minecraft"):
    ns, _, path = ref.rpartition(":")
    return ns or default_ns, path


def png_size(path):
    with open(path, "rb") as f:
        head = f.read(24)
    if head[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", head[16:24])


def model_refs(node):
    """Yields the model ids used by an item model definition (assets/<ns>/items/*.json)."""
    if isinstance(node, dict):
        kind = node.get("type", "").removeprefix("minecraft:")
        if kind == "model" and isinstance(node.get("model"), str):
            yield node["model"]
        if kind == "special" and isinstance(node.get("base"), str):
            yield node["base"]
        for value in node.values():
            yield from model_refs(value)
    elif isinstance(node, list):
        for value in node:
            yield from model_refs(value)


def check_texture(ref, where):
    ns, path = split_id(ref)
    if ns == "minecraft":
        if not vanilla_has(f"textures/{path}.png"):
            errors.append(f"{where}: vanilla texture {ref} does not exist")
        elif not path.startswith(("block/", "item/")) and f"minecraft:{path}" not in atlas_extra:
            errors.append(f"{where}: vanilla texture {ref} is not on an atlas (add it to {ATLAS})")
        return
    file = f"{ASSETS}/{ns}/textures/{path}.png"
    if os.path.normpath(file) in used_textures:
        return
    used_textures.add(os.path.normpath(file))
    if not os.path.isfile(file):
        errors.append(f"{where}: missing texture {ref} ({file})")
        return
    size = png_size(file)
    if size is None:
        errors.append(f"{file}: not a PNG file")
        return
    width, height = size
    if os.path.isfile(file + ".mcmeta"):
        used_textures.add(os.path.normpath(file + ".mcmeta"))
    elif height > width and height % width == 0 and width <= 32:
        warnings.append(f"{file}: {width}x{height} looks like an animation strip but has no .mcmeta")


def chain_textures(ref):
    """Concrete texture references of a mod model and its mod parents."""
    ns, path = split_id(ref)
    file = f"{ASSETS}/{ns}/models/{path}.json"
    if ns == "minecraft" or not os.path.isfile(file):
        return []
    try:
        model = load_json(file)
    except json.JSONDecodeError:
        return []
    values = [v for v in model.get("textures", {}).values() if isinstance(v, str) and not v.startswith("#")]
    return values + (chain_textures(model["parent"]) if "parent" in model else [])


def check_atlas(ref, where):
    """Item models look textures up in the item atlas first, then the block atlas, and must not use both."""
    atlases = {}
    for texture in chain_textures(ref):
        path = split_id(texture)[1]
        atlases.setdefault("item" if path.startswith("item/") else "block", texture)
    if len(atlases) > 1:
        errors.append(f"{where}: model {ref} mixes the item atlas ({atlases['item']}) and the block atlas ({atlases['block']})")


def check_model(ref, where, seen=()):
    ns, path = split_id(ref)
    if ref.startswith("builtin/"):
        return
    if ns == "minecraft":
        if not vanilla_has(f"models/{path}.json"):
            errors.append(f"{where}: vanilla model {ref} does not exist")
        return
    if ref in seen:
        errors.append(f"{where}: parent loop {' -> '.join(seen + (ref,))}")
        return
    file = f"{ASSETS}/{ns}/models/{path}.json"
    if os.path.normpath(file) in used_models and not seen:
        return
    used_models.add(os.path.normpath(file))
    if not os.path.isfile(file):
        errors.append(f"{where}: missing model {ref} ({file})")
        return
    try:
        model = load_json(file)
    except json.JSONDecodeError as e:
        errors.append(f"{file}: invalid JSON ({e})")
        return
    for key, value in model.get("textures", {}).items():
        if isinstance(value, dict):
            value = value.get("sprite", "")
        if value and not value.startswith("#"):
            check_texture(value, file)
    if "parent" in model:
        check_model(model["parent"], file, seen + (ref,))


def main():
    global vanilla
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    version = next(line.split("=", 1)[1].strip() for line in open("gradle.properties") if line.startswith("minecraft_version="))
    jars = glob.glob(os.path.expanduser(f"~/.gradle/caches/fabric-loom/{version}/minecraft-client.jar"))
    if jars:
        vanilla = set(zipfile.ZipFile(jars[0]).namelist())
    else:
        warnings.append(f"no {version} client jar in the Gradle cache, vanilla models and textures are not checked")
    if os.path.isfile(ATLAS):
        atlas_extra.update(source["resource"] for source in load_json(ATLAS)["sources"] if "resource" in source)
    catalog = load_json(f"{RES}/reckit/catalog.json")
    langs = {lang: load_json(f"{ASSETS}/{NS}/lang/{lang}.json") for lang in LANGS}
    item_ids = set()
    lang_keys = set()

    for pack in catalog["packs"]:
        key = f"itemGroup.{NS}.{pack['id']}"
        lang_keys.add(key)
        for item in pack["items"]:
            item_id = item["id"]
            if item_id in item_ids:
                errors.append(f"catalog: item id {item_id} is used twice")
            item_ids.add(item_id)
            lang_keys.add(f"item.{NS}.{item_id}")
            definition = f"{ASSETS}/{NS}/items/{item_id}.json"
            if not os.path.isfile(definition):
                errors.append(f"{item_id}: missing item definition {definition}")
                continue
            refs = list(model_refs(load_json(definition)))
            if not refs:
                errors.append(f"{definition}: does not use any model")
            for ref in refs:
                check_model(ref, definition)
                check_atlas(ref, definition)

    for lang, entries in langs.items():
        for key in sorted(lang_keys - entries.keys()):
            errors.append(f"lang/{lang}.json: missing name {key}")
        for key in sorted(entries.keys() - lang_keys):
            warnings.append(f"lang/{lang}.json: unused key {key}")

    items_dir = f"{ASSETS}/{NS}/items"
    for name in sorted(os.listdir(items_dir)) if os.path.isdir(items_dir) else []:
        if name.removesuffix(".json") not in item_ids:
            errors.append(f"{items_dir}/{name}: not in the catalog")

    for kind, used in (("models", used_models), ("textures", used_textures)):
        for root, _, files in os.walk(f"{ASSETS}/{NS}/{kind}"):
            for name in files:
                file = os.path.normpath(os.path.join(root, name))
                if file not in used:
                    warnings.append(f"{file}: not used by any prop")

    for root, _, files in os.walk(f"{ASSETS}/minecraft"):
        for name in files:
            file = os.path.join(root, name)
            if file != ATLAS:
                errors.append(f"{file}: props must not change vanilla assets (only {ATLAS} may add textures)")

    for warning in warnings:
        print("warning:", warning)
    for error in errors:
        print("error:", error)
    print(f"{len(item_ids)} props in {len(catalog['packs'])} packs, {len(errors)} errors, {len(warnings)} warnings")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
