#!/usr/bin/env python3
"""Draws every texture of the Block Opener mod (original pixel art, no vanilla assets used).

Run from the project root:  python3 tools/generate_textures.py   (needs Pillow)

Sprites are written as small character grids: every character is a material, "." is empty.
Materials get Minecraft-style shading automatically (light top-left edges, dark bottom-right
edges, a darker outline around the shape), so tweaking a shape is just editing the grid.
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "blockopener")
TEX = os.path.join(ROOT, "textures")


def rgb(value, alpha=255):
    value = value.lstrip("#")
    return int(value[0:2], 16), int(value[2:4], 16), int(value[4:6], 16), alpha


def shade(color, amount):
    r, g, b, a = color
    clamp = lambda v: max(0, min(255, int(v)))
    return clamp(r + amount), clamp(g + amount), clamp(b + amount), a


class Mat:
    def __init__(self, light, base, dark, outline, noise=6):
        self.light, self.base, self.dark, self.outline = rgb(light), rgb(base), rgb(dark), rgb(outline)
        self.noise = noise


def save(img, *path):
    full = os.path.join(TEX, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def render(grid, mats, fixed=None, seed=0):
    """grid: list of strings; mats: char -> Mat; fixed: char -> RGBA (no shading)."""
    fixed = fixed or {}
    h, w = len(grid), len(grid[0])
    rnd = random.Random(seed)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))

    def filled(x, y):
        return 0 <= x < w and 0 <= y < h and grid[y][x] != "."

    for y in range(h):
        for x in range(w):
            c = grid[y][x]
            if c == ".":
                continue
            if c in fixed:
                img.putpixel((x, y), fixed[c])
                continue
            m = mats[c]
            score = (not filled(x, y - 1)) + (not filled(x - 1, y)) - (not filled(x, y + 1)) - (not filled(x + 1, y))
            color = m.light if score > 0 else m.dark if score < 0 else m.base
            img.putpixel((x, y), shade(color, rnd.randint(-m.noise, m.noise)))
    # outline
    out = img.copy()
    for y in range(h):
        for x in range(w):
            if filled(x, y):
                continue
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                nx, ny = x + dx, y + dy
                if filled(nx, ny):
                    c = grid[ny][nx]
                    out.putpixel((x, y), mats[c].outline if c in mats else OUTLINE_FIXED)
                    break
    return out


OUTLINE_FIXED = rgb("#1b0f2a")


def grid_from(fn, w=16, h=16):
    return ["".join(fn(x, y) for x in range(w)) for y in range(h)]


# ================================================================= items

def block_opener():
    grid = [
        "................",
        "................",
        "..........aaa...",
        ".........aaaaa..",
        "........aa...aa.",
        ".......aa....a..",
        "......aa........",
        ".....rr.........",
        "....oo..........",
        "...yy...........",
        "..gg............",
        ".cc.............",
        ".b..............",
        "................",
        "................",
        "................",
    ]
    outline = "#2b0a55"
    mats = {
        "a": Mat("#e6c9ff", "#a65dff", "#6b2bd1", outline, 4),
        "r": Mat("#ff9e9e", "#ff4f4f", "#c42727", outline, 2),
        "o": Mat("#ffc98f", "#ff9433", "#c9661a", outline, 2),
        "y": Mat("#fff3a1", "#ffe03d", "#c9a814", outline, 2),
        "g": Mat("#a8ffb6", "#3ee85a", "#1f9e36", outline, 2),
        "c": Mat("#aef4ff", "#39c8ff", "#1b86b8", outline, 2),
        "b": Mat("#b9b6ff", "#6f68ff", "#4038c2", outline, 2),
    }
    img = render(grid, mats, seed=1)
    img.putpixel((11, 3), rgb("#ffffff"))
    return img


def pumpkin_boots():
    grid = [
        "................",
        "................",
        "................",
        "..gg........gg..",
        "..ssss....ssss..",
        "..pvpp....ppvp..",
        "..pvpp....ppvp..",
        "..pvpp....ppvp..",
        "..pvpp....ppvp..",
        ".ppvpp....ppvpp.",
        ".ppvpp....ppvpp.",
        ".yyyyy....yyyyy.",
        "................",
        "................",
        "................",
        "................",
    ]
    outline = "#4a2206"
    mats = {
        "p": Mat("#ffb24a", "#e8862a", "#b85c14", outline),
        "v": Mat("#d8772a", "#c2651a", "#9a4e10", outline, 3),
        "s": Mat("#b07a45", "#8a5a2b", "#5e3a18", outline),
        "g": Mat("#86d152", "#4f9a2a", "#2f6618", "#173a0a"),
        "y": Mat("#fff6a8", "#ffd84a", "#e0a820", outline, 2),
    }
    return render(grid, mats, seed=2)


def bee_drill():
    tip = (13.6, 1.4)
    axis = (-1 / math.sqrt(2), 1 / math.sqrt(2))
    length = 8.2

    def cell(x, y):
        px, py = x + 0.5 - tip[0], y + 0.5 - tip[1]
        t = px * axis[0] + py * axis[1]
        s = abs(px * axis[1] - py * axis[0])
        if 0 <= t <= length and s <= t * 0.36 + 0.35:
            return "y" if int((t + 0.4) / 1.55) % 2 == 0 else "k"
        if length < t <= length + 1.6 and s <= 2.6:
            return "h"
        if length + 1.6 < t <= length + 8.2 and s <= 0.72:
            return "w"
        return "."

    grid = grid_from(cell)
    outline = "#2a1a02"
    mats = {
        "y": Mat("#fff08a", "#ffc928", "#d18c0c", outline, 3),
        "k": Mat("#4a4035", "#2b241c", "#171310", outline, 2),
        "h": Mat("#ffd98a", "#f0a92e", "#b8740f", outline, 3),
        "w": Mat("#b88452", "#8a5a31", "#5c3a1d", outline, 3),
    }
    img = render(grid, mats, seed=3)
    # little wings on the collar
    for x, y in [(6, 5), (7, 5), (6, 4), (10, 9), (11, 9), (11, 10)]:
        img.putpixel((x, y), rgb("#e8f6ff", 220))
    return img


def anvil_chestplate():
    grid = [
        "................",
        "..SS........SS..",
        ".SSSS......SSSS.",
        ".SSSSS....SSSSS.",
        ".SSSBBBBBBBBSSS.",
        ".SS.BBBBBBBB.SS.",
        "....BeeeeeeB....",
        "....BBBeeBBB....",
        "....BBeeeeBB....",
        "....BBBBBBBB....",
        "....BBBBBBBB....",
        "....BBBBBBBB....",
        "....bbbbbbbb....",
        "................",
        "................",
        "................",
    ]
    mats = {
        "S": Mat("#8a8a8a", "#5a5a5a", "#3a3a3a", "#141414"),
        "B": Mat("#e0e0e0", "#b0b0b0", "#7a7a7a", "#1d1d1d"),
        "e": Mat("#5a5a5a", "#3c3c3c", "#262626", "#141414", 2),
        "b": Mat("#7a7a7a", "#5c5c5c", "#404040", "#141414"),
    }
    img = render(grid, mats, seed=4)
    for x, y in [(2, 3), (13, 3), (5, 11), (10, 11)]:
        img.putpixel((x, y), rgb("#f4f4f4"))
    return img


def diamond_leggings():
    grid = [
        "................",
        "................",
        "..gggggmmggggg..",
        "..LLLLLLLLLLLL..",
        "..LLLLL..LLLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "..LLLL....LLLL..",
        "................",
        "................",
        "................",
    ]
    mats = {
        "L": Mat("#c4fff7", "#4aedd9", "#1fa89a", "#0c3f3a"),
        "g": Mat("#fff39a", "#f5c542", "#b98a1a", "#4a3408", 3),
        "m": Mat("#ff9cf5", "#d63cff", "#8a17b8", "#3a0a4a", 2),
    }
    img = render(grid, mats, seed=5)
    for x, y in [(4, 6), (11, 9), (3, 11), (12, 5)]:
        img.putpixel((x, y), rgb("#ffffff"))
    return img


def dripstone_sword():
    guard, tip = (5.0, 11.0), (14.2, 1.8)
    length = math.dist(guard, tip)
    axis = ((tip[0] - guard[0]) / length, (tip[1] - guard[1]) / length)

    def cell(x, y):
        px, py = x + 0.5 - guard[0], y + 0.5 - guard[1]
        t = px * axis[0] + py * axis[1]
        side = px * axis[1] - py * axis[0]
        s = abs(side)
        if 0.6 <= t <= length:
            width = 1.55 * (1 - t / length) + 0.45
            # stalactite notches: little spikes sticking out every ~2.4 blocks, alternating sides
            spike = 0.9 if (int(t / 2.4) % 2 == 0) == (side > 0) and (t % 2.4) < 0.9 and t < length - 1.5 else 0.0
            if s <= width + spike:
                return "b" if s <= width * 0.55 else "d"
        if -0.6 <= t < 0.6 and s <= 2.8:
            return "q"
        if -4.2 <= t < -0.6 and s <= 0.6:
            return "h"
        if -5.3 <= t < -4.2 and s <= 0.9:
            return "p"
        return "."

    grid = grid_from(cell)
    mats = {
        "b": Mat("#ead0b4", "#c09a7c", "#8e6a54", "#34200f"),
        "d": Mat("#b48a70", "#98725c", "#6e4f3f", "#34200f"),
        "q": Mat("#9c7a66", "#6e4f3f", "#4a3226", "#1f130b"),
        "h": Mat("#6b4a36", "#4a2f22", "#2e1c14", "#140b06"),
        "p": Mat("#c9a88c", "#9a7a62", "#6a4e3e", "#1f130b"),
    }
    return render(grid, mats, seed=6)


def piston_launcher():
    start, end = (3.0, 13.0), (9.6, 6.4)
    axis = ((end[0] - start[0]) / math.dist(start, end), (end[1] - start[1]) / math.dist(start, end))
    length = math.dist(start, end)

    def cell(x, y):
        px, py = x + 0.5 - start[0], y + 0.5 - start[1]
        t = px * axis[0] + py * axis[1]
        s = abs(px * axis[1] - py * axis[0])
        if 0 <= t <= length and s <= 1.6:
            return "s"
        if length < t <= length + 1.2 and s <= 0.7:
            return "r"
        if length + 1.2 < t <= length + 3.0 and s <= 3.6:
            return "h"
        if 0.4 <= t <= 2.4 and 1.6 < s <= 3.6 and (px * axis[1] - py * axis[0]) < 0:
            return "w"
        return "."

    grid = grid_from(cell)
    mats = {
        "s": Mat("#a4a4a4", "#7a7a7a", "#555555", "#1e1e1e"),
        "r": Mat("#e8d6b0", "#c9b48a", "#93815e", "#2e2414", 2),
        "h": Mat("#e0c08a", "#bf9a5e", "#8a6a3a", "#2e2010"),
        "w": Mat("#9a6a40", "#6e4724", "#4a2e14", "#1a0f05"),
    }
    img = render(grid, mats, seed=7)
    img.putpixel((6, 10), rgb("#ff2a2a"))
    img.putpixel((7, 9), rgb("#b30000"))
    return img


def copper_magnet():
    grid = [
        "................",
        "................",
        "..ttt......ttt..",
        "..ttt......ttt..",
        "..rrr......rrr..",
        "..ccc......ccc..",
        "..ccc......ccc..",
        "..ccc......ccc..",
        "..ccc......ccc..",
        "..cccc....cccc..",
        "...cccccccccc...",
        "....cccccccc....",
        "................",
        "................",
        "................",
        "................",
    ]
    mats = {
        "t": Mat("#ffffff", "#d6dde2", "#9aa6ad", "#2d3438", 2),
        "r": Mat("#ff8a8a", "#e23c3c", "#a81f1f", "#3d0808", 2),
        "c": Mat("#f7a57a", "#e0754f", "#a8492a", "#4a1c0c"),
    }
    img = render(grid, mats, seed=8)
    for x, y in [(3, 7), (12, 8), (8, 10)]:
        img.putpixel((x, y), rgb("#57b89c"))
    for x, y in [(3, 0), (12, 0), (1, 1), (14, 1)]:
        img.putpixel((x, y), rgb("#fff176"))
    return img


def bedrock_bucket():
    grid = [
        "................",
        "................",
        "....hhhhhhhh....",
        "...h........h...",
        "..bbbbbbbbbbbb..",
        "..bVVVVVVVVVVb..",
        "..bbVVVVVVVVbb..",
        "...bbbbbbbbbb...",
        "...bbbbbbbbbb...",
        "...bbbbbbbbbb...",
        "....bbbbbbbb....",
        "....bbbbbbbb....",
        "....bbbbbbbb....",
        "................",
        "................",
        "................",
    ]
    rnd = random.Random(9)
    mats = {
        "b": Mat("#9a9a9a", "#6b6b6b", "#3f3f3f", "#121212", 22),
        "h": Mat("#8a8a8a", "#5a5a5a", "#383838", "#121212", 4),
    }
    stars = {(5, 5), (9, 5), (7, 6), (11, 5)}
    fixed = {"V": rgb("#0a0612")}
    img = render(grid, mats, fixed, seed=9)
    for y in range(16):
        for x in range(16):
            if grid[y][x] == "V":
                if (x, y) in stars:
                    img.putpixel((x, y), rgb("#d9b8ff") if rnd.random() < 0.5 else rgb("#9b59ff"))
                elif rnd.random() < 0.3:
                    img.putpixel((x, y), rgb("#22103a"))
    return img


def sculk_helmet():
    grid = [
        "................",
        "................",
        ".h............h.",
        ".hh..HHHHHH..hh.",
        "..hHHHHHHHHHHh..",
        "...HHHHHHHHHH...",
        "...HHHHHHHHHH...",
        "...HHH....HHH...",
        "...HH......HH...",
        "...HH......HH...",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    mats = {
        "H": Mat("#1f5c66", "#0f3f4a", "#082a33", "#02141a", 4),
        "h": Mat("#1f5c66", "#0f3f4a", "#082a33", "#02141a", 2),
    }
    img = render(grid, mats, seed=10)
    for x, y in [(5, 4), (10, 4), (7, 5), (4, 6), (11, 6), (8, 3)]:
        img.putpixel((x, y), rgb("#35e6f2"))
    for x, y in [(1, 2), (14, 2), (7, 4)]:
        img.putpixel((x, y), rgb("#c8ffff"))
    return img


def mossphere():
    rnd = random.Random(11)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    center, radius = (7.5, 7.5), 5.7
    inside = lambda x, y: math.dist((x + 0.5, y + 0.5), (center[0] + 0.5, center[1] + 0.5)) <= radius
    light, base, dark = rgb("#a6e06a"), rgb("#62a332"), rgb("#34611a")
    for y in range(16):
        for x in range(16):
            if not inside(x, y):
                continue
            # light from the top-left
            d = math.dist((x, y), (4.5, 4.5)) / 11.0
            color = light if d < 0.35 else base if d < 0.75 else dark
            color = shade(color, rnd.randint(-10, 10))
            if rnd.random() < 0.12:
                color = rgb("#d4ff6b")
            img.putpixel((x, y), color)
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if inside(x, y):
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and inside(x + dx, y + dy) for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1))):
                out.putpixel((x, y), rgb("#1a330b"))
    out.putpixel((5, 4), rgb("#f0ffd0"))
    out.putpixel((4, 5), rgb("#e0ffb0"))
    return out


ITEMS = {
    "block_opener": block_opener,
    "pumpkin_boots": pumpkin_boots,
    "bee_drill": bee_drill,
    "anvil_chestplate": anvil_chestplate,
    "diamond_leggings": diamond_leggings,
    "dripstone_sword": dripstone_sword,
    "piston_launcher": piston_launcher,
    "copper_magnet": copper_magnet,
    "bedrock_bucket": bedrock_bucket,
    "sculk_helmet": sculk_helmet,
    "mossphere": mossphere,
}


def silhouette(img):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(img.height):
        for x in range(img.width):
            if img.getpixel((x, y))[3] > 40:
                out.putpixel((x, y), (18, 14, 24, 230))
    return out


# ================================================================= armor layers (64x32)

def paint_face(img, x0, y0, w, h, color_fn):
    for y in range(h):
        for x in range(w):
            color = color_fn(x, y, w, h)
            if color is not None:
                img.putpixel((x0 + x, y0 + y), color)


def textured(base, rnd, amount=8):
    return shade(base, rnd.randint(-amount, amount))


def sculk_helmet_layer():
    rnd = random.Random(21)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    base, edge = rgb("#0f3f4a"), rgb("#072630")

    def fn(x, y, w, h):
        if rnd.random() < 0.09:
            return rgb("#35e6f2") if rnd.random() < 0.7 else rgb("#c8ffff")
        if x in (0, w - 1) or y in (0, h - 1):
            return textured(edge, rnd, 4)
        return textured(base, rnd, 6)

    for x0, y0 in [(8, 0), (16, 0), (0, 8), (16, 8), (24, 8)]:
        paint_face(img, x0, y0, 8, 8, fn)

    def front(x, y, w, h):
        if 1 <= x <= 6 and y >= 3:
            return None  # face opening
        return fn(x, y, w, h)

    paint_face(img, 8, 8, 8, 8, front)
    return img


def anvil_chestplate_layer():
    rnd = random.Random(22)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    iron, dark, rivet = rgb("#a9a9a9"), rgb("#5a5a5a"), rgb("#e8e8e8")

    def plate(x, y, w, h):
        if y == 0 or y == h - 1 or x in (0, w - 1):
            return textured(dark, rnd, 4)
        if (x, y) in ((1, 1), (w - 2, 1)):
            return rivet
        return textured(iron, rnd, 7)

    # body: front, back, sides, top, bottom
    for x0, y0, w, h in [(20, 20, 8, 12), (32, 20, 8, 12), (16, 20, 4, 12), (28, 20, 4, 12), (20, 16, 8, 4), (28, 16, 8, 4)]:
        paint_face(img, x0, y0, w, h, plate)
    # anvil emblem on the front
    emblem = ["......", "######", ".####.", "..##..", ".####."]
    for y, row in enumerate(emblem):
        for x, c in enumerate(row):
            if c == "#":
                img.putpixel((21 + x, 22 + y), rgb("#3a3a3a"))
    # shoulder pads on the arms (upper 8 rows)
    shoulder = rgb("#6a6a6a")

    def pad(x, y, w, h):
        if y >= 8:
            return None
        if y == 7 or x in (0, w - 1):
            return textured(rgb("#3f3f3f"), rnd, 3)
        return textured(shoulder, rnd, 6)

    for x0 in (40, 44, 48, 52):
        paint_face(img, x0, 20, 4, 12, pad)
    paint_face(img, 44, 16, 4, 4, lambda x, y, w, h: textured(shoulder, rnd, 6))
    return img


def pumpkin_boots_layer():
    rnd = random.Random(23)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    orange, ridge, cuff, sole = rgb("#e8862a"), rgb("#b85c14"), rgb("#8a5a2b"), rgb("#ffd84a")

    def boot(x, y, w, h):
        if y < 6:
            return None
        if y == 6:
            return textured(cuff, rnd, 5)
        if y == h - 1:
            return textured(sole, rnd, 4)
        if x % 2 == 1:
            return textured(ridge, rnd, 4)
        return textured(orange, rnd, 7)

    for x0 in (0, 4, 8, 12):
        paint_face(img, x0, 20, 4, 12, boot)
    paint_face(img, 8, 16, 4, 4, lambda x, y, w, h: textured(sole, rnd, 4))
    img.putpixel((5, 26), rgb("#4f9a2a"))
    img.putpixel((6, 26), rgb("#2f6618"))
    return img


def diamond_leggings_layer():
    rnd = random.Random(24)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    diamond, edge, belt, gem = rgb("#4aedd9"), rgb("#1fa89a"), rgb("#f5c542"), rgb("#d63cff")

    def waist(x, y, w, h):
        if y < 7:
            return None
        if y == 7:
            return textured(belt, rnd, 5)
        if rnd.random() < 0.06:
            return rgb("#ffffff")
        return textured(diamond if x not in (0, w - 1) else edge, rnd, 6)

    for x0, w in ((20, 8), (32, 8), (16, 4), (28, 4)):
        paint_face(img, x0, 20, w, 12, waist)
    img.putpixel((23, 27), gem)
    img.putpixel((24, 27), gem)

    def leg(x, y, w, h):
        if y >= 10:
            return None
        if rnd.random() < 0.06:
            return rgb("#ffffff")
        return textured(edge if x in (0, w - 1) or y == 9 else diamond, rnd, 6)

    for x0 in (0, 4, 8, 12):
        paint_face(img, x0, 20, 4, 12, leg)
    paint_face(img, 4, 16, 4, 4, lambda x, y, w, h: textured(diamond, rnd, 6))
    return img


# ================================================================= void water (animated)

def void_frames(size, frames, flow):
    rnd = random.Random(31 if flow else 30)
    stars = [(rnd.random() * size, rnd.random() * size, rnd.random()) for _ in range(size // 2)]
    blobs = [(rnd.random() * size, rnd.random() * size, 2 + rnd.random() * size / 4) for _ in range(6)]
    sheet = Image.new("RGBA", (size, size * frames))
    for f in range(frames):
        phase = f / frames
        for y in range(size):
            for x in range(size):
                v = 0.0
                for bx, by, br in blobs:
                    oy = (by + phase * size) % size if flow else by
                    ox = (bx + math.sin(phase * math.tau + by) * 1.5) % size
                    dx = min(abs(x - ox), size - abs(x - ox))
                    dy = min(abs(y - oy), size - abs(y - oy))
                    v += max(0.0, 1 - math.hypot(dx, dy) / br)
                v = min(1.0, v)
                r = int(8 + 50 * v)
                g = int(3 + 12 * v)
                b = int(16 + 90 * v)
                sheet.putpixel((x, y + f * size), (r, g, b, 235))
        for sx, sy, sp in stars:
            twinkle = 0.5 + 0.5 * math.sin((phase + sp) * math.tau)
            y = int((sy + (phase * size if flow else 0)) % size)
            x = int(sx)
            if twinkle > 0.55:
                c = (int(170 + 85 * twinkle), int(140 + 90 * twinkle), 255, 255)
                sheet.putpixel((x, y + f * size), c)
    return sheet


def write_mcmeta(path, frametime):
    with open(os.path.join(TEX, path + ".mcmeta"), "w") as f:
        f.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)


# ================================================================= mod icon

def icon(items):
    size = 128
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size)
            img.putpixel((x, y), (int(60 + 60 * t), int(20 + 10 * t), int(110 + 60 * t), 255))
    opener = items["block_opener"].resize((96, 96), Image.NEAREST)
    img.alpha_composite(opener, (16, 16))
    for name, pos in (("pumpkin_boots", (4, 84)), ("mossphere", (86, 4)), ("sculk_helmet", (86, 86))):
        img.alpha_composite(items[name].resize((40, 40), Image.NEAREST), pos)
    return img


def main():
    items = {}
    for name, fn in ITEMS.items():
        items[name] = fn()
        save(items[name], "item", name + ".png")
        if name != "block_opener":
            save(silhouette(items[name]), "gui", "sprites", "tracker", name + ".png")

    save(sculk_helmet_layer(), "entity", "equipment", "humanoid", "sculk_helmet.png")
    save(anvil_chestplate_layer(), "entity", "equipment", "humanoid", "anvil_chestplate.png")
    save(pumpkin_boots_layer(), "entity", "equipment", "humanoid", "pumpkin_boots.png")
    save(diamond_leggings_layer(), "entity", "equipment", "humanoid_leggings", "diamond_leggings.png")

    save(void_frames(16, 32, False), "block", "void_water_still.png")
    write_mcmeta(os.path.join("block", "void_water_still.png"), 2)
    save(void_frames(32, 32, True), "block", "void_water_flow.png")
    write_mcmeta(os.path.join("block", "void_water_flow.png"), 1)

    icon(items).save(os.path.join(ROOT, "icon.png"))
    print("textures generated")


if __name__ == "__main__":
    main()
