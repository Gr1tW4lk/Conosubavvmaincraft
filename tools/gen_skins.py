#!/usr/bin/env python3
"""Paints the 64x64 player-format skins of every humanoid character.

Pixel art is described with a few primitives (fill a body part, horizontal bands that wrap
around a limb, front-face details) plus subtle shading so the result reads well in game.
Run from the repo root: python3 tools/gen_skins.py
"""
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/konocraft/textures/entity/character")

FACE_SHADE = {"top": 1.10, "bottom": 0.72, "front": 1.0, "back": 0.84, "right": 0.92, "left": 0.92}


def clamp(v):
    return max(0, min(255, int(v)))


def sh(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), 255)


class Skin:
    PARTS = {
        # name: (u, v, w, h, d) for the base layer; overlay offsets handled separately
        "head": (0, 0, 8, 8, 8),
        "body": (16, 16, 8, 12, 4),
        "rarm": (40, 16, 4, 12, 4),
        "larm": (32, 48, 4, 12, 4),
        "rleg": (0, 16, 4, 12, 4),
        "lleg": (16, 48, 4, 12, 4),
    }
    OVERLAY = {"head": (32, 0), "body": (16, 32), "rarm": (40, 32), "larm": (48, 48), "rleg": (0, 32), "lleg": (0, 48)}

    def __init__(self, slim=False, seed=1):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.slim = slim
        self.rng = random.Random(seed)

    def dims(self, part):
        u, v, w, h, d = self.PARTS[part]
        if self.slim and part in ("rarm", "larm"):
            w = 3
        return u, v, w, h, d

    def faces(self, part, overlay=False):
        u, v, w, h, d = self.dims(part)
        if overlay:
            u, v = self.OVERLAY[part]
        return {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h),
            "back": (u + 2 * d + w, v + d, w, h),
        }

    def px(self, x, y, c, shade=1.0, noise=0.05):
        n = 1 + (self.rng.random() - 0.5) * 2 * noise
        if len(c) > 3 and c[3] == 0:
            self.img.putpixel((x, y), (0, 0, 0, 0))
            return
        self.img.putpixel((x, y), sh(c, shade * n))

    def fill(self, part, color, faces=None, overlay=False, noise=0.05):
        for face, (x, y, w, h) in self.faces(part, overlay).items():
            if faces and face not in faces:
                continue
            for i in range(w):
                for j in range(h):
                    self.px(x + i, y + j, color, FACE_SHADE[face], noise)

    def band(self, part, y0, y1, color, overlay=False, noise=0.05, faces=("right", "front", "left", "back")):
        """Rows y0..y1 (limb-local, 0 = top of the side faces) around the limb."""
        for face, (x, y, w, h) in self.faces(part, overlay).items():
            if face not in faces:
                continue
            for j in range(max(0, y0), min(h, y1 + 1)):
                for i in range(w):
                    self.px(x + i, y + j, color, FACE_SHADE[face], noise)
        if y1 >= self.dims(part)[3] - 1 and "bottom" not in faces:
            x, y, w, h = self.faces(part, overlay)["bottom"]
            for i in range(w):
                for j in range(h):
                    self.px(x + i, y + j, color, FACE_SHADE["bottom"], noise)

    def rect(self, part, face, x0, y0, x1, y1, color, overlay=False, noise=0.04):
        x, y, w, h = self.faces(part, overlay)[face]
        for i in range(max(0, x0), min(w, x1 + 1)):
            for j in range(max(0, y0), min(h, y1 + 1)):
                self.px(x + i, y + j, color, FACE_SHADE[face], noise)

    def dot(self, part, face, i, j, color, overlay=False):
        x, y, w, h = self.faces(part, overlay)[face]
        if 0 <= i < w and 0 <= j < h:
            self.img.putpixel((x + i, y + j), (color[0], color[1], color[2], 255))

    def clear(self, part, face, i, j, overlay=True):
        x, y, w, h = self.faces(part, overlay)[face]
        if 0 <= i < w and 0 <= j < h:
            self.img.putpixel((x + i, y + j), (0, 0, 0, 0))

    def save(self, name):
        os.makedirs(OUT, exist_ok=True)
        self.img.save(os.path.join(OUT, name + ".png"))


# ------------------------------------------------------------------------------------------------
# Building blocks

