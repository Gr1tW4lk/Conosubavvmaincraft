#!/usr/bin/env python3
"""Collects candidate fan skins for every character and renders preview sheets, so the
best-looking ones can be picked by eye. Runs in CI (the build machines have open internet):

    python3 tools/fetch_skins.py OUT_DIR

Sources:
  * Mojang's public profile API: players named after a character very often wear that
    character's skin, and the API serves the exact skin file.
  * minecraft.how skin pages (ids found by web search) and its search pages.
"""
import base64
import hashlib
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
DEADLINE = time.time() + 22 * 60
DUMP_DIR = None

DECOR = ["{}", "{}_", "_{}", "x{}x", "{}1", "{}2", "{}7", "{}X", "{}UwU", "{}_UwU", "{}Chan", "{}_Chan",
         "{}Sama", "The{}", "i{}", "{}__", "{}MC", "{}_MC", "{}Konosuba", "{}_Konosuba", "Konosuba{}"]

NAMES = {
    "megumin": ["Megumin", "MeguminExplosion", "Megumin_Explosion", "ExplosionMegumin", "Explosion_Megumin",
                "CrimsonMegumin", "BakuretsuMegumin", "Meguminn", "Megu_min", "Megumin_Bakuretsu"],
    "aqua": ["Aqua", "AquaSama", "Aqua_Sama", "GoddessAqua", "Goddess_Aqua", "AquaGoddess", "Aqua_Goddess",
             "AquaKonosuba", "UselessGoddess", "Useless_Goddess", "AquaTheGoddess", "Aquaa"],
    "kazuma": ["Kazuma", "KazumaSatou", "Kazuma_Satou", "SatouKazuma", "Satou_Kazuma", "KazumaSato", "Kazuma_Sato",
               "Kazumaa", "KazumaKun", "Kazuma_Kun", "Kazuma_Steal", "KazumaStealer"],
    "eris": ["Eris", "GoddessEris", "Goddess_Eris", "ErisSama", "Eris_Sama", "ErisKonosuba", "Eris_Konosuba", "Erisu",
             "ErisPad", "ErisGoddess"],
    "vanir": ["Vanir", "VanirKonosuba", "Vanir_Konosuba", "DukeVanir", "Duke_Vanir", "VanirMask", "Vanir_",
              "VanirSama", "Vanirr", "VanirDevil"],
    "darkness": ["Darkness", "Lalatina", "Darkness_Lalatina", "LalatinaDarkness", "DarknessDustiness",
                 "Dustiness_Ford", "Darkness_Crusader", "CrusaderLalatina", "Lalatina_Dustiness", "Lalatina1",
                 "Lalatinaa", "Darkness_Kono", "DarknessKono", "DarknessSama", "MasochistKnight", "Daskunesu",
                 "LalatinaChan", "Lalatina_Chan", "Dustiness", "DustinessFord", "LalatinaFord", "Lalatina_Ford",
                 "DarknessKnight", "Darkness_Knight"],
    "wiz": ["Wiz", "WizSama", "Wiz_chan", "LichQueen", "Lich_Queen", "Wiz_the_Lich", "WizTheLich", "IceWitch",
            "Ice_Witch", "IceWitchWiz", "PoorWiz", "Wiz_Shopkeeper", "WizMagicShop", "Wizu", "Uiz", "Uizu",
            "WizKonosuba", "Wiz_Konosuba", "WizLich", "Wiz_Lich", "LichWiz", "Lich_Wiz", "WizShop", "Wiz_Shop",
            "WizUwU", "Wiz_UwU", "WizChan", "Wiz__", "WizKono", "Wiz_Kono", "BrokeWiz", "LichShop"],
    "yunyun": ["Yunyun", "Yunyun_Crimson", "YunyunBestGirl", "yun_yun_", "Yunyun1", "Yunyunnn", "Yunyun_uwu",
               "Yunyun2", "YunyunSama", "Yuunyun", "Yun_Yun_", "Yunyun_Megumin", "YunyunRival", "CrimsonYunyun",
               "Yunyun_Kono", "Yunyun_MC", "YunyunMC", "xYunyunx", "Yunyunx", "Yunyun777", "YunyunKonosuba",
               "Yunyun_Konosuba", "YunyunChan", "Yunyun_Chan", "Yunyun_", "_Yunyun", "YunyunUwU", "Yunyun3",
               "Yunyun7", "LonelyYunyun", "Yunyun_Lonely", "BocchiYunyun"],
    "luna": ["LunaKonosuba", "Luna_Konosuba", "LunaGuild", "Luna_Guild", "GuildLuna", "Luna_Receptionist",
             "Receptionist", "GuildGirl", "Guild_Girl", "Waitress", "Barmaid", "TavernGirl", "Tavern_Girl",
             "Luna_Axel", "LunaAxel", "GuildMaster", "Guild_Receptionist", "Maid", "MedievalMaid", "Bargirl"],
    "beldia": ["Beldia", "Verdia", "BlackKnight", "Black_Knight", "DarkKnight", "Dark_Knight", "Knight",
               "ArmoredKnight", "DemonKnight", "Demon_Knight", "DullahanKnight", "Dullahan", "Dullahan_",
               "HeadlessKnight", "Headless_Knight", "CursedKnight", "Cursed_Knight", "ShadowKnight", "AbyssKnight",
               "Knight_", "TheKnight", "DarkPaladin", "Dark_Paladin", "ChaosKnight"],
    "undead_knight": ["UndeadKnight", "Undead_Knight", "SkeletonKnight", "Skeleton_Knight", "DeathKnight",
                      "SkeletonWarrior", "Skeleton_Warrior", "Revenant", "UndeadSoldier", "SkeletonSoldier",
                      "ZombieKnight", "Zombie_Knight", "Wight", "Draugr", "UndeadWarrior", "SkeletonKing",
                      "Skeleton_King", "Lich", "Ghoul", "BoneKnight"],
    "goblin": ["Goblin", "GoblinKing", "GoblinWarrior", "Goblin_King", "Orc", "Goblin1", "GoblinBoy",
               "TheGoblin", "Goblins", "Hobgoblin", "Goblin_Warrior", "GoblinChief", "Kobold", "GreenGoblin",
               "Goblin_", "Gobbo", "GoblinMC", "Orc_Warrior"],
    "townsfolk": ["MedievalPeasant", "Peasant1", "Villager1", "TownGuard", "Guard", "Nun", "Priest", "Adventurer",
                  "Warrior", "Ranger", "Archer", "Cleric", "Thief", "Rogue", "Paladin", "Swordsman", "Lancer",
                  "Merchant1", "Traveler", "Monk", "Shepherd", "Miller", "Tailor", "Cook", "Chef", "Blacksmith",
                  "Farmer", "Baker", "Innkeeper", "Villager", "Peasant", "Merchant", "Medieval_Villager",
                  "Knight_Guard", "Squire", "Mercenary", "Bard", "Hunter", "Fisherman", "Lumberjack"],
}
DECORATED = {"megumin", "aqua", "darkness", "kazuma", "eris", "wiz", "yunyun", "vanir"}

