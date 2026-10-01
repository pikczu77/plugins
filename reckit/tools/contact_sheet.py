#!/usr/bin/env python3
"""Builds one overview image per pack from the client game test screenshots: every prop held in first and third
person, with its id underneath.

Run from the reckit/ folder after ./gradlew runClientGameTest:
    python3 tools/contact_sheet.py [screenshots dir] [output dir]
"""
import glob
import json
import os
import sys

from PIL import Image, ImageDraw, ImageFont

CATALOG = "src/main/resources/reckit/catalog.json"
CELL = (320, 180)
COLUMNS = 3
LABEL = 20
# Third person: the player stands in the middle of a 1280x720 screenshot.
CROPS = {"1st": None, "3rd": (400, 290, 880, 560)}
FONT = ImageFont.load_default(size=14)


def shot(folder, name):
    files = sorted(glob.glob(os.path.join(folder, f"*{name}.png")))
    return Image.open(files[-1]).convert("RGB") if files else None


def main():
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
    folder = sys.argv[1] if len(sys.argv) > 1 else "build/run/clientGameTest/screenshots"
    out = sys.argv[2] if len(sys.argv) > 2 else "build/contact-sheets"
    os.makedirs(out, exist_ok=True)

    with open(CATALOG, encoding="utf-8") as f:
        packs = json.load(f)["packs"]

    for pack in packs:
        ids = [item["id"] for item in pack["items"]]
        rows = (len(ids) + COLUMNS - 1) // COLUMNS
        width, height = CELL[0] * 2 * COLUMNS, (CELL[1] + LABEL) * rows
        sheet = Image.new("RGB", (width, height), (24, 24, 28))
        draw = ImageDraw.Draw(sheet)

        for index, item_id in enumerate(ids):
            x = (index % COLUMNS) * CELL[0] * 2
            y = (index // COLUMNS) * (CELL[1] + LABEL)
            for offset, (view, crop) in enumerate(CROPS.items()):
                image = shot(folder, f"reckit-{pack['id']}-{item_id}-{view}")
                if image:
                    image = image.crop(crop) if crop else image
                    sheet.paste(image.resize(CELL, Image.LANCZOS), (x + offset * CELL[0], y))
            draw.text((x + 4, y + CELL[1] + 2), f"reckit:{item_id}", fill=(236, 236, 228), font=FONT)

        path = os.path.join(out, f"{pack['id']}.png")
        sheet.save(path)
        print(path)


if __name__ == "__main__":
    main()