def face(s, skin, hair, eye, lash=(30, 20, 25), mouth=None, blush=None, hair_dark=None, bangs="even", eyes_style="anime"):
    hd = hair_dark or sh(hair, 0.75)
    s.fill("head", skin)
    # hair: top, back, upper sides
    s.rect("head", "top", 0, 0, 7, 7, hair)
    s.rect("head", "back", 0, 0, 7, 7, hair)
    s.rect("head", "right", 0, 0, 7, 3, hair)
    s.rect("head", "left", 0, 0, 7, 3, hair)
    s.rect("head", "right", 4, 4, 7, 6, hair)
    s.rect("head", "left", 0, 4, 3, 6, hair)
    # bangs
    s.rect("head", "front", 0, 0, 7, 1, hair)
    pattern = {"even": [1, 0, 1, 1, 0, 1, 1, 0], "side": [1, 1, 1, 0, 0, 1, 0, 1], "messy": [1, 0, 1, 0, 1, 1, 0, 1], "parted": [1, 1, 0, 0, 0, 0, 1, 1]}[bangs]
    for i, on in enumerate(pattern):
        if on:
            s.dot("head", "front", i, 2, hd if i in (0, 7) else hair)
    s.dot("head", "front", 0, 3, hd)
    s.dot("head", "front", 7, 3, hd)
    # eyes: dark lash row, then iris (outer pixel lighter, inner darker)
    for ex in (1, 5):
        s.dot("head", "front", ex, 4, lash)
        s.dot("head", "front", ex + 1, 4, lash)
    if eyes_style == "anime":
        s.dot("head", "front", 1, 5, eye)
        s.dot("head", "front", 2, 5, sh(eye, 0.62))
        s.dot("head", "front", 5, 5, sh(eye, 0.62))
        s.dot("head", "front", 6, 5, eye)
        s.dot("head", "front", 1, 6, sh(eye, 0.85) if blush is None else skin)
        s.dot("head", "front", 6, 6, sh(eye, 0.85) if blush is None else skin)
    else:
        s.dot("head", "front", 1, 5, (245, 245, 245))
        s.dot("head", "front", 2, 5, eye)
        s.dot("head", "front", 5, 5, eye)
        s.dot("head", "front", 6, 5, (245, 245, 245))
    if blush:
        s.dot("head", "front", 1, 6, blush)
        s.dot("head", "front", 6, 6, blush)
    m = mouth or sh(skin, 0.7)
    s.dot("head", "front", 3, 7, m)
    s.dot("head", "front", 4, 7, m)


def long_hair(s, hair, length=10, hair_dark=None):
    hd = hair_dark or sh(hair, 0.8)
    # hair hangs down the back of the body
    s.rect("body", "back", 0, 0, 7, length - 1, hair, overlay=True)
    for i in range(8):
        if s.rng.random() < 0.35:
            s.dot("body", "back", i, length - 1, hd, overlay=True)
    # side locks on the hat layer
    s.rect("head", "right", 0, 0, 7, 7, hair, overlay=True)
    s.rect("head", "left", 0, 0, 7, 7, hair, overlay=True)
    for j in range(2, 8):
        s.clear("head", "right", 0, j)
        s.clear("head", "left", 7, j)
    s.rect("head", "back", 0, 0, 7, 7, hair, overlay=True)
    s.rect("head", "top", 0, 0, 7, 7, sh(hair, 1.05), overlay=True)


def hands(s, skin, rows=2):
    for arm in ("rarm", "larm"):
        s.band(arm, 12 - rows, 11, skin)


def boots(s, color, height=2):
    for leg in ("rleg", "lleg"):
        s.band(leg, 12 - height, 11, color)


def outline_belt(s, row, color):
    s.band("body", row, row, color, faces=("right", "front", "left", "back"))


# ------------------------------------------------------------------------------------------------

