#!/usr/bin/env python3
"""Generates the mod's original pixel-art textures (16x16) and the mod icon.

Run from the upgrades/ folder: python3 tools/make_textures.py
"""
import os
import random
from PIL import Image

OUT = "src/client/resources/assets/upgrades/textures/item"
ICON = "src/main/resources/assets/upgrades/icon.png"


def hexc(value, alpha=255):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def noise_fill(img, palette, seed, weights=None):
    rnd = random.Random(seed)
    w, h = img.size
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), hexc(rnd.choices(palette, weights=weights)[0]))


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))


def golem_arm():
    # Pale iron plates with rivets, cracks and a creeping vine.
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, ["#D9D4CC", "#CFC9BF", "#E4E0D8", "#C4BDB1"], 1, [4, 4, 2, 1])
    for y in (5, 11):
        for x in range(16):
            img.putpixel((x, y), hexc("#A79F92"))
    for x, y in ((2, 2), (13, 2), (2, 8), (13, 8), (2, 14), (13, 14)):
        img.putpixel((x, y), hexc("#8E8577"))
    for x, y in ((6, 1), (7, 2), (7, 3), (8, 4), (9, 7), (10, 8)):
        img.putpixel((x, y), hexc("#7D7466"))
    vine = [(3, 15), (3, 14), (4, 13), (4, 12), (5, 11), (5, 10), (4, 9), (4, 8), (5, 7), (6, 6)]
    for x, y in vine:
        img.putpixel((x, y), hexc("#3F7A2A"))
    for x, y in ((2, 13), (5, 12), (3, 9), (6, 8), (7, 6)):
        img.putpixel((x, y), hexc("#5FA83A"))
    save(img, "golem_arm")

    # The fist: darker plates with finger lines.
    fist = Image.new("RGBA", (16, 16))
    noise_fill(fist, ["#C9C2B6", "#BDB5A7", "#D3CDC2"], 2)
    for x in (4, 8, 12):
        for y in range(16):
            fist.putpixel((x, y), hexc("#978E80"))
    save(fist, "golem_fist")


def skin():
    # Warm skin tone for the villager-like nose.
    img = Image.new("RGBA", (16, 16))
    # Redder than a player's face, so the nose stands out.
    noise_fill(img, ["#D9826A", "#CC735C", "#E3927A", "#BF6A52"], 3, [5, 3, 2, 1])
    for y in range(16):
        img.putpixel((0, y), hexc("#9E4F3A"))
        img.putpixel((15, y), hexc("#9E4F3A"))
    for x, y in ((6, 12), (9, 12), (6, 13), (9, 13)):
        img.putpixel((x, y), hexc("#6E3222"))
    save(img, "nose_skin")


def obsidian():
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, ["#140F1F", "#1D1530", "#0E0A16", "#2A1F45", "#3B2A63"], 4, [6, 5, 4, 2, 1])
    for x, y in ((3, 4), (4, 4), (10, 9), (11, 10), (6, 13)):
        img.putpixel((x, y), hexc("#6B4FB0"))
    save(img, "horn_obsidian")


