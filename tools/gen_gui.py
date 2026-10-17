#!/usr/bin/env python3
"""Pixel-art GUI textures (nine-slice panels, buttons, frames, icons) for the KonoCraft screens."""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src/main/resources/assets/konocraft/textures/gui")
os.makedirs(OUT, exist_ok=True)
rng = random.Random(7)

OUTLINE = (18, 12, 26, 255)
GOLD_HI = (255, 236, 150, 255)
GOLD = (232, 178, 64, 255)
GOLD_LO = (150, 98, 30, 255)
NAVY_TOP = (34, 44, 86)
NAVY_BOT = (16, 20, 44)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def frame(img, x0, y0, w, h, inner_top, inner_bot, alpha=238, gold=True, noise=0, outline=OUTLINE,
          hi=GOLD_HI, mid=GOLD, lo=GOLD_LO):
    px = img.load()
    for y in range(h):
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            if edge == 0:
                c = outline
            elif gold and edge in (1, 2):
                if edge == 1:
                    c = hi if (x <= y or (y == 1 and x < w - 2)) and not (y == h - 2 or x == w - 2) else lo
                    if y == 1 or x == 1:
                        c = hi
                    if y == h - 2 or x == w - 2:
                        c = lo
                else:
                    c = mid
            elif gold and edge == 3:
                c = outline
            else:
                t = y / max(1, h - 1)
                base = lerp(inner_top, inner_bot, t)
                if noise:
                    n = rng.randint(-noise, noise)
                    base = tuple(max(0, min(255, v + n)) for v in base)
                c = base + (alpha,)
            px[x0 + x, y0 + y] = c


def corners(img, x0, y0, w, h, color=GOLD_HI, accent=(235, 90, 130, 255)):
    """Little diamond ornaments inside each corner."""
    px = img.load()
    for cx, cy in ((5, 5), (w - 6, 5), (5, h - 6), (w - 6, h - 6)):
        for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
            px[x0 + cx + dx, y0 + cy + dy] = color
        px[x0 + cx, y0 + cy] = accent


# --- dark panel (dialogue box, shop) 48x48, corner 10
img = Image.new("RGBA", (48, 48))
frame(img, 0, 0, 48, 48, NAVY_TOP, NAVY_BOT, alpha=235, noise=2)
corners(img, 0, 0, 48, 48)
img.save(os.path.join(OUT, "panel_dark.png"))

# --- parchment panel (adventurer card) 48x48, corner 10
img = Image.new("RGBA", (48, 48))
frame(img, 0, 0, 48, 48, (240, 226, 190), (222, 200, 152), alpha=250, noise=5,
      outline=(60, 34, 14, 255), hi=(214, 160, 92, 255), mid=(150, 96, 44, 255), lo=(96, 56, 22, 255))
px = img.load()
for i in range(40):
    x, y = rng.randint(5, 42), rng.randint(5, 42)
    r, g, b, a = px[x, y]
    px[x, y] = (r - 18, g - 20, b - 22, a)
corners(img, 0, 0, 48, 48, color=(170, 110, 50, 255), accent=(190, 40, 40, 255))
img.save(os.path.join(OUT, "panel_paper.png"))

# --- quest note (board cards) 48x48, corner 8: paper with a ragged edge and a red pin
img = Image.new("RGBA", (48, 48))
px = img.load()
for y in range(48):
    for x in range(48):
        edge = min(x, y, 47 - x, 47 - y)
        rag = rng.random() < 0.25
        if edge == 0 and rag:
            continue
        shade = 238 - int(18 * (y / 47)) + rng.randint(-4, 4)
        c = (shade, shade - 12, shade - 44, 255)
        if edge <= 1:
            c = (196, 170, 120, 255)
        px[x, y] = c
img.save(os.path.join(OUT, "note.png"))

# --- buttons 64x60: normal / hover / disabled, corner 5
def buttons(name, top, bot, htop, hbot, dtop, dbot, **kw):
    img = Image.new("RGBA", (64, 60))
    frame(img, 0, 0, 64, 20, top, bot, alpha=245, **kw)
    frame(img, 0, 20, 64, 20, htop, hbot, alpha=250, **kw)
    frame(img, 0, 40, 64, 20, dtop, dbot, alpha=220, gold=False)
    px = img.load()
    # a soft glow line under the top border of the hovered state
    for x in range(4, 60):
        r, g, b, a = px[x, 24]
        px[x, 24] = (min(255, r + 40), min(255, g + 40), min(255, b + 50), a)
    img.save(os.path.join(OUT, name))


buttons("button.png", (44, 56, 104), (24, 30, 64), (74, 92, 168), (40, 52, 110), (54, 54, 64), (38, 38, 46))
buttons("button_paper.png", (196, 140, 80), (150, 98, 50), (226, 172, 104), (178, 118, 60), (150, 138, 120), (120, 110, 96),
        outline=(54, 30, 12, 255), hi=(250, 220, 160, 255), mid=(212, 160, 90, 255), lo=(110, 66, 26, 255))

# --- name plate 32x16, corner 6
img = Image.new("RGBA", (32, 16))
frame(img, 0, 0, 32, 16, (44, 34, 70), (24, 18, 40), alpha=245)
img.save(os.path.join(OUT, "nameplate.png"))

# --- portrait frame 48x48, corner 8 (transparent window in the middle)
img = Image.new("RGBA", (48, 48))
frame(img, 0, 0, 48, 48, (58, 70, 120), (26, 30, 60), alpha=200)
px = img.load()
for y in range(48):
    for x in range(48):
        if min(x, y, 47 - x, 47 - y) >= 6:
            t = y / 47
            c = lerp((90, 110, 170), (30, 36, 70), t)
            px[x, y] = c + (150,)
corners(img, 0, 0, 48, 48)
img.save(os.path.join(OUT, "portrait.png"))

# --- icons 32x16: gem, gem lit, arrow, coin | pin
img = Image.new("RGBA", (32, 16))
px = img.load()
gem = ["...##...", "..#ab#..", ".#aabb#.", "#aabbbc#", ".#bbbc#.", "..#bc#..", "...##...", "........"]
pal_gem = {"#": OUTLINE, "a": (170, 220, 255, 255), "b": (80, 150, 230, 255), "c": (40, 80, 160, 255)}
pal_lit = {"#": OUTLINE, "a": (255, 210, 230, 255), "b": (240, 90, 140, 255), "c": (160, 30, 80, 255)}
arrow = ["........", "#######.", "#abbbc#.", ".#abc#..", ".#abc#..", "..#c#...", "..###...", "...#...."]
pal_arrow = {"#": OUTLINE, "a": GOLD_HI, "b": GOLD, "c": GOLD_LO}
coin = ["..####..", ".#aaab#.", "#aabbbc#", "#abccbc#", "#abccbc#", "#abbbcc#", ".#bccc#.", "..####.."]
pin = ["..###...", ".#abb#..", "#abbbc#.", "#bbbcc#.", ".#ccc#..", "..#d#...", "..#d#...", "...#...."]
pal_pin = {"#": OUTLINE, "a": (255, 160, 160, 255), "b": (220, 50, 50, 255), "c": (130, 20, 20, 255), "d": (180, 180, 190, 255)}
for ox, oy, art, pal in ((0, 0, gem, pal_gem), (8, 0, gem, pal_lit), (16, 0, arrow, pal_arrow), (24, 0, coin, pal_arrow), (0, 8, pin, pal_pin)):
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in pal:
                px[ox + x, oy + y] = pal[ch]
img.save(os.path.join(OUT, "icons.png"))
print("gui textures written to", OUT)