def megumin():
    s = Skin(slim=True, seed=11)
    skin, hair, eye = (252, 221, 196), (44, 34, 42), (212, 26, 42)
    face(s, skin, hair, eye, blush=(245, 180, 175), bangs="even")
    # eyepatch over her left eye (viewer's right): black patch + strap
    s.dot("head", "front", 5, 4, (20, 18, 22))
    s.dot("head", "front", 6, 4, (20, 18, 22))
    s.dot("head", "front", 5, 5, (20, 18, 22))
    s.dot("head", "front", 6, 5, (20, 18, 22))
    s.dot("head", "front", 7, 3, (20, 18, 22))
    s.rect("head", "left", 0, 3, 7, 3, (20, 18, 22))
    s.rect("head", "back", 0, 3, 7, 3, (20, 18, 22))
    s.rect("head", "right", 0, 3, 7, 3, (20, 18, 22))
    # short hair on the hat layer: little side tufts
    for side in ("right", "left"):
        s.rect("head", side, 0, 0, 7, 2, hair, overlay=True)
    s.rect("head", "back", 0, 0, 7, 4, hair, overlay=True)
    red, dark_red, black, gold, brown = (190, 32, 44), (130, 20, 30), (28, 24, 30), (225, 180, 70), (110, 70, 40)
    # dress
    s.fill("body", red)
    s.rect("body", "front", 0, 0, 7, 1, black)
    s.dot("body", "front", 3, 0, (230, 230, 230))
    s.dot("body", "front", 4, 0, (230, 230, 230))
    s.rect("body", "front", 3, 2, 4, 5, dark_red)
    s.dot("body", "front", 3, 3, gold)
    s.dot("body", "front", 4, 4, gold)
    outline_belt(s, 6, brown)
    s.dot("body", "front", 3, 6, gold)
    s.band("body", 7, 11, (60, 40, 50))
    s.band("body", 11, 11, red)
    # black cape on the back and shoulders (jacket layer)
    s.rect("body", "back", 0, 0, 7, 11, black, overlay=True)
    s.rect("body", "top", 0, 0, 7, 3, black, overlay=True)
    for side in ("right", "left"):
        s.rect("body", side, 0, 0, 3, 3, black, overlay=True)
    s.rect("body", "back", 1, 2, 6, 2, red, overlay=True)
    # sleeves
    for arm in ("rarm", "larm"):
        s.fill(arm, red)
        s.band(arm, 0, 1, black)
        s.band(arm, 8, 9, (30, 26, 32))
    hands(s, skin, 2)
    # legs: black stocking + bandaged leg
    s.fill("lleg", (30, 28, 36))
    s.band("lleg", 0, 0, skin)
    s.fill("rleg", skin)
    for j in (1, 3, 5, 7):
        s.band("rleg", j, j, (235, 230, 220))
    boots(s, brown, 2)
    s.save("megumin")


def aqua():
    s = Skin(slim=True, seed=12)
    skin, hair, eye = (255, 233, 216), (120, 200, 250), (60, 135, 235)
    face(s, skin, hair, eye, blush=(250, 190, 190), bangs="parted", hair_dark=(80, 160, 225))
    long_hair(s, hair, 11, (80, 160, 225))
    # hair ring ornament on top
    for i, j in ((2, 1), (3, 0), (4, 0), (5, 1), (2, 2), (5, 2)):
        s.dot("head", "top", i, j, (90, 170, 240), overlay=True)
    s.dot("head", "front", 6, 1, (90, 220, 255), overlay=True)
    blue, white, light, pink = (40, 95, 205), (240, 244, 252), (120, 185, 245), (240, 170, 210)
    s.fill("body", blue)
    s.rect("body", "front", 2, 0, 5, 3, white)
    s.dot("body", "front", 3, 2, (90, 220, 255))
    s.dot("body", "front", 4, 2, (90, 220, 255))
    outline_belt(s, 6, white)
    s.band("body", 7, 11, light)
    s.band("body", 11, 11, white)
    # pink shawl (hagoromo) across the back
    s.rect("body", "back", 0, 3, 7, 4, pink, overlay=True)
    for arm in ("rarm", "larm"):
        s.fill(arm, white)
        s.band(arm, 0, 1, skin)
        s.band(arm, 9, 9, blue)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, skin)
        s.band(leg, 4, 11, white)
        s.band(leg, 4, 4, blue)
    boots(s, (200, 220, 245), 1)
    s.save("aqua")