MCHOW_IDS = {
    "megumin": [234124, 248349],
    "aqua": [353623, 249345, 313174],
    "darkness": [212296],
    "kazuma": [227092],
}
MCHOW_QUERIES = {
    "megumin": ["megumin"], "aqua": ["aqua"], "darkness": ["darkness", "lalatina"], "kazuma": ["kazuma"],
    "eris": ["eris"], "wiz": ["wiz"], "yunyun": ["yunyun"], "vanir": ["vanir"], "beldia": ["dullahan"],
    "luna": ["receptionist"], "chris": ["chris"],
}


def out_of_time():
    return time.time() > DEADLINE


def get(url, binary=False, data=None, headers=None, retries=1):
    for attempt in range(retries + 1):
        try:
            h = {"User-Agent": UA, "Accept": "*/*"}
            if headers:
                h.update(headers)
            req = urllib.request.Request(url, data=data, headers=h)
            with urllib.request.urlopen(req, timeout=15) as r:
                body = r.read()
                return body if binary else body.decode("utf-8", "replace")
        except Exception as e:
            if attempt == retries:
                print("  fail", url, e)
                return None
            time.sleep(1.0)


def dump(name, text):
    """Keeps raw responses so the scraper can be adjusted if a site changes its markup."""
    if DUMP_DIR and text:
        fn = os.path.join(DUMP_DIR, re.sub(r"[^a-z0-9]+", "_", name.lower())[:80] + ".html")
        if not os.path.exists(fn):
            with open(fn, "w") as fh:
                fh.write(text)


def skin_size_ok(data):
    try:
        return Image.open(io.BytesIO(data)).size in ((64, 64), (64, 32))
    except Exception:
        return False


# ---------------------------------------------------------------- Mojang

