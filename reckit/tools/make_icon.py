#!/usr/bin/env python3
"""Draws the mod icon: a pixel-art film clapperboard (32x32, scaled up to 128x128).

Run from the reckit/ folder: python3 tools/make_icon.py
"""
from PIL import Image

OUT = "src/main/resources/assets/reckit/icon.png"

BLACK = (34, 34, 38, 255)
DARK = (58, 58, 66, 255)
WHITE = (236, 236, 228, 255)
RED = (214, 52, 52, 255)
GOLD = (240, 196, 64, 255)


def main():
    img = Image.new("RGBA", (32, 32))
    px = img.load()

    # Slate body with an outline.
    for y in range(12, 29):
        for x in range(3, 29):
            px[x, y] = BLACK if x in (3, 28) or y in (12, 28) else DARK
    # Chalk lines on the slate.
    for y in (17, 21, 25):
        for x in range(6, 26):
            px[x, y] = WHITE if (x + y) % 9 else DARK
    # Red record dot.
    for y in range(14, 17):
        for x in range(23, 26):
            px[x, y] = RED
    # Striped clapper stick, opened upwards.
    for i in range(26):
        x = 3 + i
        top = 9 - i * 6 // 25
        for y in range(top - 3, top + 1):
            stripe = ((x + (9 - y)) // 3) % 2
            px[x, y] = BLACK if y in (top - 3, top) else (WHITE if stripe else BLACK)
    # Gold hinge.
    for y in range(8, 12):
        for x in range(3, 6):
            px[x, y] = GOLD

    img.resize((128, 128), Image.NEAREST).save(OUT)


if __name__ == "__main__":
    main()