def darkness():
    s = Skin(slim=True, seed=13)
    skin, hair, eye = (250, 227, 207), (242, 206, 96), (70, 110, 205)
    face(s, skin, hair, eye, blush=(245, 170, 170), bangs="side", hair_dark=(205, 160, 60))
    # ponytail
    s.rect("head", "back", 2, 2, 5, 7, sh(hair, 0.9), overlay=True)
    s.rect("body", "back", 3, 0, 4, 6, hair, overlay=True)
    s.dot("head", "back", 3, 2, (50, 80, 170), overlay=True)
    s.dot("head", "back", 4, 2, (50, 80, 170), overlay=True)
    silver, steel, blue, under = (205, 212, 224), (150, 160, 178), (50, 80, 175), (40, 40, 55)
    s.fill("body", silver)
    s.rect("body", "front", 0, 0, 7, 0, steel)
    s.rect("body", "front", 3, 1, 4, 5, blue)
    s.rect("body", "front", 1, 6, 6, 6, steel)
    s.band("body", 7, 11, blue)
    s.band("body", 11, 11, steel)
    for side in ("right", "left"):
        s.rect("body", side, 0, 0, 3, 6, under)
    for arm in ("rarm", "larm"):
        s.fill(arm, under)
        s.band(arm, 0, 3, silver)
        s.band(arm, 3, 3, blue)
        s.band(arm, 8, 11, steel)
        s.band(arm, 0, 3, silver, overlay=True)
    for leg in ("rleg", "lleg"):
        s.fill(leg, under)
        s.band(leg, 5, 11, silver)
        s.band(leg, 5, 5, blue)
    boots(s, steel, 2)
    s.save("darkness")


def kazuma():
    s = Skin(slim=False, seed=14)
    skin, hair, eye = (246, 214, 186), (96, 62, 36), (95, 64, 42)
    face(s, skin, hair, eye, bangs="messy", eyes_style="plain")
    s.dot("head", "front", 2, 7, sh(skin, 0.7))
    s.dot("head", "front", 5, 7, sh(skin, 0.7))
    shirt, leather, cloak, pants = (228, 220, 196), (120, 80, 45), (62, 124, 62), (46, 44, 52)
    s.fill("body", shirt)
    s.rect("body", "front", 1, 2, 6, 7, leather)
    outline_belt(s, 8, (80, 55, 30))
    s.band("body", 9, 11, pants)
    s.rect("body", "back", 0, 0, 7, 11, cloak, overlay=True)
    s.rect("body", "top", 0, 0, 7, 3, cloak, overlay=True)
    s.rect("body", "front", 0, 0, 1, 2, cloak, overlay=True)
    s.rect("body", "front", 6, 0, 7, 2, cloak, overlay=True)
    for arm in ("rarm", "larm"):
        s.fill(arm, (70, 130, 70))
        s.band(arm, 0, 2, cloak, overlay=True)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, pants)
    boots(s, (90, 60, 35), 3)
    s.save("kazuma")


def eris():
    s = Skin(slim=True, seed=15)
    skin, hair, eye = (255, 237, 226), (236, 236, 248), (110, 110, 225)
    face(s, skin, hair, eye, blush=(250, 200, 205), bangs="parted", hair_dark=(195, 195, 220))
    long_hair(s, hair, 12, (200, 200, 225))
    s.dot("head", "front", 1, 1, (230, 190, 80), overlay=True)
    s.dot("head", "front", 6, 1, (230, 190, 80), overlay=True)
    white, shawl, gold = (246, 246, 252), (122, 104, 205), (232, 192, 82)
    s.fill("body", white)
    s.rect("body", "front", 0, 0, 7, 2, shawl)
    s.dot("body", "front", 3, 3, gold)
    s.dot("body", "front", 4, 3, gold)
    outline_belt(s, 6, gold)
    s.rect("body", "back", 0, 0, 7, 3, shawl, overlay=True)
    for arm in ("rarm", "larm"):
        s.fill(arm, white)
        s.band(arm, 0, 2, shawl)
        s.band(arm, 9, 9, gold)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, white)
        s.band(leg, 0, 9, white, overlay=True)
    boots(s, (225, 215, 240), 1)
    s.save("eris")


