#!/usr/bin/env python3
"""
Scans data/cobblesafari/rotomphone_skins for JSON files, collects each "id" value,
and ensures rotomphone_variant_<id>.json and rotomphone_variant_<id>_s.json exist
under assets/cobblesafari/models/item. Missing files are created; existing files
are not modified.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = REPO_ROOT / "common/src/main/resources/data/cobblesafari/rotomphone_skins"
MODELS_DIR = REPO_ROOT / "common/src/main/resources/assets/cobblesafari/models/item"
PARENT = "cobblesafari:item/rotomphone_item"


def texture_path(skin_id: str, shiny: bool) -> str:
    base = f"cobblesafari:item/rotomphone/rotomphone_item_skin_{skin_id}"
    return f"{base}-s" if shiny else base


def make_model(skin_id: str, shiny: bool) -> dict:
    t = texture_path(skin_id, shiny)
    return {
        "parent": PARENT,
        "textures": {
            "0": t,
            "particle": t,
        },
    }


def load_skin_ids() -> list[str]:
    if not DATA_DIR.is_dir():
        print(f"Missing data directory: {DATA_DIR}", file=sys.stderr)
        sys.exit(1)

    ids: list[str] = []
    for path in sorted(DATA_DIR.glob("*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            print(f"Skip {path.name}: {e}", file=sys.stderr)
            continue
        if "id" not in data:
            print(f"Skip {path.name}: no 'id' field", file=sys.stderr)
            continue
        raw = data["id"]
        if not isinstance(raw, str) or not raw.strip():
            print(f"Skip {path.name}: invalid 'id'", file=sys.stderr)
            continue
        ids.append(raw.strip())
    return ids


def main() -> None:
    MODELS_DIR.mkdir(parents=True, exist_ok=True)
    skin_ids = load_skin_ids()
    if not skin_ids:
        print("No skin ids found.", file=sys.stderr)
        sys.exit(1)

    created = 0
    existed = 0
    for skin_id in skin_ids:
        for shiny, suffix in ((False, ""), (True, "_s")):
            filename = f"rotomphone_variant_{skin_id}{suffix}.json"
            target = MODELS_DIR / filename
            if target.is_file():
                print(f"exists: {filename}")
                existed += 1
                continue
            text = json.dumps(
                make_model(skin_id, shiny),
                indent=2,
                ensure_ascii=False,
            )
            target.write_text(text + "\n", encoding="utf-8")
            print(f"create: {filename}")
            created += 1

    print(f"Done. Created {created} file(s), {existed} already present.")


if __name__ == "__main__":
    main()
