#!/usr/bin/env python3
"""Look up Yarn 1.20.1 names: classes, fields and methods with readable signatures.

Usage: yarn.py ClassName [filter]      -- members of a class (simple or full name)
       yarn.py -s text                 -- search classes whose name contains text
       yarn.py -m methodName           -- search all classes for a method name
Reads mapping files from ../fabricmc/yarn/mappings (FabricMC/yarn, branch 1.20.1).
"""
import os
import pickle
import re
import sys

ROOT = os.environ.get("YARN_DIR", "/home/user/fabricmc/yarn/mappings")
CACHE = "/tmp/claude-0/yarn_cache.pkl"


def load():
    if os.path.exists(CACHE):
        with open(CACHE, "rb") as f:
            return pickle.load(f)
    inter_to_named = {}
    classes = {}  # named -> {fields:[], methods:[]}
    for dirpath, _, files in os.walk(ROOT):
        for fn in files:
            if not fn.endswith(".mapping"):
                continue
            stack = []
            with open(os.path.join(dirpath, fn), encoding="utf-8") as f:
                for line in f:
                    stripped = line.rstrip("\n")
                    depth = len(stripped) - len(stripped.lstrip("\t"))
                    parts = stripped.strip().split(" ")
                    if not parts or not parts[0]:
                        continue
                    kind = parts[0]
                    if kind == "CLASS":
                        # class depth = nesting level
                        while stack and stack[-1][0] >= depth:
                            stack.pop()
                        inter = parts[1]
                        named = parts[2] if len(parts) > 2 else parts[1]
                        if stack:
                            pinter, pnamed = stack[-1][1], stack[-1][2]
                            inter = pinter + "$" + inter
                            named = pnamed + "$" + named
                        inter_to_named[inter] = named
                        classes.setdefault(named, {"fields": [], "methods": []})
                        stack.append((depth, inter, named))
                    elif kind in ("FIELD", "METHOD") and stack:
                        while stack and stack[-1][0] >= depth:
                            stack.pop()
                        if not stack:
                            continue
                        owner = stack[-1][2]
                        if len(parts) == 4:
                            inter_name, name, desc = parts[1], parts[2], parts[3]
                        else:
                            inter_name, name, desc = parts[1], parts[1], parts[2]
                        key = "fields" if kind == "FIELD" else "methods"
                        classes[owner][key].append((name, desc))
    data = (inter_to_named, classes)
    os.makedirs(os.path.dirname(CACHE), exist_ok=True)
    with open(CACHE, "wb") as f:
        pickle.dump(data, f)
    return data


PRIM = {"V": "void", "Z": "boolean", "B": "byte", "C": "char", "S": "short", "I": "int", "J": "long", "F": "float", "D": "double"}


def type_str(desc, i, names):
    arr = 0
    while desc[i] == "[":
        arr += 1
        i += 1
    c = desc[i]
    if c == "L":
        end = desc.index(";", i)
        cls = desc[i + 1:end]
        cls = names.get(cls, cls)
        s = cls.split("/")[-1]
        i = end + 1
    else:
        s = PRIM[c]
        i += 1
    return s + "[]" * arr, i


def sig(name, desc, names):
    if not desc.startswith("("):
        t, _ = type_str(desc, 0, names)
        return f"{t} {name}"
    i = 1
    args = []
    while desc[i] != ")":
        t, i = type_str(desc, i, names)
        args.append(t)
    ret, _ = type_str(desc, i + 1, names)
    return f"{ret} {name}({', '.join(args)})"


def main():
    names, classes = load()
    if len(sys.argv) < 2:
        print(__doc__)
        return
    if sys.argv[1] == "-s":
        q = sys.argv[2].lower()
        for c in sorted(classes):
            if q in c.lower().split("/")[-1]:
                print(c)
        return
    if sys.argv[1] == "-m":
        q = sys.argv[2]
        for c, d in sorted(classes.items()):
            for n, desc in d["methods"]:
                if re.fullmatch(q, n):
                    print(c.split("/")[-1].ljust(40), sig(n, desc, names))
        return
    q = sys.argv[1]
    flt = sys.argv[2] if len(sys.argv) > 2 else None
    matches = [c for c in classes if c == q or c.split("/")[-1] == q]
    for c in matches:
        print("==", c)
        for n, desc in classes[c]["fields"]:
            s = sig(n, desc, names)
            if not flt or re.search(flt, s, re.I):
                print("  F", s)
        for n, desc in classes[c]["methods"]:
            s = sig(n, desc, names)
            if not flt or re.search(flt, s, re.I):
                print("  M", s)


if __name__ == "__main__":
    main()