def luna():
    s = Skin(slim=True, seed=16)
    skin, hair, eye = (250, 225, 202), (206, 150, 82), (70, 150, 92)
    face(s, skin, hair, eye, blush=(245, 185, 175), bangs="side", hair_dark=(170, 115, 60))
    long_hair(s, hair, 7, (170, 115, 60))
    blouse, corset, skirt = (245, 240, 230), (112, 70, 40), (52, 92, 62)
    s.fill("body", blouse)
    s.rect("body", "front", 1, 4, 6, 8, corset)
    s.dot("body", "front", 3, 5, (220, 200, 160))
    s.dot("body", "front", 4, 7, (220, 200, 160))
    s.rect("body", "front", 3, 1, 4, 1, (60, 120, 80))
    s.band("body", 9, 11, skirt)
    for arm in ("rarm", "larm"):
        s.fill(arm, blouse)
        s.band(arm, 9, 9, corset)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, skirt)
        s.band(leg, 0, 6, skirt, overlay=True)
    boots(s, (80, 50, 30), 2)
    s.save("luna")


def wiz():
    s = Skin(slim=True, seed=17)
    skin, hair, eye = (238, 230, 240), (128, 86, 62), (130, 90, 150)
    face(s, skin, hair, eye, blush=(230, 200, 220), bangs="parted", hair_dark=(100, 64, 44))
    long_hair(s, hair, 12, (100, 64, 44))
    robe, light, black = (78, 46, 104), (126, 84, 166), (30, 24, 36)
    s.fill("body", robe)
    s.rect("body", "front", 2, 0, 5, 2, light)
    s.rect("body", "front", 3, 3, 4, 11, black)
    outline_belt(s, 5, (180, 150, 200))
    for arm in ("rarm", "larm"):
        s.fill(arm, robe)
        s.band(arm, 8, 9, light)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, robe)
        s.band(leg, 0, 10, robe, overlay=True)
    boots(s, black, 1)
    s.save("wiz")


def vanir():
    s = Skin(slim=False, seed=18)
    skin, hair = (238, 218, 198), (24, 24, 30)
    face(s, skin, hair, (220, 30, 30), bangs="even", eyes_style="plain")
    # the mask: black upper face with white trim and red eyes
    s.rect("head", "front", 0, 2, 7, 5, (18, 18, 22))
    s.rect("head", "front", 0, 2, 7, 2, (235, 235, 240))
    s.dot("head", "front", 1, 4, (230, 30, 30))
    s.dot("head", "front", 2, 4, (255, 90, 60))
    s.dot("head", "front", 5, 4, (255, 90, 60))
    s.dot("head", "front", 6, 4, (230, 30, 30))
    s.dot("head", "front", 3, 7, (120, 70, 60))
    s.dot("head", "front", 4, 7, (120, 70, 60))
    s.rect("head", "front", 0, 2, 7, 5, (18, 18, 22), overlay=True)
    s.dot("head", "front", 1, 4, (255, 40, 40), overlay=True)
    s.dot("head", "front", 6, 4, (255, 40, 40), overlay=True)
    s.rect("head", "front", 0, 2, 7, 2, (240, 240, 245), overlay=True)
    suit, shirt, tie, apron = (26, 26, 32), (240, 240, 240), (190, 30, 40), (250, 170, 205)
    s.fill("body", suit)
    s.rect("body", "front", 3, 0, 4, 4, shirt)
    s.rect("body", "front", 3, 1, 4, 3, tie)
    s.rect("body", "front", 1, 5, 6, 11, apron)
    for i in range(1, 7, 2):
        s.dot("body", "front", i, 11, (255, 220, 235))
    for arm in ("rarm", "larm"):
        s.fill(arm, suit)
        s.band(arm, 10, 10, shirt)
    hands(s, (230, 230, 230), 1)
    for leg in ("rleg", "lleg"):
        s.fill(leg, suit)
    s.rect("rleg", "front", 0, 0, 3, 2, apron)
    s.rect("lleg", "front", 0, 0, 3, 2, apron)
    boots(s, (12, 12, 16), 1)
    s.save("vanir")