def mojang_lookup(names):
    found = {}
    for i in range(0, len(names), 10):
        if out_of_time():
            break
        chunk = names[i:i + 10]
        payload = json.dumps(chunk).encode()
        body = get("https://api.minecraftservices.com/minecraft/profile/lookup/bulk/byname",
                   data=payload, headers={"Content-Type": "application/json"})
        if body is None:
            body = get("https://api.mojang.com/profiles/minecraft",
                       data=payload, headers={"Content-Type": "application/json"})
        if body:
            try:
                for p in json.loads(body):
                    found[p["name"]] = p["id"]
            except Exception as e:
                print("  bad lookup response", e, body[:200])
        time.sleep(0.4)
    return found


def mojang_skin(uuid):
    body = get(f"https://sessionserver.mojang.com/session/minecraft/profile/{uuid}")
    if not body:
        return None
    try:
        prof = json.loads(body)
        tex = json.loads(base64.b64decode(prof["properties"][0]["value"]))
        skin = tex["textures"].get("SKIN")
        if not skin:
            return None
        url = skin["url"].replace("http://", "https://")
        data = get(url, binary=True)
        if data and skin_size_ok(data):
            return data, url
    except Exception as e:
        print("  bad profile", uuid, e)
    return None


def mojang(character):
    base = NAMES.get(character, [])
    names = list(base)
    if character in DECORATED:
        names += [d.format(base[0]) for d in DECOR]
    seen, uniq = set(), []
    for n in names:
        if 3 <= len(n) <= 16 and re.fullmatch(r"[A-Za-z0-9_]+", n) and n.lower() not in seen:
            seen.add(n.lower())
            uniq.append(n)
    out = []
    for name, uuid in mojang_lookup(uniq).items():
        if out_of_time():
            break
        res = mojang_skin(uuid)
        time.sleep(0.25)
        if res:
            out.append((f"player-{name}", res[0], f"https://namemc.com/profile/{name}"))
    return out


# ---------------------------------------------------------------- minecraft.how

def mchow_page(sid):
    html = get(f"https://minecraft.how/skin/{sid}")
    if not html:
        return None
    dump(f"mchow skin {sid}", html)
    urls = re.findall(r'(https?://[^"\'\s<>]+?\.png)', html)
    urls += ["https://minecraft.how" + u for u in re.findall(r'["\'](/[^"\'\s<>]+?\.png)', html)]
    urls += [f"https://minecraft.how/skin/{sid}/download", f"https://minecraft.how/download/skin/{sid}"]
    for u in urls:
        data = get(u, binary=True, retries=0)
        if data and skin_size_ok(data):
            return data, u
    return None


def mchow(character):
    out = []
    ids = list(MCHOW_IDS.get(character, []))
    for q in MCHOW_QUERIES.get(character, []):
        for pattern in ("https://minecraft.how/skins?search={}", "https://minecraft.how/skins/search/{}",
                        "https://minecraft.how/search?q={}"):
            if out_of_time():
                break
            html = get(pattern.format(urllib.parse.quote(q)), retries=0)
            if html:
                dump("mchow search " + pattern.split("/")[-1] + " " + q, html)
                for m in re.finditer(r'/skin/(\d+)', html):
                    sid = int(m.group(1))
                    if sid not in ids:
                        ids.append(sid)
    for sid in ids[:12]:
        if out_of_time():
            break
        res = mchow_page(sid)
        if res:
            out.append((f"mchow-{sid}", res[0], f"https://minecraft.how/skin/{sid}"))
    return out


# ---------------------------------------------------------------- other galleries

WEB_QUERIES = {
    "yunyun": ["yunyun", "yun yun", "yunyun konosuba", "konosuba"],
    "luna": ["luna konosuba", "konosuba", "guild receptionist", "receptionist"],
}
WEB_SEARCH = [
    "https://api.mineskin.org/v2/skins?size=64&filter={q}",
    "https://api.mineskin.org/get/list/1?size=64&filter={q}",
    "https://www.minecraftskins.net/search?q={q}",
    "https://minecraft.novaskin.me/search?q={q}",
    "https://www.planetminecraft.com/skins/?keywords={q}",
    "https://mskins.net/en/search?query={q}",
    "https://minecraft-inside.ru/skins/search/?q={q}",
    "https://tlauncher.org/ru/catalog/skin/?q={q}",
    "https://www.skinsmc.org/en/search?q={q}",
]


def page_images(base, html):
    urls = set()
    for m in re.finditer(r'https?://textures\.minecraft\.net/texture/[0-9a-f]+', html):
        urls.add(m.group(0).replace("http://", "https://"))
    for m in re.finditer(r'(?:src|href|data-src|data-skin|content)=["\']([^"\'\s<>]+?\.png[^"\'\s<>]*)["\']', html):
        urls.add(urllib.parse.urljoin(base, m.group(1).replace("&amp;", "&")))
    for m in re.finditer(r'(https?:\\?/\\?/[^"\'\s<>]+?\.png)', html):
        urls.add(m.group(1).replace("\\/", "/"))
    return list(urls)


