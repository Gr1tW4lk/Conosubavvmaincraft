#!/usr/bin/env python3
"""Static checks for dialogue scripts: JSON validity, labels, actions, locations, items, dialogue ids."""
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DLG = os.path.join(ROOT, "src/main/resources/konocraft/dialogues")
JAVA = os.path.join(ROOT, "src/main/java")

errors = []


def java_source():
    out = ""
    for f in glob.glob(JAVA + "/**/*.java", recursive=True):
        out += open(f, encoding="utf-8").read()
    return out


src = java_source()
actions = set(re.findall(r'case "([a-z_]+)" ->', open(os.path.join(JAVA, "dev/konocraft/story/Actions.java"), encoding="utf-8").read()))
locations = set(re.findall(r'(?:add|ground)\("([a-z_]+)"', open(os.path.join(JAVA, "dev/konocraft/story/Locations.java"), encoding="utf-8").read()))
items = set(re.findall(r'register\("([a-z_]+)"', open(os.path.join(JAVA, "dev/konocraft/registry/ModItems.java"), encoding="utf-8").read()))
items |= set(re.findall(r'KonoCraft.id\("([a-z_]+)"\)', open(os.path.join(JAVA, "dev/konocraft/registry/ModItems.java"), encoding="utf-8").read()))
skills = set(re.findall(r'^\t([A-Z_]+)\(', open(os.path.join(JAVA, "dev/konocraft/skill/Skill.java"), encoding="utf-8").read(), re.M))
speakers = set(re.findall(r'Map.entry\("([a-z_]+)"', open(os.path.join(JAVA, "dev/konocraft/story/Speakers.java"), encoding="utf-8").read()))
speakers |= {"player"}
sounds = set(re.findall(r'register(?:Reference)?\("([a-z_.]+)"\)', open(os.path.join(JAVA, "dev/konocraft/registry/ModSounds.java"), encoding="utf-8").read()))

files = [os.path.basename(f)[:-5] for f in glob.glob(DLG + "/*.json")]
declared = re.findall(r'"([a-z0-9_]+)"', re.search(r'FILES = \{([^}]*)\}', src).group(1))
for f in declared:
    if f not in files:
        errors.append(f"dialogue file {f}.json missing")
for f in files:
    if f not in declared:
        errors.append(f"dialogue file {f}.json is never loaded (add it to Dialogues.FILES)")

dialogues = {}
for f in glob.glob(DLG + "/*.json"):
    try:
        data = json.load(open(f, encoding="utf-8"))
    except Exception as e:
        errors.append(f"{f}: {e}")
        continue
    for k, v in data.items():
        if k in dialogues:
            errors.append(f"duplicate dialogue {k}")
        dialogues[k] = v

KNOWN_FX = {"explosion", "explosion_double", "explosion_small", "magic_circle", "sparkle", "water", "heal", "holy", "curse", "flash", "shake",
            "smoke", "barrier_break", "teleport", "hearts", "tears", "confetti"}


def check_target(spec, where):
    if spec.startswith("@") or spec.startswith("ahead") or spec == "":
        return
    if spec in locations:
        return
    if len(spec.split()) == 3:
        return
    errors.append(f"{where}: unknown target '{spec}'")


for did, lines in dialogues.items():
    labels = {l["id"] for l in lines if "id" in l}
    for i, l in enumerate(lines):
        where = f"{did}[{i}]"
        for key in l:
            if key not in {"s", "n", "t", "c", "do", "id", "goto", "end", "auto", "if"}:
                errors.append(f"{where}: unknown key {key}")
        if "s" in l and l["s"] not in speakers:
            errors.append(f"{where}: unknown speaker {l['s']}")
        if "goto" in l and l["goto"] not in labels:
            errors.append(f"{where}: unknown goto {l['goto']}")
        for c in l.get("c", []):
            if len(c) > 1 and c[1] not in labels and c[1] != "end":
                errors.append(f"{where}: unknown choice label {c[1]}")
        acts = l.get("do", [])
        if isinstance(acts, str):
            acts = [acts]
        for a in acts:
            parts = a.split(" ", 1)
            cmd, arg = parts[0], parts[1] if len(parts) > 1 else ""
            if cmd not in actions:
                errors.append(f"{where}: unknown action {cmd}")
            if cmd in ("tp", "tp_alone", "party_tp"):
                check_target(arg, where)
            if cmd == "npc":
                check_target(arg.split()[1], where)
            if cmd in ("give", "take"):
                it = arg.split()[0]
                if ":" not in it and it not in items:
                    errors.append(f"{where}: unknown item {it}")
            if cmd == "learn" and arg.upper() not in skills:
                errors.append(f"{where}: unknown skill {arg}")
            if cmd == "fx":
                t = arg.split(" ", 1)
                if t[0] not in KNOWN_FX:
                    errors.append(f"{where}: unknown fx {t[0]}")
                if len(t) > 1:
                    check_target(t[1], where)
            if cmd in ("target", "look"):
                check_target(arg, where)
            if cmd == "megumin_explosion":
                spec = arg.rsplit(" ", 1)[0] if re.search(r" \d+$", arg) else arg
                check_target(spec, where)
            if cmd == "dialogue" and arg not in dialogues:
                errors.append(f"{where}: unknown dialogue {arg}")
            if cmd == "sound" and arg.split()[0] not in sounds:
                errors.append(f"{where}: unknown sound {arg}")
            if cmd == "shop" and arg not in set(re.findall(r'"([a-z_]+)", new Shop\(', open(os.path.join(JAVA, "dev/konocraft/story/Shops.java"), encoding="utf-8").read())):
                errors.append(f"{where}: unknown shop {arg}")

# dialogue ids referenced from Java
for ref in set(re.findall(r'(?:start\([a-zA-Z]+, |yield |-> )"([a-z0-9_]+)"', src)):
    if ref in dialogues:
        continue
    if ref.startswith("town_"):
        continue
    # only report things that look like dialogue ids used with DialogueManager/Talk
    if re.search(r'DialogueManager\.start\([^)]*"' + ref + '"', src) or re.search(r'"' + ref + r'";?\s*$', src, re.M):
        if ref not in ("boss", "destroyer", "battle", "ending", "beldia", "townsfolk"):
            errors.append(f"java references missing dialogue {ref}")
for i in range(1, 15):
    if f"town_{i}" not in dialogues:
        errors.append(f"missing town_{i}")
for ref in re.findall(r'DialogueManager\.start\([^,]+,\s*"([a-z0-9_]+)"', src):
    if ref not in dialogues:
        errors.append(f"DialogueManager.start missing dialogue {ref}")
talk = open(os.path.join(JAVA, "dev/konocraft/story/Talk.java"), encoding="utf-8").read()
for ref in re.findall(r'"([a-z0-9_]+)"', talk):
    if ref in ("eris", "kazuma", "luna", "aqua", "megumin", "darkness", "wiz", "vanir", "yunyun", "beldia", "townsfolk", "kazuma_alive",
               "met_wiz", "met_vanir", "met_yunyun", "town_"):
        continue
    if ref not in dialogues:
        errors.append(f"Talk references missing dialogue {ref}")

print(f"{len(dialogues)} dialogues, {sum(len(v) for v in dialogues.values())} lines")
for e in errors:
    print("ERROR", e)
sys.exit(1 if errors else 0)