def yunyun():
    s = Skin(slim=True, seed=19)
    skin, hair, eye = (252, 226, 206), (36, 30, 38), (214, 30, 54)
    face(s, skin, hair, eye, blush=(250, 160, 170), bangs="side")
    ribbon = (240, 112, 162)
    s.rect("head", "top", 1, 1, 6, 2, ribbon, overlay=True)
    s.dot("head", "back", 3, 3, ribbon, overlay=True)
    s.dot("head", "back", 4, 3, ribbon, overlay=True)
    # braids
    s.rect("head", "right", 5, 4, 7, 7, hair, overlay=True)
    s.rect("head", "left", 0, 4, 2, 7, hair, overlay=True)
    s.rect("body", "back", 0, 0, 7, 5, hair, overlay=True)
    black, pink = (30, 26, 34), (240, 145, 185)
    s.fill("body", black)
    s.rect("body", "front", 3, 0, 4, 3, ribbon)
    outline_belt(s, 6, pink)
    s.band("body", 7, 11, pink)
    for arm in ("rarm", "larm"):
        s.fill(arm, black)
        s.band(arm, 0, 1, pink)
    hands(s, skin, 2)
    for leg in ("rleg", "lleg"):
        s.fill(leg, (34, 30, 40))
        s.band(leg, 0, 1, skin)
    boots(s, pink, 1)
    s.save("yunyun")


def beldia():
    s = Skin(slim=False, seed=20)
    plate, dark, purple, red = (44, 42, 56), (26, 24, 34), (112, 62, 152), (200, 30, 40)
    s.fill("head", plate)
    s.rect("head", "front", 1, 3, 6, 4, dark)
    s.dot("head", "front", 2, 4, red)
    s.dot("head", "front", 5, 4, red)
    s.rect("head", "front", 3, 0, 4, 7, purple)
    s.rect("head", "top", 3, 0, 4, 7, purple)
    s.rect("head", "front", 0, 5, 7, 7, plate)
    for i in range(1, 7, 2):
        s.dot("head", "front", i, 6, dark)
    s.fill("body", plate)
    s.rect("body", "front", 3, 0, 4, 11, purple)
    s.rect("body", "front", 1, 3, 6, 3, purple)
    s.band("body", 7, 7, dark)
    # neck stump (the head is carried separately)
    s.rect("body", "top", 2, 1, 5, 2, (90, 20, 40))
    s.rect("body", "back", 0, 0, 7, 11, (110, 20, 30), overlay=True)
    for arm in ("rarm", "larm"):
        s.fill(arm, plate)
        s.band(arm, 0, 2, purple)
        s.band(arm, 9, 11, dark)
    for leg in ("rleg", "lleg"):
        s.fill(leg, plate)
        s.band(leg, 4, 4, purple)
    boots(s, dark, 3)
    s.save("beldia")


def goblin():
    s = Skin(slim=False, seed=21)
    skin, dark = (96, 146, 62), (64, 104, 42)
    s.fill("head", skin)
    s.rect("head", "top", 0, 0, 7, 7, dark)
    s.dot("head", "front", 1, 4, (230, 60, 40))
    s.dot("head", "front", 2, 4, (120, 20, 20))
    s.dot("head", "front", 5, 4, (120, 20, 20))
    s.dot("head", "front", 6, 4, (230, 60, 40))
    s.rect("head", "front", 2, 6, 5, 6, (40, 30, 20))
    s.dot("head", "front", 2, 7, (240, 240, 220))
    s.dot("head", "front", 5, 7, (240, 240, 220))
    s.rect("head", "front", 3, 5, 4, 5, dark)
    # pointy ears on the hat layer
    for side in ("right", "left"):
        s.rect("head", side, 3, 3, 4, 4, skin, overlay=True)
    s.fill("body", skin)
    s.rect("body", "front", 2, 2, 5, 4, dark)
    s.band("body", 8, 11, (112, 80, 40))
    s.band("body", 8, 8, (70, 50, 25))
    for arm in ("rarm", "larm"):
        s.fill(arm, skin)
    for leg in ("rleg", "lleg"):
        s.fill(leg, skin)
        s.band(leg, 0, 2, (112, 80, 40))
    boots(s, dark, 1)
    s.save("goblin")


