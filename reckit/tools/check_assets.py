#!/usr/bin/env python3
"""Checks that every prop in the catalog is complete: item definition, models, textures and names.

Run from the reckit/ folder: python3 tools/check_assets.py (also part of ./gradlew check).
Exits with 1 on errors; warnings (unused files, odd texture sizes) are only printed.
"""
import json
import os
import struct
import sys

RES = "src/main/resources"
ASSETS = f"{RES}/assets"
NS = "reckit"
LANGS = ("pl_pl", "en_us")

errors = []
warnings = []
used_models = set()
used_textures = set()


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
        return
    file = f"{ASSETS}/{ns}/textures/{path}.png"
    used_textures.add(os.path.normpath(file))
    if not os.path.isfile(file):
        errors.append(f"{where}: missing texture {ref} ({file})")
        return
    size = png_size(file)
    if size is None:
        errors.append(f"{file}: not a PNG file")
        return
    width, height = size
    if height != width:
        if height % width == 0 and os.path.isfile(file + ".mcmeta"):
            used_textures.add(os.path.normpath(file + ".mcmeta"))
        elif height % width == 0:
            warnings.append(f"{file}: {width}x{height} looks like an animation strip but has no .mcmeta")
        else:
            warnings.append(f"{file}: not square ({width}x{height})")
    elif os.path.isfile(file + ".mcmeta"):
        used_textures.add(os.path.normpath(file + ".mcmeta"))


def check_model(ref, where, seen=()):
    ns, path = split_id(ref)
    if ns == "minecraft" or ref.startswith("builtin/"):
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
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    catalog = load_json(f"{RES}/reckit/catalog.json")
    langs = {lang: load_json(f"{ASSETS}/{NS}/lang/{lang}.json") for lang in LANGS}
    item_ids = set()
    lang_keys = set()

    for pack in catalog["packs"]:
        key = f"itemGroup.{NS}.{pack['id']}"
        lang_keys.add(key)
        for item in pack["items"]:
            item_id = item["id"]
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

    if os.path.isdir(f"{ASSETS}/minecraft"):
        errors.append(f"{ASSETS}/minecraft: props must not change vanilla assets")

    for warning in warnings:
        print("warning:", warning)
    for error in errors:
        print("error:", error)
    print(f"{len(item_ids)} props in {len(catalog['packs'])} packs, {len(errors)} errors, {len(warnings)} warnings")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
