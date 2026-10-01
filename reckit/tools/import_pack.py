#!/usr/bin/env python3
"""Imports the props of one source pack into the mod.

Run from the reckit/ folder:
    python3 tools/import_pack.py sources/<pack>.json <resource pack .zip or folder>

The recipe (sources/<pack>.json) lists the items to take from the pack: their id, names and either the source model
("model") or a flat texture ("texture", made into a handheld model). Models and textures are copied into
assets/reckit/models/item/<pack>/ and assets/reckit/textures/{item,block}/<pack>/ (see Importer) with their references
rewritten; vanilla models and textures stay references to minecraft:. Item overrides (custom_model_data, pulling,
blocking) are dropped: props have one look.
Afterwards the catalog, the lang files and the item list in the README are regenerated from all recipes.
"""
import glob
import io
import json
import os
import shutil
import sys
import tempfile
import zipfile

from PIL import Image

RES = "src/main/resources"
ASSETS = f"{RES}/assets"
NS = "reckit"
LANGS = ("pl_pl", "en_us")
ATLAS = f"{ASSETS}/minecraft/atlases/blocks.json"
README = "README.md"
# Textures bigger than this that are not square or not a multiple of 16 are padded and scaled down, so they do not
# limit the mipmaps of the whole atlas (and flat items do not get thousands of edge faces).
MAX_ODD_TEXTURE = 128


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent="\t")
        f.write("\n")


def split_id(ref):
    ns, _, path = ref.rpartition(":")
    return ns or "minecraft", path


class VanillaAssets:
    """The client jar of the target version, to check that vanilla models and textures still exist."""

    def __init__(self):
        version = "unknown"
        with open("gradle.properties", encoding="utf-8") as f:
            for line in f:
                if line.startswith("minecraft_version="):
                    version = line.split("=", 1)[1].strip()
        jars = glob.glob(os.path.expanduser(f"~/.gradle/caches/fabric-loom/{version}/minecraft-client.jar"))
        self.jar = zipfile.ZipFile(jars[0]) if jars else None
        self.names = set(self.jar.namelist()) if self.jar else None
        if self.jar is None:
            print(f"warning: no {version} client jar in the Gradle cache (run ./gradlew build), vanilla assets are not checked")

    def has(self, path):
        return self.names is None or f"assets/minecraft/{path}" in self.names

    def read(self, path):
        return self.jar.read(f"assets/minecraft/{path}") if self.names and f"assets/minecraft/{path}" in self.names else None


