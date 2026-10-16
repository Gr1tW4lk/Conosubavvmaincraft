#!/usr/bin/env python3
"""Downloads candidate fan skins for every character from public skin galleries and renders
preview sheets, so the best-looking ones can be picked by eye. Runs in CI (the build machines
have open internet access): python3 tools/fetch_skins.py OUT_DIR
"""
import io
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

from PIL import Image, ImageDraw

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"

QUERIES = {
    "megumin": ["megumin", "megumin konosuba"],
    "aqua": ["aqua konosuba", "aqua goddess"],
    "darkness": ["darkness konosuba", "lalatina"],
    "kazuma": ["kazuma", "kazuma konosuba"],
    "eris": ["eris konosuba", "goddess eris"],
    "wiz": ["wiz konosuba", "wiz lich"],
    "yunyun": ["yunyun", "yunyun konosuba"],
    "vanir": ["vanir", "vanir konosuba"],
    "luna": ["luna konosuba", "guild receptionist"],
    "chris": ["chris konosuba"],
    "beldia": ["dullahan", "headless knight"],
    "goblin": ["goblin"],
    "undead_knight": ["undead knight", "skeleton knight"],
    "townsfolk": ["villager medieval", "medieval peasant", "medieval merchant", "fantasy villager"],
}
PER_QUERY = 8
DUMP_DIR = None


def dump(name, html):
    """Keeps the raw page so the scraper can be fixed if a site changes its markup."""
    if DUMP_DIR and html:
        fn = os.path.join(DUMP_DIR, re.sub(r"[^a-z0-9]+", "_", name.lower())[:80] + ".html")
        if not os.path.exists(fn):
            with open(fn, "w") as fh:
                fh.write(html)


def get(url, binary=False, retries=2):
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
            with urllib.request.urlopen(req, timeout=25) as r:
                data = r.read()
                return data if binary else data.decode("utf-8", "replace")
        except Exception as e:
            if attempt == retries:
                print("  fail", url, e)
                return None
            time.sleep(1.5)


def is_skin(data):
    try:
        img = Image.open(io.BytesIO(data))
        return img.size in ((64, 64), (64, 32))
    except Exception:
        return False


def skindex(query):
    q = urllib.parse.quote(query.replace(" ", "-"))
    html = get(f"https://www.minecraftskins.com/search/skin/{q}/1/")
    if not html:
        return []
    dump("skindex " + query, html)
    ids = []
    for m in re.finditer(r'href="/skin/(\d+)/([^/"]+)/?"', html):
        if m.group(1) not in [i for i, _ in ids]:
            ids.append((m.group(1), m.group(2)))
    out = []
    for sid, slug in ids[:PER_QUERY]:
        data = get(f"https://www.minecraftskins.com/skin/download/{sid}", binary=True)
        if data and is_skin(data):
            out.append((f"skindex-{sid}-{slug}", data, f"https://www.minecraftskins.com/skin/{sid}/{slug}/"))
    return out


def namemc(query):
    q = urllib.parse.quote(query.replace(" ", "-").lower())
    html = get(f"https://namemc.com/minecraft-skins/tag/{q}")
    if not html:
        html = get(f"https://namemc.com/minecraft-skins/search?q={urllib.parse.quote(query)}")
    if not html:
        return []
    dump("namemc " + query, html)
    out = []
    seen = set()
    for m in re.finditer(r'/skin/([0-9a-f]{16})', html):
        sid = m.group(1)
        if sid in seen:
            continue
        seen.add(sid)
        data = get(f"https://s.namemc.com/i/{sid}.png", binary=True)
        if data and is_skin(data):
            out.append((f"namemc-{sid}", data, f"https://namemc.com/skin/{sid}"))
        if len(out) >= PER_QUERY:
            break
    return out


def is_slim(img):
    img = img.convert("RGBA")
    if img.size != (64, 64):
        return False
    return all(img.getpixel((54, y))[3] == 0 and img.getpixel((55, y))[3] == 0 for y in range(20, 32))


