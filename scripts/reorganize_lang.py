#!/usr/bin/env python3
"""
Reorganize a Minecraft lang JSON file by category, preserving the existing order.

A key's *category* is its first two dot-separated segments, e.g.
  "block.cobblesafari.meteorite"      -> "block.cobblesafari"
  "cobblesafari.command.safari.help"  -> "cobblesafari.command"

The script groups every entry under the category block where that category
FIRST appears in the current file. This means:
  - Categories are emitted in their current first-appearance order.
  - Within a category, keys keep their current relative order.
  - A key whose category already exists earlier (e.g. a new "block.*" line you
    pasted at the very bottom) is moved up to the END of that category's block.
  - A key introducing a brand-new category stays after the existing ones, in the
    order those new categories first appear.

Every key/value line is kept byte-for-byte identical. Only the line order and
the trailing comma (required to keep the JSON valid) are adjusted. A blank line
is inserted between each category block. The opening "{" and closing "}" lines
are preserved as-is.

Usage:
  python reorganize_lang.py [files...] [--depth N] [--check]

  files     One or more lang .json files. If omitted, defaults to every .json
            in common/src/main/resources/assets/cobblesafari/lang relative to
            this script.
  --depth N Number of leading dot-segments that define a category (default: 2).
  --check   Don't write; just report which files would change (exit 1 if any).
"""

import re
import sys
from collections import OrderedDict
from pathlib import Path

KEY_RE = re.compile(r'^\s*"((?:[^"\\]|\\.)*)"\s*:')


def category_of(key: str, depth: int) -> str:
    return ".".join(key.split(".")[:depth])


def reorganize(text: str, depth: int) -> str:
    lines = text.splitlines(keepends=True)

    open_idx = next(i for i, ln in enumerate(lines) if ln.strip() == "{")
    close_idx = max(i for i, ln in enumerate(lines) if ln.strip() == "}")

    preamble = lines[: open_idx + 1]
    postamble = lines[close_idx:]
    body = lines[open_idx + 1 : close_idx]

    eol = "\r\n" if lines[open_idx].endswith("\r\n") else "\n"

    # Group entry "cores" (line text minus trailing whitespace/comma) by category,
    # keeping first-appearance order of categories and current order within each.
    groups: "OrderedDict[str, list[str]]" = OrderedDict()
    for ln in body:
        if not ln.strip():
            continue  # drop blank separator lines
        m = KEY_RE.match(ln)
        if not m:
            raise ValueError(f"Unrecognized (non key/value) line: {ln!r}")
        core = ln.rstrip()
        if core.endswith(","):
            core = core[:-1]
        groups.setdefault(category_of(m.group(1), depth), []).append(core)

    all_cores = [core for cores in groups.values() for core in cores]

    out = list(preamble)
    last = len(all_cores) - 1
    idx = 0
    for cat_idx, cores in enumerate(groups.values()):
        if cat_idx > 0:
            out.append(eol)
        for core in cores:
            out.append(core + ("" if idx == last else ",") + eol)
            idx += 1
    out.extend(postamble)
    return "".join(out)


def default_files() -> "list[Path]":
    lang_dir = (
        Path(__file__).resolve().parent
        / ".."
        / "common"
        / "src"
        / "main"
        / "resources"
        / "assets"
        / "cobblesafari"
        / "lang"
    ).resolve()
    return sorted(lang_dir.glob("*.json"))


def main(argv: "list[str]") -> int:
    depth = 2
    check = False
    paths: "list[Path]" = []

    args = list(argv)
    while args:
        arg = args.pop(0)
        if arg == "--depth":
            depth = int(args.pop(0))
        elif arg == "--check":
            check = True
        elif arg in ("-h", "--help"):
            print(__doc__)
            return 0
        else:
            paths.append(Path(arg))

    if not paths:
        paths = default_files()
    if not paths:
        print("No lang files found.")
        return 1

    changed = False
    for path in paths:
        original = path.read_text(encoding="utf-8", newline="")
        result = reorganize(original, depth)
        if result == original:
            print(f"  unchanged: {path}")
            continue
        changed = True
        if check:
            print(f"  WOULD REORDER: {path}")
        else:
            path.write_text(result, encoding="utf-8", newline="")
            print(f"  reorganized: {path}")

    if check and changed:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