class Importer:
    """Copies models and textures of one pack.

    Item models look their textures up in the item atlas first and in the block atlas otherwise, and one model may only
    use one of them. So a model that uses vanilla block textures (or entity textures added to the block atlas) gets its
    pack textures copied to textures/block/ (block atlas), every other model to textures/item/ (item atlas).
    """

    def __init__(self, recipe, root, vanilla):
        self.pack = recipe["id"]
        self.root = root
        self.vanilla = vanilla
        self.models = {}
        self.textures = {}
        self.used_names = {"models": set(), "item": set(), "block": set()}
        self.atlas = set()
        self.problems = []

    def source(self, kind, ref, ext):
        ns, path = split_id(ref)
        file = os.path.join(self.root, "assets", ns, kind, path + ext)
        return file if os.path.isfile(file) else None

    def unique(self, kind, name):
        candidate, n = name, 2
        while candidate in self.used_names[kind]:
            candidate, n = f"{name}_{n}", n + 1
        self.used_names[kind].add(candidate)
        return candidate

    def chain_textures(self, ref):
        """Concrete texture references of a pack model and its pack parents."""
        file = self.source("models", ref, ".json") if not ref.startswith("builtin/") else None
        if file is None:
            return []
        model = load_json(file)
        values = [v for v in model.get("textures", {}).values() if isinstance(v, str) and not v.startswith("#")]
        return values + (self.chain_textures(model["parent"]) if "parent" in model else [])

    def atlas_of(self, textures):
        for ref in textures:
            ns, path = split_id(ref)
            in_pack = self.source("textures", ref, ".png") is not None
            if not in_pack and ns == "minecraft" and self.vanilla.has(f"textures/{path}.png") and not path.startswith("item/"):
                return "block"
        return "item"

    def texture(self, ref, atlas):
        """Returns the new reference of a texture, or None if it exists neither in the pack nor in vanilla."""
        if (ref, atlas) in self.textures:
            return self.textures[(ref, atlas)]
        ns, path = split_id(ref)
        file = self.source("textures", ref, ".png")
        if file is None:
            if ns != "minecraft" or not self.vanilla.has(f"textures/{path}.png"):
                self.problems.append(f"missing texture {ref}")
                return None
            if atlas == "item" or path.startswith("block/"):
                new = f"minecraft:{path}"
            elif path.startswith("item/"):
                # A vanilla item texture in a block atlas model: copy it next to the block atlas textures.
                image = self.vanilla.read(f"textures/{path}.png")
                if image is None:
                    self.problems.append(f"{ref} has to be copied from the client jar, which is missing")
                    return None
                new = self.copy_texture(ref, atlas, image, self.vanilla.read(f"textures/{path}.png.mcmeta"))
            else:
                self.atlas.add(f"minecraft:{path}")
                new = f"minecraft:{path}"
        else:
            mcmeta = file + ".mcmeta"
            with open(file, "rb") as f:
                image = f.read()
            new = self.copy_texture(ref, atlas, image, open(mcmeta, "rb").read() if os.path.isfile(mcmeta) else None)
        self.textures[(ref, atlas)] = new
        return new

    def copy_texture(self, ref, atlas, image, mcmeta):
        name = self.unique(atlas, os.path.basename(split_id(ref)[1]))
        dest = f"{ASSETS}/{NS}/textures/{atlas}/{self.pack}/{name}.png"
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        picture = Image.open(io.BytesIO(image))
        width, height = picture.size
        odd = width != height or width % 16 != 0
        if mcmeta is None and odd and max(width, height) > MAX_ODD_TEXTURE:
            side = max(width, height)
            square = Image.new("RGBA", (side, side))
            square.paste(picture.convert("RGBA"), ((side - width) // 2, (side - height) // 2))
            square.resize((MAX_ODD_TEXTURE, MAX_ODD_TEXTURE), Image.LANCZOS).save(dest)
            print(f"  {ref}: {width}x{height} padded and scaled to {MAX_ODD_TEXTURE}x{MAX_ODD_TEXTURE}")
        else:
            with open(dest, "wb") as f:
                f.write(image)
        if mcmeta is not None:
            meta = json.loads(mcmeta)
            animation = meta.get("animation", {})
            frame_count = height // animation.get("height", width)
            if isinstance(animation.get("frametime"), float):
                animation["frametime"] = max(1, int(animation["frametime"]))  # 1.21 reads whole ticks, like 1.20 did
            frames = animation.get("frames")
            if frames:
                valid = [f for f in frames if (f["index"] if isinstance(f, dict) else f) < frame_count]
                if len(valid) != len(frames):
                    print(f"  {ref}: dropped animation frames past the {frame_count} in the texture")
                    animation["frames"] = valid
            save_json(dest + ".mcmeta", meta)
        return f"{NS}:{atlas}/{self.pack}/{name}"

    def model(self, ref, atlas=None):
        """Returns the new reference of a model (copied with its parents and textures)."""
        if ref.startswith("builtin/"):
            return ref
        ns, path = split_id(ref)
        file = self.source("models", ref, ".json")
        if file is None:
            if ns != "minecraft" or not self.vanilla.has(f"models/{path}.json"):
                self.problems.append(f"missing model {ref}")
            return f"minecraft:{path}" if ns == "minecraft" else ref
        atlas = atlas or self.atlas_of(self.chain_textures(ref))
        if (ref, atlas) not in self.models:
            new = f"{NS}:item/{self.pack}/{self.unique('models', os.path.basename(path))}"
            self.models[(ref, atlas)] = new
            self.write_model(new, load_json(file), atlas)
        return self.models[(ref, atlas)]

    def write_model(self, ref, model, atlas):
        model.pop("overrides", None)
        if "parent" in model:
            model["parent"] = self.model(model["parent"], atlas)
        textures = model.get("textures", {})
        for key, value in list(textures.items()):
            if isinstance(value, str) and not value.startswith("#"):
                textures[key] = self.texture(value, atlas)
        fallback = next((v for v in textures.values() if v and not v.startswith("#")), None)
        for key, value in list(textures.items()):
            if value is None:
                if fallback is None:
                    del textures[key]
                else:
                    textures[key] = fallback
                    self.problems.append(f"{ref}: texture '{key}' replaced with {fallback}")
        if "elements" in model and fallback and "particle" not in textures:
            textures["particle"] = fallback
        save_json(f"{ASSETS}/{NS}/models/{split_id(ref)[1]}.json", model)

    def item(self, entry):
        if "model" in entry:
            model = self.model(entry["model"])
        else:
            model = f"{NS}:item/{self.pack}/{self.unique('models', entry['id'])}"
            texture = entry["texture"]
            source = {"parent": entry.get("parent", "minecraft:item/handheld"), "textures": {"layer0": texture}}
            if "display" in entry:
                source["display"] = entry["display"]
            self.write_model(model, source, self.atlas_of([texture]))
        save_json(f"{ASSETS}/{NS}/items/{entry['id']}.json", {"model": {"type": "minecraft:model", "model": model}})


def resource_root(path, temp):
    """The folder with assets/ inside a resource pack zip or folder (also when it is nested one level deeper)."""
    if zipfile.is_zipfile(path):
        with zipfile.ZipFile(path) as zf:
            zf.extractall(temp)
        path = temp
    for root, dirs, _ in os.walk(path):
        if "assets" in dirs:
            return root
    sys.exit(f"No assets/ folder in {path}")


def remove_pack(pack, item_ids):
    for folder in ("models/item", "textures/item", "textures/block"):
        shutil.rmtree(f"{ASSETS}/{NS}/{folder}/{pack}", ignore_errors=True)
    for item_id in item_ids:
        path = f"{ASSETS}/{NS}/items/{item_id}.json"
        if os.path.exists(path):
            os.remove(path)


def regenerate():
    """Rebuilds the catalog, the lang files and the README item list from all recipes (keeping the pack order)."""
    recipes = {r["id"]: r for r in (load_json(f) for f in sorted(glob.glob("sources/*.json")))}
    catalog_path = f"{RES}/reckit/catalog.json"
    order = [p["id"] for p in load_json(catalog_path)["packs"] if p["id"] in recipes]
    order += [pack for pack in recipes if pack not in order]

    packs, langs = [], {lang: {} for lang in LANGS}
    readme = []
    for pack in order:
        recipe = recipes[pack]
        items = []
        for lang in LANGS:
            langs[lang][f"itemGroup.{NS}.{pack}"] = recipe["name"][lang]
        source = recipe["source"]
        readme.append(f"### {recipe['name']['pl_pl']}\n")
        readme.append(f"Źródło: [{source['title']}]({source['url']}) ({source['author']}, Minecraft {source['minecraft']}).\n")
        readme.append("| Przedmiot | Komenda |\n|---|---|")
        for entry in recipe["items"]:
            item = {"id": entry["id"]}
            for key in ("stack", "glint", "color", "bold"):
                if key in entry:
                    item[key] = entry[key]
            items.append(item)
            for lang in LANGS:
                langs[lang][f"item.{NS}.{entry['id']}"] = entry["name"][lang]
            readme.append(f"| {entry['name']['pl_pl']} | `/give @s {NS}:{entry['id']}` |")
        readme.append("")
        packs.append({"id": pack, **({"icon": recipe["icon"]} if "icon" in recipe else {}), "items": items})

    save_json(catalog_path, {"packs": packs})
    for lang, entries in langs.items():
        save_json(f"{ASSETS}/{NS}/lang/{lang}.json", entries)

    with open(README, encoding="utf-8") as f:
        text = f.read()
    start, end = "<!-- items:start -->\n", "<!-- items:end -->"
    before, rest = text.split(start, 1)
    after = rest.split(end, 1)[1]
    with open(README, "w", encoding="utf-8") as f:
        f.write(before + start + "\n".join(readme) + "\n" + end + after)


def main():
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    recipe = load_json(sys.argv[1])
    pack = recipe["id"]
    old = next((p for p in load_json(f"{RES}/reckit/catalog.json")["packs"] if p["id"] == pack), {"items": []})
    remove_pack(pack, [item["id"] for item in old["items"]] + [item["id"] for item in recipe["items"]])

    with tempfile.TemporaryDirectory() as temp:
        importer = Importer(recipe, resource_root(sys.argv[2], temp), VanillaAssets())
        for entry in recipe["items"]:
            importer.item(entry)

    if importer.atlas:
        atlas = load_json(ATLAS) if os.path.isfile(ATLAS) else {"sources": []}
        known = {source.get("resource") for source in atlas["sources"]}
        atlas["sources"] += [{"type": "minecraft:single", "resource": r} for r in sorted(importer.atlas - known)]
        save_json(ATLAS, atlas)

    regenerate()
    for problem in importer.problems:
        print("warning:", problem)
    print(f"{pack}: {len(recipe['items'])} props, {len(importer.models)} models, {len(importer.textures)} textures")


if __name__ == "__main__":
    main()
