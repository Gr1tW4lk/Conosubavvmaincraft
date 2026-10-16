#!/usr/bin/env python3
"""16x16 item/block sprites, item & block model JSON, blockstates and misc textures.

Sprites are drawn as character grids with a palette (pixel art by hand), or with small
procedural helpers for diagonal weapons. Run from the repo root: python3 tools/gen_items.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/konocraft")
ITEM_TEX = os.path.join(ASSETS, "textures/item")
BLOCK_TEX = os.path.join(ASSETS, "textures/block")
ENTITY_TEX = os.path.join(ASSETS, "textures/entity")


def grid(rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != "." and ch in palette:
                c = palette[ch]
                img.putpixel((x, y), c if len(c) == 4 else c + (255,))
    return img


def save(img, name, folder=ITEM_TEX):
    os.makedirs(folder, exist_ok=True)
    img.save(os.path.join(folder, name + ".png"))


def line(img, x0, y0, x1, y1, color, width=1):
    n = max(abs(x1 - x0), abs(y1 - y0)) * 2 + 1
    for i in range(n + 1):
        t = i / n
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        for dx in range(width):
            px, py = int(round(x)) + dx, int(round(y))
            if 0 <= px < 16 and 0 <= py < 16:
                img.putpixel((px, py), color + (255,) if len(color) == 3 else color)


def diag(img, x0, y0, x1, y1, color, dark=None):
    """1px diagonal with a darker shadow pixel underneath."""
    line(img, x0, y0, x1, y1, color)
    if dark:
        line(img, x0 + 1, y0, x1 + 1, y1, dark)
        line(img, x0, y0, x1, y1, color)


def put(img, pts, color):
    for x, y in pts:
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), color + (255,) if len(color) == 3 else color)


# ------------------------------------------------------------------------------------------------

def adventurer_card():
    return grid([
        "................",
        "................",
        ".oooooooooooooo.",
        ".oGGGGGGGGGGGGo.",
        ".oGccccccccccGo.",
        ".oGcppcllllllGo.",
        ".oGcpscccccccGo.",
        ".oGcppcllllccGo.",
        ".oGcccccccccc Go".replace(" ", "c")[:16],
        ".oGcllllllllcGo.",
        ".oGcccccccccccGo"[:16],
        ".oGcllllcrrrcGo.",
        ".oGGGGGGGGGGGGo.",
        ".oooooooooooooo.",
        "................",
        "................",
    ], {"o": (90, 60, 30), "G": (220, 175, 70), "c": (245, 232, 200), "p": (240, 200, 170), "s": (60, 40, 30),
        "l": (150, 130, 100), "r": (200, 50, 50)})


def guide_book():
    return grid([
        "................",
        "...dddddddddd...",
        "..dggggggggggd..",
        "..dgGGGGGGGGgdw.",
        "..dgGggYYggGgdw.",
        "..dgGgYggYgGgdw.",
        "..dgGgggYggGgdw.",
        "..dgGggYgggGgdw.",
        "..dgGgggggGGgdw.",
        "..dgGggYgggGgdw.",
        "..dgGGGGGGGGgdw.",
        "..dggggggggggdw.",
        "..dggggggggggdw.",
        "...dddddddddd.w.",
        "....wwwwwwwwwww.",
        "................",
    ], {"d": (30, 60, 30), "g": (60, 130, 60), "G": (200, 170, 70), "Y": (250, 230, 120), "w": (235, 228, 210)})


def sword(name, blade, edge, guard, grip, pommel, length=11, broad=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # blade from (4,11) up to (4+length, 11-length) diagonal
    x0, y0 = 5, 10
    x1, y1 = min(15, x0 + length - 1), max(0, y0 - length + 1)
    line(img, x0, y0, x1, y1, blade)
    line(img, x0 + 1, y0, x1, y1 + 1, edge)
    if broad:
        line(img, x0, y0 - 1, x1 - 1, y1, edge)
    put(img, [(x1, y1)], (255, 255, 255))
    # guard (perpendicular)
    put(img, [(2, 9), (3, 10), (4, 11), (5, 12), (6, 13), (3, 9), (6, 12)], guard)
    # grip
    put(img, [(3, 12), (2, 13)], grip)
    put(img, [(4, 12)], grip)
    put(img, [(1, 14), (2, 14), (1, 13)], pommel)
    save(img, name)


def staff(name, wood, wood_dark, gem, gem_light, frame=None, gnarled=False):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    line(img, 1, 15, 11, 5, wood)
    line(img, 2, 15, 11, 6, wood_dark)
    line(img, 1, 15, 11, 5, wood)
    if gnarled:
        put(img, [(4, 11), (7, 8)], wood_dark)
        # hook at the top around the gem
        put(img, [(11, 4), (12, 3), (13, 2), (14, 2), (15, 3), (15, 4), (15, 5)], wood)
        put(img, [(12, 2), (14, 1)], wood_dark)
    if frame:
        put(img, [(10, 3), (11, 2), (14, 5), (13, 6), (10, 6), (14, 2)], frame)
    put(img, [(11, 3), (12, 3), (11, 4), (12, 4), (13, 4), (12, 5), (13, 5), (13, 3)], gem)
    put(img, [(12, 3), (11, 4)], gem_light)
    put(img, [(12, 4)], (255, 255, 255))
    save(img, name)


def simple(name, rows, palette, folder=ITEM_TEX):
    save(grid(rows, palette), name, folder)


def potion(name, liquid, light, cork=(140, 100, 60), extra=None):
    pal = {"g": (210, 225, 235), "G": (170, 185, 200), "l": liquid, "L": light, "c": cork, "w": (255, 255, 255)}
    rows = [
        "................",
        "......cccc......",
        "......cccc......",
        ".......gg.......",
        ".......gg.......",
        "......gGGg......",
        ".....gllllg.....",
        "....glLllllg....",
        "...glLwlllllg...",
        "...gllllllllg...",
        "...gllllllllg...",
        "...glllllllLg...",
        "....gllllllg....",
        ".....gggggg.....",
        "................",
        "................",
    ]
    img = grid(rows, pal)
    if extra:
        extra(img)
    save(img, name)


def items():
    save(adventurer_card(), "adventurer_card")
    save(guide_book(), "guide_book")
    sword("chunchunmaru", (215, 222, 232), (150, 160, 175), (200, 160, 60), (120, 30, 40), (40, 30, 30), length=12)
    sword("crusader_sword", (225, 230, 240), (140, 150, 170), (220, 180, 70), (60, 80, 160), (70, 110, 220), length=11, broad=True)
    staff("manatite_staff", (150, 105, 60), (100, 70, 40), (70, 150, 255), (170, 220, 255), frame=(200, 200, 210))
    staff("goddess_scepter", (230, 190, 80), (180, 140, 50), (240, 250, 255), (160, 220, 255), frame=(230, 190, 80))
    staff("megumin_staff", (110, 74, 44), (70, 46, 26), (220, 30, 40), (255, 120, 110), frame=(230, 190, 70), gnarled=True)
    staff("staff_replica", (190, 140, 90), (140, 100, 60), (240, 110, 170), (255, 190, 220), gnarled=True)

    simple("toad_meat", [
        "................",
        "................",
        ".....gggg.......",
        "....gGGGgg......",
        "...gGrrrrgg.....",
        "...grRrrrrrg....",
        "..grrrRrrrrrg...",
        "..grrrrrRrrrrw..",
        "..grrrrrrrrrrww.",
        "...grrrRrrrrrw..",
        "....grrrrrrrg...",
        ".....grrrrgg....",
        "......gggg......",
        "................",
        "................",
        "................",
    ], {"g": (90, 130, 50), "G": (120, 170, 70), "r": (225, 120, 120), "R": (250, 160, 160), "w": (245, 235, 225)})
    simple("fried_toad_legs", [
        "................",
        "...........ww...",
        "..........wwww..",
        "..........bwwb..",
        ".........bb.....",
        "....oooobb......",
        "...oOOOoob......",
        "..oOyyOOoo..ww..",
        "..oOOOyOOoo.wwww",
        "..ooOOOOOooobwwb",
        "...ooooOOOoobb..",
        "....ooooooooo...",
        "......ooooo.....",
        "................",
        "................",
        "................",
    ], {"o": (190, 120, 40), "O": (225, 160, 60), "y": (250, 210, 110), "w": (245, 240, 225), "b": (170, 120, 70)})
    simple("cabbage", [
        "................",
        "................",
        "......GGGG......",
        "....GGgggGGG....",
        "...GggLLggggG...",
        "..GgLLgggLLggG..",
        "..GgLggvggLggG..",
        ".GggggvvvgggggG.",
        ".GgLggvggggLggG.",
        ".GgLLgvggLLLggG.",
        "..GgggvggggggG..",
        "..GGggggggLgGG..",
        "...GGGgggggGG...",
        ".....GGGGGG.....",
        "................",
        "................",
    ], {"G": (70, 140, 50), "g": (130, 200, 90), "L": (175, 225, 135), "v": (220, 245, 200)})
    simple("cabbage_salad", [
        "................",
        "................",
        "................",
        ".....gLgGr......",
        "...gLggLgrrGg...",
        "..gLgGgLggrgLg..",
        "..wwwwwwwwwwww..",
        "..wbbbbbbbbbbw..",
        "...wbbbbbbbbw...",
        "...wbbbbbbbbw...",
        "....wbbbbbbw....",
        ".....wwwwww.....",
        "................",
        "................",
        "................",
        "................",
    ], {"g": (110, 190, 80), "L": (170, 230, 130), "G": (70, 140, 50), "r": (220, 60, 60), "w": (240, 240, 245), "b": (190, 200, 215)})
    simple("crimson_soda", [
        "................",
        "......ww........",
        "......cc........",
        "......gg........",
        "......gg..o.....",
        ".....gggg...o...",
        "....grrrrg......",
        "....grRwrg..o...",
        "....grrrrg......",
        "....grrRrg......",
        "....gyyyyg......",
        "....grrrrg......",
        "....grrrrg......",
        "....gggggg......",
        "................",
        "................",
    ], {"w": (240, 240, 240), "c": (200, 40, 40), "g": (200, 220, 230), "r": (200, 30, 50), "R": (255, 90, 100), "y": (240, 200, 80),
        "o": (255, 200, 210)})
    potion("mana_potion", (40, 90, 230), (120, 170, 255))
    potion("holy_water", (150, 220, 255), (230, 250, 255), cork=(230, 190, 80),
           extra=lambda img: put(img, [(7, 7), (7, 8), (7, 9), (7, 10), (6, 8), (8, 8)], (255, 230, 120)))

    def fuse(img):
        put(img, [(7, 0), (8, 0), (9, 0)], (60, 40, 30))
        put(img, [(10, 0)], (255, 200, 60))
        put(img, [(6, 8), (7, 8), (8, 9), (9, 8), (6, 10), (9, 10)], (30, 30, 30))
    potion("explosive_potion", (230, 110, 30), (255, 190, 90), extra=fuse)
    simple("manatite", [
        "................",
        "................",
        "........L.......",
        ".......LbL......",
        "......LbbbL..L..",
        "...L..LbBbL.LbL.",
        "..LbL.LbbbL.LbL.",
        "..LbbLLbBbLLbbL.",
        "..LbBbLbbbLbBbL.",
        "..LbbbLbbbLbbbL.",
        "...LbbbbbbbbbL..",
        "...ssssssssss...",
        "..ssssssssssss..",
        "................",
        "................",
        "................",
    ], {"L": (190, 230, 255), "b": (70, 140, 240), "B": (230, 250, 255), "s": (110, 110, 120)})
    simple("coronatite", [
        "................",
        "................",
        ".......yy.......",
        "......yOOy......",
        ".....yOrrOy.....",
        "....yOrRRrOy....",
        "...yOrRwwRrOy...",
        "...yOrRwwRrOy...",
        "....yOrRRrOy....",
        ".....yOrrOy.....",
        "......yOOy......",
        ".......yy.......",
        "................",
        "................",
        "................",
        "................",
    ], {"y": (255, 230, 120), "O": (255, 150, 40), "r": (230, 70, 20), "R": (255, 120, 50), "w": (255, 250, 220)})
    simple("teleport_scroll", [
        "................",
        "................",
        "...ccc..........",
        "..cpppcccccc....",
        "..cpPpppppppc...",
        "...cpppppppppc..",
        "....cppvvvpppc..",
        "....cpvPPvvppc..",
        "....cppvvvpppc..",
        "....cppppppppc..",
        "....cpppppppc...",
        "....cccccccpppc.",
        "...........cppc.",
        "............cc..",
        "................",
        "................",
    ], {"c": (150, 120, 80), "p": (240, 225, 190), "P": (200, 180, 140), "v": (150, 90, 220)})
    simple("toad_ring", [
        "................",
        "................",
        "......gGGg......",
        ".....gGllGg.....",
        "......gGGg......",
        ".....yy..yy.....",
        "....y......y....",
        "...y........y...",
        "...y........y...",
        "...y........y...",
        "....y......y....",
        ".....yy..yy.....",
        ".......yy.......",
        "................",
        "................",
        "................",
    ], {"g": (60, 120, 40), "G": (110, 180, 70), "l": (240, 220, 80), "y": (230, 180, 60)})
    simple("axis_form", [
        "................",
        "...pppppppppp...",
        "...pbbbbbbbbp...",
        "...pbwwbbwwbp...",
        "...pbbbbbbbbp...",
        "...pllllllllp...",
        "...pppppppppp...",
        "...plllllllpp...",
        "...pppppppppp...",
        "...pllllllllp...",
        "...ppppppppbp...",
        "...plllllpbbp...",
        "...pppppppbpp...",
        "...pppppppppp...",
        "................",
        "................",
    ], {"p": (245, 245, 250), "b": (70, 150, 240), "w": (255, 255, 255), "l": (170, 170, 190)})
    simple("beldia_head", [
        "................",
        ".......pp.......",
        "......pPPp......",
        "....dddddddd....",
        "...dPddddddPd...",
        "...ddddddddd d..".replace(" ", "d")[:16],
        "...dkkkkkkkkd...",
        "...dkRkkkkRkd...",
        "...dkkkkkkkkd...",
        "...ddddddddddd..",
        "...dDdDdDdDdd...",
        "....dddddddd....",
        ".....dddddd.....",
        "................",
        "................",
        "................",
    ], {"d": (50, 48, 64), "D": (30, 28, 38), "P": (110, 62, 152), "p": (150, 90, 200), "k": (15, 12, 20), "R": (255, 40, 40)})
    simple("destroyer_diary", [
        "................",
        "...bbbbbbbbbb...",
        "..bBBBBBBBBBBb..",
        "..bBBBggggBBBbw.",
        "..bBBgBBBBgBBbw.",
        "..bBgBBggBBgBbw.",
        "..bBgBgBBgBgBbw.",
        "..bBgBBggBBgBbw.",
        "..bBBgBBBBgBBbw.",
        "..bBBBggggBBBbw.",
        "..bBBBBBBBBBBbw.",
        "..bBBkBBBBBBBbw.",
        "..bBBBBBBBBBBbw.",
        "...bbbbbbbbbb.w.",
        "....wwwwwwwwwww.",
        "................",
    ], {"b": (70, 45, 25), "B": (120, 80, 45), "g": (210, 170, 80), "w": (230, 220, 200), "k": (40, 30, 20)})

    def orb(name, core, mid, outer):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for x in range(16):
            for y in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 2.2:
                    img.putpixel((x, y), core + (255,))
                elif d < 4.2:
                    img.putpixel((x, y), mid + (255,))
                elif d < 6.0:
                    img.putpixel((x, y), outer + (int(220 * (6.0 - d) / 1.8),))
        save(img, name)
    orb("magic_bolt", (255, 255, 230), (255, 220, 120), (255, 170, 60))
    orb("dark_bolt", (240, 200, 255), (160, 70, 220), (70, 20, 110))


def blocks():
    rng = random.Random(5)
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            base = (120, 82, 46) if (y // 4) % 2 == 0 else (108, 72, 40)
            n = 1 + (rng.random() - 0.5) * 0.15
            c = tuple(max(0, min(255, int(v * n))) for v in base)
            img.putpixel((x, y), c + (255,))
    for x in range(16):
        img.putpixel((x, 0), (70, 45, 25, 255))
        img.putpixel((x, 15), (70, 45, 25, 255))
    for y in range(16):
        img.putpixel((0, y), (70, 45, 25, 255))
        img.putpixel((15, y), (70, 45, 25, 255))
    # pinned papers
    papers = [(2, 2, 6, 6, (240, 232, 205)), (8, 3, 13, 8, (235, 240, 220)), (3, 9, 8, 13, (245, 225, 200)), (10, 10, 13, 13, (240, 230, 235))]
    for x0, y0, x1, y1, col in papers:
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                img.putpixel((x, y), col + (255,))
        for y in range(y0 + 2, y1, 2):
            for x in range(x0 + 1, x1):
                if rng.random() < 0.8:
                    img.putpixel((x, y), (140, 130, 115, 255))
        img.putpixel(((x0 + x1) // 2, y0), (200, 40, 40, 255))
    img.putpixel((5, 11), (60, 120, 200, 255))
    save(img, "quest_board", BLOCK_TEX)
    side = Image.new("RGBA", (16, 16), (90, 60, 32, 255))
    save(side, "quest_board_side", BLOCK_TEX)


def misc():
    # Barrier dome texture: hexagonal lattice
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for x in range(32):
        for y in range(32):
            a = (x + (y // 8 % 2) * 4) % 8
            b = y % 8
            edge = a == 0 or b == 0 or (a + b) % 8 == 0
            img.putpixel((x, y), (255, 255, 255, 230) if edge else (255, 255, 255, 60))
    save(img, "barrier", ENTITY_TEX)
    # Toad king: recolour the toad texture (darker, golden spots)
    toad = Image.open(os.path.join(ENTITY_TEX, "giant_toad.png")).convert("RGBA")
    king = Image.new("RGBA", toad.size)
    rng = random.Random(9)
    for x in range(toad.width):
        for y in range(toad.height):
            r, g, b, a = toad.getpixel((x, y))
            if a == 0:
                king.putpixel((x, y), (0, 0, 0, 0))
                continue
            if g > r and g > b:
                r, g, b = int(r * 0.7), int(g * 0.75), int(b * 0.6)
                if rng.random() < 0.05:
                    r, g, b = 230, 190, 60
            king.putpixel((x, y), (r, g, b, a))
    king.save(os.path.join(ENTITY_TEX, "toad_king.png"))
    # mod icon: Megumin-style explosion over a hat silhouette
    icon = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for x in range(128):
        for y in range(128):
            d = math.hypot(x - 64, y - 60)
            if d < 58:
                t = d / 58
                col = (255, int(240 - 160 * t), int(180 - 170 * t), 255) if t < 0.85 else (190, 40, 30, 255)
                if (math.atan2(y - 60, x - 64) * 8 / math.pi) % 2 < 0.25 and t > 0.4:
                    col = (255, 250, 200, 255)
                icon.putpixel((x, y), col)
    for x in range(20, 108):
        for y in range(100, 108):
            icon.putpixel((x, y), (30, 26, 34, 255))
    for y in range(40, 100):
        half = int((y - 40) * 0.35) + 3
        for x in range(64 - half, 64 + half):
            icon.putpixel((x, y), (30, 26, 34, 255))
    for x in range(44, 85):
        for y in range(88, 94):
            icon.putpixel((x, y), (200, 30, 40, 255))
    icon.save(os.path.join(ASSETS, "icon.png"))


# ------------------------------------------------------------------------------------------------
# JSON: item models, block models, blockstates

HANDHELD = {"chunchunmaru", "crusader_sword", "manatite_staff", "goddess_scepter", "megumin_staff", "staff_replica"}
EGGS = ["giant_toad", "toad_king", "flying_cabbage", "goblin", "undead_knight", "destroyer_golem", "beldia", "townsfolk"]


def models():
    mdir = os.path.join(ASSETS, "models/item")
    os.makedirs(mdir, exist_ok=True)
    names = [f[:-4] for f in os.listdir(ITEM_TEX) if f.endswith(".png")]
    for n in names:
        parent = "minecraft:item/handheld" if n in HANDHELD else "minecraft:item/generated"
        json.dump({"parent": parent, "textures": {"layer0": f"konocraft:item/{n}"}}, open(os.path.join(mdir, n + ".json"), "w"), indent=2)
    for e in EGGS:
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(mdir, e + "_spawn_egg.json"), "w"), indent=2)
    json.dump({"parent": "konocraft:block/quest_board"}, open(os.path.join(mdir, "quest_board.json"), "w"), indent=2)
    bdir = os.path.join(ASSETS, "models/block")
    os.makedirs(bdir, exist_ok=True)
    json.dump({
        "parent": "minecraft:block/block",
        "textures": {"front": "konocraft:block/quest_board", "side": "konocraft:block/quest_board_side", "particle": "konocraft:block/quest_board"},
        "elements": [{
            "from": [0, 0, 13], "to": [16, 16, 16],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "east": {"uv": [13, 0, 16, 16], "texture": "#side"},
                "west": {"uv": [0, 0, 3, 16], "texture": "#side"},
                "up": {"uv": [0, 13, 16, 16], "texture": "#side"},
                "down": {"uv": [0, 0, 16, 3], "texture": "#side"}
            }
        }],
        "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]}}
    }, open(os.path.join(bdir, "quest_board.json"), "w"), indent=2)
    sdir = os.path.join(ASSETS, "blockstates")
    os.makedirs(sdir, exist_ok=True)
    json.dump({"variants": {
        "facing=north": {"model": "konocraft:block/quest_board"},
        "facing=east": {"model": "konocraft:block/quest_board", "y": 90},
        "facing=south": {"model": "konocraft:block/quest_board", "y": 180},
        "facing=west": {"model": "konocraft:block/quest_board", "y": 270}
    }}, open(os.path.join(sdir, "quest_board.json"), "w"), indent=2)
    # loot table so the board drops itself
    ldir = os.path.join(ROOT, "src/main/resources/data/konocraft/loot_tables/blocks")
    os.makedirs(ldir, exist_ok=True)
    json.dump({"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "konocraft:quest_board"}],
                                                      "conditions": [{"condition": "minecraft:survives_explosion"}]}]},
              open(os.path.join(ldir, "quest_board.json"), "w"), indent=2)


if __name__ == "__main__":
    items()
    blocks()
    misc()
    models()
    print("items done")
