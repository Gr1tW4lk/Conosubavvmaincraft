#!/usr/bin/env python3
"""Copies the chosen fan skins into the mod's textures and records which ones use slim arms.

    python3 tools/install_skins.py CANDIDATES_DIR

Choices live in tools/skins_chosen.json: {"megumin": "megumin_03.png", ...}. 64x32 legacy skins
are converted to the 64x64 layout (left limbs mirrored from the right ones), like the game does.
"""
import json
import os
import re
import sys

from PIL import Image, ImageOps

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/konocraft/textures/entity/character")
RENDERER = os.path.join(ROOT, "src/client/java/dev/konocraft/client/render/CharacterRenderer.java")


def legacy_to_modern(img):
    out = Image.new("RGBA", (64, 64))
    out.paste(img, (0, 0))

    def mirror_limb(sx, sy, dx, dy):
        # faces of a 4x4x12 limb: top, bottom, right, front, left, back
        def box(x, y, w, h):
            return img.crop((x, y, x + w, y + h))
        top, bottom = box(sx + 4, sy, 4, 4), box(sx + 8, sy, 4, 4)
        right, front = box(sx, sy + 4, 4, 12), box(sx + 4, sy + 4, 4, 12)
        left, back = box(sx + 8, sy + 4, 4, 12), box(sx + 12, sy + 4, 4, 12)
        flip = ImageOps.mirror
        out.paste(flip(top), (dx + 4, dy))
        out.paste(flip(bottom), (dx + 8, dy))
        out.paste(flip(left), (dx, dy + 4))
        out.paste(flip(front), (dx + 4, dy + 4))
        out.paste(flip(right), (dx + 8, dy + 4))
        out.paste(flip(back), (dx + 12, dy + 4))
    mirror_limb(0, 16, 16, 48)   # leg
    mirror_limb(40, 16, 32, 48)  # arm
    return out


def is_slim(img):
    return all(img.getpixel((54, y))[3] == 0 and img.getpixel((55, y))[3] == 0 for y in range(20, 32))


def main():
    src = sys.argv[1]
    chosen = json.load(open(os.path.join(ROOT, "tools/skins_chosen.json")))
    slim = []
    for key, fn in sorted(chosen.items()):
        if key.startswith("_"):
            continue
        img = Image.open(os.path.join(src, fn)).convert("RGBA")
        if img.size == (64, 32):
            img = legacy_to_modern(img)
        assert img.size == (64, 64), (fn, img.size)
        if is_slim(img):
            slim.append(key)
        img.save(os.path.join(TEX, key + ".png"))
        print(f"{key:16s} <- {fn} {'slim' if key in slim else 'wide'}")
    code = open(RENDERER).read()
    names = ", ".join(f'"{k}"' for k in slim if not k.startswith("townsfolk"))
    code = re.sub(r"SLIM = Set\.of\([^)]*\);", f"SLIM = Set.of({names});", code)
    open(RENDERER, "w").write(code)
    print("slim:", names)


if __name__ == "__main__":
    main()