def undead_knight():
    s = Skin(slim=False, seed=22)
    iron, rust, bone, tabard = (112, 104, 100), (142, 84, 52), (222, 214, 196), (82, 42, 112)
    s.fill("head", iron)
    s.rect("head", "front", 1, 2, 6, 6, bone)
    s.rect("head", "front", 1, 3, 2, 4, (20, 10, 30))
    s.rect("head", "front", 5, 3, 6, 4, (20, 10, 30))
    s.dot("head", "front", 2, 4, (170, 90, 255))
    s.dot("head", "front", 5, 4, (170, 90, 255))
    s.rect("head", "front", 2, 6, 5, 6, (30, 20, 20))
    for i in (2, 4):
        s.dot("head", "front", i, 6, bone)
    s.rect("head", "front", 0, 0, 7, 1, iron)
    for _ in range(14):
        s.dot("head", s.rng.choice(["top", "right", "left", "back"]), s.rng.randrange(8), s.rng.randrange(8), rust)
    s.fill("body", iron)
    s.rect("body", "front", 2, 1, 5, 11, tabard)
    s.dot("body", "front", 3, 11, (0, 0, 0))
    s.dot("body", "front", 5, 9, (0, 0, 0))
    for arm in ("rarm", "larm"):
        s.fill(arm, iron)
        s.band(arm, 10, 11, bone)
    for leg in ("rleg", "lleg"):
        s.fill(leg, iron)
    for part in ("body", "rarm", "larm", "rleg", "lleg"):
        for _ in range(10):
            f = s.rng.choice(["front", "right", "left", "back"])
            x, y, w, h = s.faces(part)[f]
            s.img.putpixel((x + s.rng.randrange(w), y + s.rng.randrange(h)), sh(rust, 0.9 + s.rng.random() * 0.3))
    boots(s, (70, 64, 60), 2)
    s.save("undead_knight")


TOWNS = [
    # hair, shirt, pants, extra
    ((96, 60, 34), (170, 60, 50), (70, 60, 50), "apron"),
    ((230, 200, 110), (60, 110, 170), (60, 55, 50), "vest"),
    ((40, 32, 30), (90, 140, 70), (90, 70, 50), "hat"),
    ((170, 60, 40), (200, 180, 140), (60, 70, 100), "apron"),
    ((140, 140, 145), (110, 80, 130), (50, 50, 60), "beard"),
    ((120, 80, 50), (220, 210, 190), (120, 90, 60), "scarf"),
]


def townsfolk():
    for i, (hair, shirt, pants, extra) in enumerate(TOWNS):
        s = Skin(slim=False, seed=40 + i)
        skin = [(245, 214, 186), (238, 200, 170), (226, 186, 150), (250, 222, 198), (232, 196, 166), (244, 210, 180)][i]
        face(s, skin, hair, (80, 60, 40) if i % 2 else (60, 90, 140), bangs=["even", "side", "messy", "parted", "even", "side"][i], eyes_style="plain")
        s.fill("body", shirt)
        outline_belt(s, 7, (80, 55, 30))
        s.band("body", 8, 11, pants)
        for arm in ("rarm", "larm"):
            s.fill(arm, shirt)
        hands(s, skin, 2)
        for leg in ("rleg", "lleg"):
            s.fill(leg, pants)
        boots(s, (70, 48, 28), 2)
        if extra == "apron":
            s.rect("body", "front", 1, 3, 6, 11, (235, 232, 222))
            s.rect("rleg", "front", 0, 0, 3, 3, (235, 232, 222))
            s.rect("lleg", "front", 0, 0, 3, 3, (235, 232, 222))
        elif extra == "vest":
            s.rect("body", "front", 0, 0, 2, 7, (90, 60, 40))
            s.rect("body", "front", 5, 0, 7, 7, (90, 60, 40))
        elif extra == "hat":
            s.rect("head", "top", 0, 0, 7, 7, (150, 120, 70), overlay=True)
            for side in ("front", "back", "right", "left"):
                s.rect("head", side, 0, 0, 7, 1, (150, 120, 70), overlay=True)
            s.rect("head", "front", 0, 1, 7, 1, (110, 60, 40), overlay=True)
        elif extra == "beard":
            s.rect("head", "front", 1, 6, 6, 7, hair)
            s.dot("head", "front", 3, 6, (120, 70, 60))
            s.dot("head", "front", 4, 6, (120, 70, 60))
        elif extra == "scarf":
            s.band("body", 0, 1, (190, 50, 60))
            s.rect("body", "front", 5, 2, 6, 5, (190, 50, 60))
        s.save(f"townsfolk_{i}")


if __name__ == "__main__":
    for fn in (megumin, aqua, darkness, kazuma, eris, luna, wiz, vanir, yunyun, beldia, goblin, undead_knight, townsfolk):
        fn()
    print("skins written to", OUT)