def wing():
    # Membrane: dark purple-black with thin veins; bones: pale grey. Transparent outside the wing shape.
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            # Scalloped trailing edge: the lower edge rises towards the tip.
            edge = 15 - (x // 4) * 2 - (1 if x % 4 in (1, 2) else 0)
            if y <= edge and y >= 2 - x // 8:
                c = rnd.choice(["#231A2E", "#1B1424", "#2C2138", "#1F1729"])
                img.putpixel((x, y), hexc(c, 235))
    # Bones: the top bar and three fingers.
    for x in range(16):
        img.putpixel((x, 2 - x // 8 if x < 8 else 1), hexc("#B9B4AA"))
        img.putpixel((x, 3 - x // 8 if x < 8 else 2), hexc("#8E897F"))
    for start in (3, 8, 13):
        for y in range(3, 16):
            x = start - (y - 3) // 3
            if 0 <= x < 16 and img.getpixel((x, y))[3] > 0:
                img.putpixel((x, y), hexc("#9C968C"))
    save(img, "dragon_wing")


def portal():
    # Animated swirl (4 frames, stacked vertically) for the pocket portal inside.
    frames = 4
    img = Image.new("RGBA", (16, 16 * frames))
    for f in range(frames):
        rnd = random.Random(10 + f)
        for y in range(16):
            for x in range(16):
                band = (x + y * 2 + f * 4) % 16
                base = ["#5A1BB0", "#7A2BE0", "#9A4CF0", "#6A20C8"][band // 4]
                if rnd.random() < 0.18:
                    base = "#C79BFF"
                img.putpixel((x, y + f * 16), hexc(base, 220))
    save(img, "portal_swirl")
    with open(os.path.join(OUT, "portal_swirl.png.mcmeta"), "w") as meta:
        meta.write('{\n\t"animation": {\n\t\t"frametime": 3\n\t}\n}\n')


def metal(name, palette, seed, trim):
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, palette, seed, [5, 3, 2, 1])
    for x in range(16):
        img.putpixel((x, 0), hexc(trim))
        img.putpixel((x, 15), hexc(trim))
    for y in range(16):
        img.putpixel((0, y), hexc(trim))
        img.putpixel((15, y), hexc(trim))
    save(img, name)


def wood(name, palette, seed):
    img = Image.new("RGBA", (16, 16))
    rnd = random.Random(seed)
    for y in range(16):
        row = rnd.choice(palette)
        for x in range(16):
            c = row if rnd.random() > 0.2 else rnd.choice(palette)
            img.putpixel((x, y), hexc(c))
    save(img, name)


def cloth(name, palette, seed, stripe=None):
    img = Image.new("RGBA", (16, 16))
    noise_fill(img, palette, seed, [6, 3, 1][:len(palette)])
    if stripe:
        for x in range(16):
            img.putpixel((x, 2), hexc(stripe))
            img.putpixel((x, 13), hexc(stripe))
    save(img, name)


def shield_face():
    # Wooden round-ish plate with a metal boss and rim (transparent corners).
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rnd = random.Random(20)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            if dx * dx + dy * dy <= 60:
                if dx * dx + dy * dy >= 44:
                    c = rnd.choice(["#8F969C", "#A4ABB1"])
                else:
                    c = rnd.choice(["#8B5A2B", "#7A4E25", "#9A6634"])
                img.putpixel((x, y), hexc(c))
    for y in range(6, 10):
        for x in range(6, 10):
            img.putpixel((x, y), hexc(rnd.choice(["#C7CDD2", "#B0B7BD"])))
    img.putpixel((7, 7), hexc("#EEF1F3"))
    save(img, "mini_shield")


def icon():
    # 64x64 mod icon: a green upward arrow over a stylised body silhouette with parts sticking out.
    size = 64
    img = Image.new("RGBA", (size, size), hexc("#1E2A1E"))
    rnd = random.Random(30)
    for y in range(size):
        for x in range(size):
            if rnd.random() < 0.08:
                img.putpixel((x, y), hexc("#243324"))
    def rect(x0, y0, x1, y1, color):
        for y in range(y0, y1):
            for x in range(x0, x1):
                img.putpixel((x, y), hexc(color))
    # Body silhouette (generic mannequin, grey).
    rect(26, 14, 38, 26, "#9AA0A6")
    rect(24, 27, 40, 45, "#7E858C")
    rect(18, 27, 24, 43, "#9AA0A6")
    rect(40, 27, 46, 43, "#9AA0A6")
    rect(25, 45, 31, 60, "#6C737A")
    rect(33, 45, 39, 60, "#6C737A")
    # A pickaxe chain from the shoulder.
    for i in range(12):
        img.putpixel((46 + i, 29 - i // 3), hexc("#8B5A2B"))
    rect(56, 22, 59, 30, "#5FE0D8")
    # Hoe on the head.
    rect(31, 4, 33, 14, "#8B5A2B")
    rect(27, 3, 33, 5, "#C9C9C9")
    # Wing behind.
    for y in range(18, 34):
        for x in range(4, 18 - (y - 18) // 2):
            img.putpixel((x, y), hexc("#2C2138"))
    # Green arrow.
    for y in range(6):
        rect(10 - y, 4 + y, 11 + y, 5 + y, "#55FF55")
    rect(7, 10, 14, 16, "#55FF55")
    img = img.resize((128, 128), Image.NEAREST)
    os.makedirs(os.path.dirname(ICON), exist_ok=True)
    img.save(ICON)


if __name__ == "__main__":
    golem_arm()
    skin()
    obsidian()
    wing()
    portal()
    metal("mannequin_iron", ["#DADADA", "#CBCBCB", "#E8E8E8", "#BDBDBD"], 40, "#9E9E9E")
    metal("mannequin_diamond", ["#4AE0D6", "#39CFC6", "#77EEE6", "#2BB5AE"], 41, "#1E8A85")
    metal("mannequin_netherite", ["#4A4148", "#3D353C", "#574D55", "#312A30"], 42, "#241F23")
    wood("stand_wood", ["#9C7A4C", "#8E6D42", "#A8855A"], 43)
    wood("bed_wood", ["#A0764A", "#946B40", "#AD8155"], 44)
    cloth("bed_sheet", ["#F2F2F2", "#E4E4E4", "#FFFFFF"], 45)
    cloth("bed_blanket", ["#C0302A", "#A82822", "#D0443C"], 46, "#F2F2F2")
    shield_face()
    icon()
    print("textures written")