def web(character):
    out = []
    for q in WEB_QUERIES.get(character, []):
        for pattern in WEB_SEARCH:
            if out_of_time():
                return out
            url = pattern.format(q=urllib.parse.quote(q))
            html = get(url, retries=0)
            if not html:
                continue
            host = urllib.parse.urlparse(url).netloc
            dump(f"web {host} {q}", html)
            imgs = page_images(url, html)
            print(f"  {host} '{q}': {len(html)} bytes, {len(imgs)} images", flush=True)
            for u in imgs[:40]:
                if out_of_time():
                    return out
                data = get(u, binary=True, retries=0)
                if data and skin_size_ok(data):
                    out.append((f"web-{host}-{len(out)}", data, url))
    return out


# ---------------------------------------------------------------- MineSkin gallery

MINESKIN_QUERIES = {
    "megumin": ["megumin"], "aqua": ["aqua"], "darkness": ["darkness", "lalatina", "crusader"],
    "kazuma": ["kazuma"], "eris": ["eris"], "wiz": ["wiz", "lich"], "vanir": ["vanir"],
    "yunyun": ["yunyun", "yun yun"], "luna": ["luna", "receptionist", "guild"],
    "beldia": ["beldia", "dullahan", "headless", "black knight"], "konosuba": ["konosuba"],
    "goblin": ["goblin"], "undead_knight": ["skeleton knight", "undead knight"],
}


def mineskin(character):
    out = []
    for q in MINESKIN_QUERIES.get(character, []):
        if out_of_time():
            break
        body = get("https://api.mineskin.org/v2/skins?size=64&filter=" + urllib.parse.quote(q))
        try:
            skins = json.loads(body).get("skins", []) if body else []
        except Exception:
            skins = []
        print(f"  mineskin '{q}': {len(skins)} skins", flush=True)
        for sk in skins[:48]:
            if out_of_time():
                break
            tex = sk.get("texture")
            if not tex:
                continue
            data = get("https://textures.minecraft.net/texture/" + tex, binary=True, retries=0)
            if data and skin_size_ok(data):
                name = re.sub(r"[^A-Za-z0-9_]+", "_", sk.get("name") or "")[:18]
                out.append((f"mineskin-{sk.get('shortId') or sk['uuid'][:8]}-{name}", data, "https://mineskin.org/" + sk["uuid"]))
    return out


# ---------------------------------------------------------------- previews

def to_modern(img):
    img = img.convert("RGBA")
    if img.size == (64, 32):
        full = Image.new("RGBA", (64, 64))
        full.paste(img, (0, 0))
        full.paste(img.crop((0, 16, 16, 32)), (16, 48))
        full.paste(img.crop((40, 16, 56, 32)), (32, 48))
        img = full
    return img


def is_slim(img):
    img = to_modern(img)
    return all(img.getpixel((54, y))[3] == 0 and img.getpixel((55, y))[3] == 0 for y in range(20, 32))


def front(img):
    """Front view of a skin as a 16x32 figure (base + overlay layers)."""
    img = to_modern(img)
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
    img = to_modern(img)
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
    only = sys.argv[2].split(",") if len(sys.argv) > 2 else list(NAMES)
    for character in only:
        print("==", character, flush=True)
        found, digests = [], set()
        sources = (mchow, mojang)
        if len(sys.argv) > 3:
            sources = tuple(globals()[n] for n in sys.argv[3].split(","))
        elif len(sys.argv) > 2:
            sources = (mojang,)
        for source in sources:
            if out_of_time():
                print("  out of time")
                break
            try:
                for item in source(character):
                    h = hashlib.sha1(item[1]).hexdigest()
                    if h not in digests:
                        digests.add(h)
                        found.append(item)
            except Exception as e:
                print("  source failed", source.__name__, e)
        found = found[:96]
        index[character] = []
        for i, (name, data, page) in enumerate(found):
            fn = f"{character}_{i:02d}.png"
            with open(os.path.join(out_dir, "png", fn), "wb") as fh:
                fh.write(data)
            sk = Image.open(io.BytesIO(data))
            index[character].append({"file": fn, "name": name, "page": page, "slim": front(sk)[1]})
        sheet(character, found, out_dir)
        print("  found", len(found), flush=True)
    with open(os.path.join(out_dir, "index.json"), "w") as fh:
        json.dump(index, fh, indent=2)


if __name__ == "__main__":
    main()
