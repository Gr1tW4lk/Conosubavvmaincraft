#!/usr/bin/env python3
"""Additive (black = transparent) effect textures for Explosion: magic circle, shockwave ring, glow orb."""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter, ImageChops

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src/main/resources/assets/konocraft/textures/fx")
os.makedirs(OUT, exist_ok=True)
rng = random.Random(42)


def glowify(img, radius, strength=1.0):
    blur = img.filter(ImageFilter.GaussianBlur(radius))
    if strength != 1.0:
        blur = blur.point(lambda v: min(255, int(v * strength)))
    return ImageChops.add(img, blur)


# --- magic circle 512x512, white lines (tinted crimson in game)
S = 512
c = S / 2
img = Image.new("L", (S, S), 0)
d = ImageDraw.Draw(img)


def ring(r, w):
    d.ellipse((c - r, c - r, c + r, c + r), outline=255, width=w)


ring(246, 5)
ring(232, 3)
ring(176, 4)
ring(164, 2)
ring(92, 3)
ring(40, 3)
# rune band between the two outer rings: little angular glyphs
glyphs = []
for i in range(48):
    a = i * 2 * math.pi / 48
    r1, r2 = 186, 222
    mid = (r1 + r2) / 2
    gx, gy = c + math.cos(a) * mid, c + math.sin(a) * mid
    t = (-math.sin(a), math.cos(a))
    n = (math.cos(a), math.sin(a))
    kind = rng.randint(0, 5)
    pts = []
    h = 14
    if kind == 0:
        pts = [(-1, -h), (-1, h), (6, 0)]
    elif kind == 1:
        pts = [(-6, -h), (6, h), (6, -h), (-6, h)]
    elif kind == 2:
        pts = [(0, -h), (0, h), (-7, 2), (7, 2)]
    elif kind == 3:
        pts = [(-6, -h), (-6, h), (6, h), (6, -4)]
    elif kind == 4:
        pts = [(-7, 0), (0, -h), (7, 0), (0, h), (-7, 0)]
    else:
        pts = [(-6, h), (0, -h), (6, h), (-4, 4), (4, 4)]
    world = [(gx + t[0] * px + n[0] * py, gy + t[1] * px + n[1] * py) for px, py in pts]
    d.line(world, fill=255, width=3)
# hexagram
for off in (0, math.pi / 3):
    tri = [(c + math.cos(off + k * 2 * math.pi / 3 - math.pi / 2) * 164, c + math.sin(off + k * 2 * math.pi / 3 - math.pi / 2) * 164) for k in range(3)]
    d.line(tri + [tri[0]], fill=255, width=4)
# spokes and inner star
for k in range(12):
    a = k * math.pi / 6
    d.line((c + math.cos(a) * 40, c + math.sin(a) * 40, c + math.cos(a) * 92, c + math.sin(a) * 92), fill=200, width=2)
for k in range(5):
    a1 = k * 4 * math.pi / 5 - math.pi / 2
    a2 = (k + 1) * 4 * math.pi / 5 - math.pi / 2
    d.line((c + math.cos(a1) * 88, c + math.sin(a1) * 88, c + math.cos(a2) * 88, c + math.sin(a2) * 88), fill=255, width=3)
# small orbs at the hexagram tips
for k in range(6):
    a = k * math.pi / 3 - math.pi / 2
    x, y = c + math.cos(a) * 164, c + math.sin(a) * 164
    d.ellipse((x - 11, y - 11, x + 11, y + 11), outline=255, width=3)
    d.ellipse((x - 4, y - 4, x + 4, y + 4), fill=255)
d.ellipse((c - 14, c - 14, c + 14, c + 14), fill=255)
img = glowify(img, 6, 1.3)
img = glowify(img, 18, 0.7)
Image.merge("RGBA", (img, img, img, img)).save(os.path.join(OUT, "magic_circle.png"))

# --- shockwave ring 256x256
S2 = 256
img = Image.new("L", (S2, S2), 0)
px = img.load()
for y in range(S2):
    for x in range(S2):
        r = math.hypot(x - S2 / 2 + 0.5, y - S2 / 2 + 0.5) / (S2 / 2)
        v = math.exp(-((r - 0.86) / 0.07) ** 2) + 0.35 * math.exp(-((r - 0.7) / 0.18) ** 2)
        px[x, y] = max(0, min(255, int(v * 255)))
Image.merge("RGBA", (img, img, img, img)).save(os.path.join(OUT, "shockwave.png"))

# --- glow orb 128x128
S3 = 128
img = Image.new("L", (S3, S3), 0)
px = img.load()
for y in range(S3):
    for x in range(S3):
        r = math.hypot(x - S3 / 2 + 0.5, y - S3 / 2 + 0.5) / (S3 / 2)
        v = max(0.0, 1 - r) ** 2.2 + 0.6 * math.exp(-(r / 0.18) ** 2)
        px[x, y] = max(0, min(255, int(v * 255)))
Image.merge("RGBA", (img, img, img, img)).save(os.path.join(OUT, "glow.png"))

# --- smoke puffs 256x256: a 2x2 atlas of soft billowing blobs, white with alpha (tinted in game)
T = 128
atlas = Image.new("RGBA", (2 * T, 2 * T), (0, 0, 0, 0))
for tile in range(4):
    dens = Image.new("L", (T, T), 0)
    dd = ImageDraw.Draw(dens)
    for _ in range(14):
        a = rng.random() * 2 * math.pi
        d = rng.random() ** 0.7 * 30
        r = rng.uniform(14, 30)
        x, y = T / 2 + math.cos(a) * d, T / 2 + math.sin(a) * d
        dd.ellipse((x - r, y - r, x + r, y + r), fill=255)
    dens = dens.filter(ImageFilter.GaussianBlur(9))
    shade = Image.new("L", (T, T), 0)
    sp = shade.load()
    dp = dens.load()
    for y in range(T):
        for x in range(T):
            n = rng.randint(-14, 14)
            edge = max(0.0, 1 - math.hypot(x - T / 2, y - T / 2) / (T / 2))
            dp[x, y] = max(0, min(255, int(dp[x, y] * min(1.0, edge * 2.2)) + (n if dp[x, y] > 20 else 0)))
            sp[x, y] = max(0, min(255, 225 + n - int(40 * (y / T))))
    tile_img = Image.merge("RGBA", (shade, shade, shade, dens))
    atlas.paste(tile_img, ((tile % 2) * T, (tile // 2) * T))
atlas.save(os.path.join(OUT, "smoke.png"))
print("fx textures written to", OUT)
