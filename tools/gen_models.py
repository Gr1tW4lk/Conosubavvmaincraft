#!/usr/bin/env python3
"""Generates entity models (Java TexturedModelData) and their painted textures from one spec.

Each model is a tree of parts; each part has cubes. UVs are packed automatically and every cube
face is painted by a painter function, so geometry and texture always match.
Run from the repo root: python3 tools/gen_models.py
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/konocraft/textures/entity")
JAVA_OUT = os.path.join(ROOT, "src/client/java/dev/konocraft/client/model/GeneratedModels.java")


def clamp(v):
    return max(0, min(255, int(v)))


def shade(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f), c[3] if len(c) > 3 else 255)


def mix(a, b, t):
    return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


# ------------------------------------------------------------------------------------------------
# Painters: functions (img, x, y, w, h, face, rng) that paint a rectangle of the texture.

def noise_fill(base, amount=0.08, spots=None):
    def paint(img, x, y, w, h, face, rng):
        f = {"top": 1.12, "bottom": 0.7, "front": 1.0, "back": 0.86, "left": 0.93, "right": 0.93}[face]
        for i in range(w):
            for j in range(h):
                n = 1 + (rng.random() - 0.5) * 2 * amount
                c = shade(base, f * n)
                if spots and rng.random() < spots[1]:
                    c = shade(spots[0], f * n)
                img.putpixel((x + i, y + j), c)
    return paint


def faces(default, **overrides):
    """Different painter per face."""
    def paint(img, x, y, w, h, face, rng):
        p = overrides.get(face, default)
        p(img, x, y, w, h, face, rng)
    return paint


def gradient(top, bottom, amount=0.05):
    def paint(img, x, y, w, h, face, rng):
        f = {"top": 1.1, "bottom": 0.75, "front": 1.0, "back": 0.88, "left": 0.94, "right": 0.94}[face]
        for j in range(h):
            t = j / max(1, h - 1)
            base = mix(top, bottom, t)
            for i in range(w):
                n = 1 + (rng.random() - 0.5) * 2 * amount
                img.putpixel((x + i, y + j), shade(base, f * n))
    return paint


def plates(base, line, size=4, amount=0.06, rivets=None):
    """Metal plates with seams (and optional rivets)."""
    def paint(img, x, y, w, h, face, rng):
        f = {"top": 1.1, "bottom": 0.7, "front": 1.0, "back": 0.85, "left": 0.92, "right": 0.92}[face]
        for i in range(w):
            for j in range(h):
                n = 1 + (rng.random() - 0.5) * 2 * amount
                c = shade(base, f * n)
                if i % size == 0 or j % size == 0:
                    c = shade(line, f)
                elif rivets and i % size == 1 and j % size == 1:
                    c = shade(rivets, f)
                img.putpixel((x + i, y + j), c)
    return paint


def glow(color, core=None):
    def paint(img, x, y, w, h, face, rng):
        for i in range(w):
            for j in range(h):
                d = math.hypot((i + 0.5) / w - 0.5, (j + 0.5) / h - 0.5) * 2
                c = mix(core or (255, 255, 255), color, min(1, d * 1.3))
                img.putpixel((x + i, y + j), c)
    return paint


def eye_face(skin, eye, pupil=(15, 15, 15)):
    """A big round eye on the front face; skin elsewhere."""
    base = noise_fill(skin, 0.05)

    def paint(img, x, y, w, h, face, rng):
        base(img, x, y, w, h, face, rng)
        if face in ("front", "left", "right", "top"):
            cx, cy = w / 2, h / 2
            for i in range(w):
                for j in range(h):
                    d = math.hypot(i + 0.5 - cx, j + 0.5 - cy) / (min(w, h) / 2)
                    if d < 0.95:
                        img.putpixel((x + i, y + j), eye if d > 0.45 else pupil)
                    if 0.15 < d < 0.3 and i < cx and j < cy:
                        img.putpixel((x + i, y + j), (255, 255, 255, 255))
    return paint


# ------------------------------------------------------------------------------------------------

class Cube:
    def __init__(self, origin, size, paint, mirror=False, dilation=0.0):
        self.origin = origin
        self.size = size
        self.paint = paint
        self.mirror = mirror
        self.dilation = dilation
        self.uv = None

    def uv_size(self):
        w, h, d = [int(math.ceil(s)) for s in self.size]
        return 2 * (d + w), d + h


class Part:
    def __init__(self, name, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=(), children=()):
        self.name = name
        self.pivot = pivot
        self.rot = rot
        self.cubes = list(cubes)
        self.children = list(children)

    def all_cubes(self):
        for c in self.cubes:
            yield c
        for ch in self.children:
            yield from ch.all_cubes()


class Model:
    def __init__(self, name, tex, parts, method=None, texture=None):
        self.name = name
        self.tex = tex
        self.parts = parts
        self.method = method or name
        self.texture = texture or name

    def cubes(self):
        for p in self.parts:
            yield from p.all_cubes()


def pack(model):
    W, H = model.tex
    cubes = sorted(model.cubes(), key=lambda c: -c.uv_size()[1])
    x = y = row = 0
    for c in cubes:
        w, h = c.uv_size()
        if x + w > W:
            x = 0
            y += row
            row = 0
        if y + h > H:
            raise SystemExit(f"Texture {model.name} {W}x{H} too small")
        c.uv = (x, y)
        x += w
        row = max(row, h)


def paint_model(model, seed):
    rng = random.Random(seed)
    img = Image.new("RGBA", model.tex, (0, 0, 0, 0))
    for c in model.cubes():
        u, v = c.uv
        w, h, d = [int(math.ceil(s)) for s in c.size]
        rects = {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h),
            "back": (u + 2 * d + w, v + d, w, h),
        }
        for face, (x, y, fw, fh) in rects.items():
            if fw > 0 and fh > 0:
                c.paint(img, x, y, fw, fh, face, rng)
    return img


def f(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "f"


def java_part(p, parent, lines, depth):
    builder = "ModelPartBuilder.create()"
    for c in p.cubes:
        ox, oy, oz = c.origin
        sx, sy, sz = c.size
        builder += f".uv({c.uv[0]}, {c.uv[1]})"
        if c.mirror:
            builder += ".mirrored()"
        if c.dilation:
            builder += f".cuboid({f(ox)}, {f(oy)}, {f(oz)}, {f(sx)}, {f(sy)}, {f(sz)}, new Dilation({f(c.dilation)}))"
        else:
            builder += f".cuboid({f(ox)}, {f(oy)}, {f(oz)}, {f(sx)}, {f(sy)}, {f(sz)})"
        if c.mirror:
            builder += ".mirrored(false)"
    px, py, pz = p.pivot
    rx, ry, rz = p.rot
    var = f"p_{p.name}"
    lines.append(f"\t\tModelPartData {var} = {parent}.addChild(\"{p.name}\", {builder}, ModelTransform.of({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)}));")
    for ch in p.children:
        java_part(ch, var, lines, depth + 1)


def java_model(m):
    lines = [f"\tpublic static TexturedModelData {m.method}() {{",
             "\t\tModelData data = new ModelData();",
             "\t\tModelPartData root = data.getRoot();"]
    for p in m.parts:
        java_part(p, "root", lines, 0)
    lines.append(f"\t\treturn TexturedModelData.of(data, {m.tex[0]}, {m.tex[1]});")
    lines.append("\t}")
    return "\n".join(lines)


# ------------------------------------------------------------------------------------------------
# Model definitions

TOAD_GREEN = (108, 154, 60)
TOAD_DARK = (70, 110, 40)
TOAD_BELLY = (232, 214, 140)


def toad():
    skin = noise_fill(TOAD_GREEN, 0.07, spots=(TOAD_DARK, 0.12))
    body_paint = faces(skin, bottom=noise_fill(TOAD_BELLY, 0.05))
    head_paint = faces(skin, bottom=noise_fill(TOAD_BELLY, 0.05), front=gradient(TOAD_GREEN, (60, 95, 35)))

    def lip(img, x, y, w, h, face, rng):
        noise_fill(TOAD_GREEN, 0.06)(img, x, y, w, h, face, rng)
        if face == "front":
            for i in range(w):
                img.putpixel((x + i, y + h - 1), (60, 20, 30, 255))
    jaw_paint = faces(noise_fill(TOAD_BELLY, 0.05), top=noise_fill((200, 70, 90), 0.08),
                      front=noise_fill(TOAD_BELLY, 0.05), left=skin, right=skin)
    leg = faces(skin, bottom=noise_fill((90, 120, 50), 0.05))
    eye = eye_face(TOAD_GREEN, (232, 196, 60))
    return Model("giant_toad", (128, 128), [
        Part("body", (0, 20, 1), (-0.18, 0, 0), [Cube((-10, -15, -11), (20, 15, 24), body_paint)]),
        Part("head", (0, 9, -9), (0, 0, 0), [Cube((-9, -6, -12), (18, 7, 13), faces(head_paint, front=lip))], [
            Part("eye_left", (-5, -6, -6), (0, 0, 0), [Cube((-2.5, -3.5, -2.5), (5, 4, 5), eye)]),
            Part("eye_right", (5, -6, -6), (0, 0, 0), [Cube((-2.5, -3.5, -2.5), (5, 4, 5), eye)]),
            Part("jaw", (0, 1, 0), (0, 0, 0), [Cube((-8.5, 0, -11.5), (17, 4, 12), jaw_paint),
                                               Cube((-3, 0.5, -10), (6, 1, 8), noise_fill((210, 80, 110), 0.05))]),
        ]),
        Part("leg_front_left", (-7, 17, -7), (0, 0, 0), [Cube((-2, 0, -2), (4, 7, 4), leg)]),
        Part("leg_front_right", (7, 17, -7), (0, 0, 0), [Cube((-2, 0, -2), (4, 7, 4), leg, mirror=True)]),
        Part("leg_back_left", (-9, 15, 8), (0, 0, 0), [Cube((-4, -2, -5), (6, 9, 11), leg), Cube((-4, 7, -9), (6, 2, 9), leg)]),
        Part("leg_back_right", (9, 15, 8), (0, 0, 0), [Cube((-2, -2, -5), (6, 9, 11), leg, mirror=True), Cube((-2, 7, -9), (6, 2, 9), leg, mirror=True)]),
    ])


def cabbage():
    leaf = (150, 210, 105)
    leaf_dark = (110, 175, 75)
    core_p = faces(noise_fill(leaf, 0.06, spots=(leaf_dark, 0.15)))

    def face_paint(img, x, y, w, h, face, rng):
        core_p(img, x, y, w, h, face, rng)
        if face == "front":
            for px, py in ((2, 3), (w - 3, 3)):
                img.putpixel((x + px, y + py), (20, 30, 20, 255))
                img.putpixel((x + px, y + py + 1), (20, 30, 20, 255))
            img.putpixel((x + w // 2, y + 6), (200, 90, 110, 255))

    veins = noise_fill((170, 225, 130), 0.05, spots=((210, 240, 190), 0.2))
    return Model("flying_cabbage", (64, 64), [
        Part("head", (0, 18, 0), (0, 0, 0), [Cube((-4, -4, -4), (8, 8, 8), face_paint),
                                            Cube((-4.5, -4.8, -4.5), (9, 4, 9), noise_fill(leaf_dark, 0.08), dilation=0)], [
            Part("wing_left", (-4, -1, 0), (0, 0, 0.3), [Cube((-8, 0, -4), (8, 1, 8), veins)]),
            Part("wing_right", (4, -1, 0), (0, 0, -0.3), [Cube((0, 0, -4), (8, 1, 8), veins, mirror=True)]),
            Part("leaf_top", (0, -4, 0), (0.3, 0.6, 0), [Cube((-3, -3, -0.5), (6, 3, 1), veins)]),
        ]),
    ])


def golem():
    bronze = (150, 112, 62)
    dark = (90, 66, 36)
    metal = plates(bronze, dark, 5, 0.07, rivets=(200, 170, 110))

    def head_paint(img, x, y, w, h, face, rng):
        metal(img, x, y, w, h, face, rng)
        if face == "front":
            for i in range(1, w - 1):
                for j in range(3, 5):
                    img.putpixel((x + i, y + j), (20, 20, 25, 255))
            for i in (2, w - 3):
                img.putpixel((x + i, y + 3), (90, 240, 255, 255))
                img.putpixel((x + i, y + 4), (60, 200, 255, 255))

    def body_paint(img, x, y, w, h, face, rng):
        metal(img, x, y, w, h, face, rng)
        if face == "front":
            cx, cy = w // 2, h // 2 - 1
            for i in range(-2, 2):
                for j in range(-2, 2):
                    img.putpixel((x + cx + i, y + cy + j), (70, 230, 255, 255) if abs(i + 0.5) + abs(j + 0.5) < 2.5 else (30, 120, 160, 255))
    return Model("destroyer_golem", (128, 128), [
        Part("head", (0, -7, -2), (0, 0, 0), [Cube((-4, -12, -5.5), (8, 10, 8), head_paint)]),
        Part("body", (0, -7, 0), (0, 0, 0), [Cube((-9, -2, -6), (18, 12, 11), body_paint),
                                            Cube((-4.5, 10, -3), (9, 5, 6), metal, dilation=0.5)]),
        Part("right_arm", (0, -7, 0), (0, 0, 0), [Cube((-13, -2.5, -3), (4, 30, 6), metal)]),
        Part("left_arm", (0, -7, 0), (0, 0, 0), [Cube((9, -2.5, -3), (4, 30, 6), metal, mirror=True)]),
        Part("right_leg", (-4, 11, 0), (0, 0, 0), [Cube((-3.5, -3, -3), (6, 16, 5), metal)]),
        Part("left_leg", (5, 11, 0), (0, 0, 0), [Cube((-3.5, -3, -3), (6, 16, 5), metal, mirror=True)]),
    ])


def destroyer():
    hull = (140, 128, 110)
    seam = (80, 72, 62)
    bronze = (156, 110, 60)
    hull_p = plates(hull, seam, 6, 0.08, rivets=(190, 180, 160))
    deck_p = faces(hull_p, top=plates((120, 112, 98), seam, 4, 0.1))
    tower_p = plates((170, 160, 140), (100, 90, 80), 4, 0.07)
    roof_p = noise_fill((150, 60, 40), 0.1, spots=((120, 45, 30), 0.2))
    leg_p = plates((70, 66, 64), (40, 38, 36), 3, 0.08)
    joint_p = noise_fill(bronze, 0.1)

    def face_p(img, x, y, w, h, face, rng):
        hull_p(img, x, y, w, h, face, rng)
        if face == "front":
            # two huge glowing eyes and a grille
            for (ex, ey) in ((w // 4, h // 3), (3 * w // 4, h // 3)):
                for i in range(-2, 3):
                    for j in range(-1, 2):
                        img.putpixel((x + ex + i, y + ey + j), (255, 80, 40, 255) if abs(i) < 2 else (255, 180, 60, 255))
            for i in range(w // 4, 3 * w // 4):
                if i % 2 == 0:
                    for j in range(2 * h // 3, h - 1):
                        img.putpixel((x + i, y + j), (30, 26, 22, 255))

    def window_p(img, x, y, w, h, face, rng):
        tower_p(img, x, y, w, h, face, rng)
        if face in ("front", "back", "left", "right"):
            for i in range(1, w - 1, 3):
                for j in range(1, h - 1, 3):
                    img.putpixel((x + i, y + j), (255, 210, 120, 255) if rng.random() < 0.6 else (40, 40, 50, 255))

    # Spider legs: the femur rises above the hull, the long shin comes down to the ground.
    # Front legs splay forward, back legs backward. KonoModels.Destroyer animates these angles.
    legs = []
    zs = [-17, -6, 6, 17]
    spread = [0.38, 0.13, -0.13, -0.38]
    for side, sx in (("l", -1), ("r", 1)):
        for k, z in enumerate(zs):
            name = f"leg_{side}{k}"
            legs.append(Part(name, (sx * 19, 8, z), (0, sx * spread[k], sx * -0.75), [
                Cube((-2.5, -2.5, -2.5), (5, 5, 5), joint_p),
                Cube((0, -2, -2) if sx > 0 else (-16, -2, -2), (16, 4, 4), leg_p),
                Cube((2, -3, -1) if sx > 0 else (-14, -3, -1), (12, 1, 2), joint_p)], [
                Part(name + "_lower", (sx * 16, 0, 0), (0, 0, sx * 0.55), [
                    Cube((-2.5, -2.5, -2.5), (5, 5, 5), joint_p),
                    Cube((-1.5, 2, -1.5), (3, 24, 3), leg_p),
                    Cube((-2, 8, -2), (4, 6, 4), plates((90, 84, 80), (40, 38, 36), 3, 0.08)),
                    Cube((-3, 25, -3), (6, 2, 6), joint_p)])]))
    belly_p = plates((110, 100, 88), seam, 5, 0.08)
    return Model("destroyer", (256, 256), [
        Part("hull", (0, 4, 0), (0, 0, 0), [
            Cube((-20, -6, -24), (40, 12, 48), deck_p),
            Cube((-17, 6, -21), (34, 3, 42), hull_p),
            Cube((-13, 9, -17), (26, 3, 34), belly_p),
            Cube((-8, 12, -11), (16, 2, 22), belly_p),
        ], [
            Part("front", (0, 0, -24), (0, 0, 0), [Cube((-12, -5, -8), (24, 10, 8), face_p)]),
            Part("tower_main", (0, -6, 2), (0, 0, 0), [Cube((-10, -9, -10), (20, 9, 18), window_p),
                                                      Cube((-11, -11, -11), (22, 2, 20), roof_p)]),
            Part("tower_a", (-12, -6, -14), (0, 0, 0), [Cube((-3, -7, -3), (6, 7, 6), window_p), Cube((-4, -9, -4), (8, 2, 8), roof_p)]),
            Part("tower_b", (12, -6, -14), (0, 0, 0), [Cube((-3, -7, -3), (6, 7, 6), window_p), Cube((-4, -9, -4), (8, 2, 8), roof_p)]),
            Part("tower_c", (-13, -6, 16), (0, 0, 0), [Cube((-3, -6, -3), (6, 6, 6), window_p), Cube((-4, -8, -4), (8, 2, 8), roof_p)]),
            Part("tower_d", (13, -6, 16), (0, 0, 0), [Cube((-3, -6, -3), (6, 6, 6), window_p), Cube((-4, -8, -4), (8, 2, 8), roof_p)]),
            Part("turret_l", (-17, -6, -2), (0, 0, 0), [Cube((-2, -3, -2), (4, 3, 4), joint_p), Cube((-0.5, -2.5, -7), (1, 1, 6), leg_p)]),
            Part("turret_r", (17, -6, -2), (0, 0, 0), [Cube((-2, -3, -2), (4, 3, 4), joint_p), Cube((-0.5, -2.5, -7), (1, 1, 6), leg_p)]),
        ]),
    ] + legs)


def crystal():
    purple = (190, 110, 255)
    return Model("barrier_crystal", (64, 64), [
        Part("gem", (0, 10, 0), (0, 0.785, 0), [Cube((-3, -12, -3), (6, 16, 6), gradient((240, 200, 255), (120, 50, 210), 0.04))], [
            Part("gem_tip", (0, -12, 0), (0.785, 0, 0.785), [Cube((-2, -2, -2), (4, 4, 4), noise_fill((250, 230, 255), 0.03))]),
        ]),
        Part("base", (0, 24, 0), (0, 0, 0), [Cube((-5, -3, -5), (10, 3, 10), plates((80, 70, 90), (40, 35, 50), 4, 0.06)),
                                            Cube((-6, -1, -6), (12, 1, 12), noise_fill(purple, 0.1))]),
    ])


def core():
    return Model("destroyer_core", (128, 64), [
        Part("outer", (0, 12, 0), (0, 0, 0), [Cube((-8, -8, -8), (16, 16, 16), glow((220, 60, 20), (255, 230, 120)))], [
            Part("inner", (0, 0, 0), (0.785, 0.785, 0), [Cube((-4, -4, -4), (8, 8, 8), glow((255, 200, 60), (255, 255, 230)))]),
        ]),
        Part("ring", (0, 12, 0), (0, 0, 0), [Cube((-11, -1, -11), (22, 2, 22), plates((70, 60, 55), (30, 25, 22), 4, 0.06))]),
    ])


def megumin_hat():
    black = (30, 26, 34)
    band_red = (190, 30, 40)
    hat = noise_fill(black, 0.08)
    band = noise_fill(band_red, 0.06)

    def band_paint(img, x, y, w, h, face, rng):
        band(img, x, y, w, h, face, rng)
        if face == "front":
            for i in range(w // 2 - 1, w // 2 + 1):
                for j in range(h):
                    img.putpixel((x + i, y + j), (230, 190, 60, 255))
    return Model("megumin_hat", (64, 64), [
        Part("hat", (0, 0, 0), (0, 0, 0), [
            Cube((-8, -9, -8), (16, 1, 16), hat),
            Cube((-4.5, -13, -4.5), (9, 4, 9), hat),
            Cube((-4.6, -11, -4.6), (9.2, 1.5, 9.2), band_paint),
        ], [
            Part("tip1", (0, -13, 0), (-0.2, 0, 0), [Cube((-3.5, -4, -3.5), (7, 4, 7), hat)], [
                Part("tip2", (0, -4, 0.5), (-0.35, 0, 0), [Cube((-2.5, -4, -2.5), (5, 4, 5), hat)], [
                    Part("tip3", (0, -4, 0.5), (-0.5, 0, 0), [Cube((-1.5, -3, -1.5), (3, 3, 3), hat)]),
                ]),
            ]),
        ]),
    ], method="meguminHat")


MODELS = [toad(), cabbage(), golem(), destroyer(), crystal(), core(), megumin_hat()]


def main():
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
    methods = []
    for i, m in enumerate(MODELS):
        pack(m)
        img = paint_model(m, 1000 + i)
        img.save(os.path.join(TEX_DIR, m.texture + ".png"))
        methods.append(java_model(m))
        print("model", m.name, m.tex)
    java = """package dev.konocraft.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;

/** GENERATED by tools/gen_models.py - do not edit by hand. */
@SuppressWarnings("unused")
public final class GeneratedModels {
	private GeneratedModels() {
	}

""" + "\n\n".join(methods) + "\n}\n"
    with open(JAVA_OUT, "w") as fh:
        fh.write(java)


if __name__ == "__main__":
    main()