def front(img):
    """Front view of a skin as a 16x32 figure (base + overlay layers)."""
    img = img.convert("RGBA")
    if img.size == (64, 32):
        full = Image.new("RGBA", (64, 64))
        full.paste(img, (0, 0))
        img = full
    slim = is_slim(img)
    aw = 3 if slim else 4
    fig = Image.new("RGBA", (16, 32))

    def paste(x, y, w, h, dx, dy):
        fig.alpha_composite(img.crop((x, y, x + w, y + h)), (dx, dy))
    paste(8, 8, 8, 8, 4, 0)
    paste(40, 8, 8, 8, 4, 0)
    paste(20, 20, 8, 12, 4, 8)
    paste(20, 36, 8, 12, 4, 8)
    paste(44, 20, aw, 12, 4 - aw, 8)
    paste(44, 36, aw, 12, 4 - aw, 8)
    paste(36, 52, aw, 12, 12, 8)
    paste(52, 52, aw, 12, 12, 8)
    paste(4, 20, 4, 12, 4, 20)
    paste(4, 36, 4, 12, 4, 20)
    paste(20, 52, 4, 12, 8, 20)
    paste(4, 52, 4, 12, 8, 20)
    return fig, slim


def back(img):
    img = img.convert("RGBA")
    if img.size == (64, 32):
        full = Image.new("RGBA", (64, 64))
        full.paste(img, (0, 0))
        img = full
    fig = Image.new("RGBA", (16, 32))
    fig.alpha_composite(img.crop((24, 8, 32, 16)), (4, 0))
    fig.alpha_composite(img.crop((56, 8, 64, 16)), (4, 0))
    fig.alpha_composite(img.crop((32, 20, 40, 32)), (4, 8))
    fig.alpha_composite(img.crop((32, 36, 40, 48)), (4, 8))
    fig.alpha_composite(img.crop((12, 20, 16, 32)), (8, 20))
    fig.alpha_composite(img.crop((28, 52, 32, 64)), (4, 20))
    return fig


def sheet(character, skins, out_dir):
    if not skins:
        return
    S = 5
    cell_w, cell_h = 2 * 16 * S + 24, 32 * S + 40
    cols = 6
    rows = (len(skins) + cols - 1) // cols
    img = Image.new("RGBA", (cols * cell_w, rows * cell_h), (70, 86, 104, 255))
    d = ImageDraw.Draw(img)
    for i, (name, data, _) in enumerate(skins):
        sk = Image.open(io.BytesIO(data))
        f, slim = front(sk)
        b = back(sk)
        x, y = (i % cols) * cell_w + 8, (i // cols) * cell_h + 4
        img.alpha_composite(f.resize((16 * S, 32 * S), Image.NEAREST), (x, y))
        img.alpha_composite(b.resize((16 * S, 32 * S), Image.NEAREST), (x + 16 * S + 8, y))
        d.text((x, y + 32 * S + 4), f"#{i} {'slim' if slim else 'wide'}", fill=(255, 255, 0))
        d.text((x, y + 32 * S + 18), name[:34], fill=(255, 255, 255))
    img.save(os.path.join(out_dir, f"sheet_{character}.png"))


def main():
    global DUMP_DIR
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "skins_out"
    os.makedirs(os.path.join(out_dir, "png"), exist_ok=True)
    DUMP_DIR = os.path.join(out_dir, "html")
    os.makedirs(DUMP_DIR, exist_ok=True)
    index = {}
    for character, queries in QUERIES.items():
        print("==", character)
        found = []
        names = set()
        for q in queries:
            for source in (skindex, namemc):
                try:
                    for item in source(q):
                        if item[0] not in names:
                            names.add(item[0])
                            found.append(item)
                except Exception as e:
                    print("  source failed", source.__name__, q, e)
            if len(found) >= 18:
                break
        found = found[:18]
        index[character] = []
        for i, (name, data, page) in enumerate(found):
            fn = f"{character}_{i:02d}.png"
            with open(os.path.join(out_dir, "png", fn), "wb") as fh:
                fh.write(data)
            sk = Image.open(io.BytesIO(data))
            index[character].append({"file": fn, "name": name, "page": page, "slim": front(sk)[1]})
        sheet(character, found, out_dir)
        print("  found", len(found))
    with open(os.path.join(out_dir, "index.json"), "w") as fh:
        json.dump(index, fh, indent=2)


if __name__ == "__main__":
    main()
