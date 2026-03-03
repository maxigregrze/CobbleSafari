# Single-Model Multi-Block Approach — Forge 1.21.1

Instead of maintaining two separate BlockBench models for a tall block, you can build **one full model** and reference it from both block halves using lightweight wrapper JSONs. This way, all geometry lives in a single source file and any shape edits only need to be made once.

---

## Overview

The strategy is:

1. Build the **complete tall model** in BlockBench (spanning 2 block heights)
2. Export it as a single model JSON (`yourblock_full.json`)
3. Create two minimal wrapper JSONs — one per half — that reference the full model as a `parent`
4. Apply a **root transform** on the upper wrapper to shift the geometry down by 1 block so it renders correctly in the upper position

---

## Step 1: Build the Full Model in BlockBench

In BlockBench, the valid element coordinate range is **-16 to +32**, which gives you up to 3 block units of space. For a 2-block-tall model, use:

- **Y = 0** as the floor of the lower half
- **Y = 32** as the ceiling of the upper half

Build your entire model within this space. Export it as:

```
assets/yourmod/models/block/yourblock_full.json
```

Make sure to set `"ambientocclusion": false` at the top level to avoid shading artifacts on geometry that exceeds the 1-block bounds:

```json
{
    "ambientocclusion": false,
    "textures": {
        "0": "yourmod:block/yourtexture",
        "particle": "yourmod:block/yourtexture"
    },
    "elements": [ ... ]
}
```

---

## Step 2: Create the Lower Half Wrapper

This wrapper simply inherits the full model as-is. No transform needed — the lower half sits at the world position where the block was placed, so the geometry aligns naturally.

**`yourblock_lower.json`**
```json
{
    "parent": "yourmod:block/yourblock_full"
}
```

---

## Step 3: Create the Upper Half Wrapper with a Root Transform

The upper block position is 1 block above the lower one. Without any correction, the full model would render with its base at Y+1, pushing the whole model up by one block. To fix this, apply a **root transform** that shifts the geometry down by 1 block unit (`-1` on the Y axis in block space).

Forge 1.21.1 supports `"transform"` at the root of a model JSON for this purpose:

**`yourblock_upper.json`**
```json
{
    "parent": "yourmod:block/yourblock_full",
    "display": {
        "gui": {},
        "ground": {},
        "fixed": {},
        "thirdperson_righthand": {},
        "firstperson_righthand": {},
        "firstperson_lefthand": {}
    },
    "transform": {
        "origin": "corner",
        "translation": [0, -1, 0]
    }
}
```

The `"translation": [0, -1, 0]` shifts the rendered geometry down by 1 block in the Y direction, so the upper half of the model appears correctly in the upper block position.

> **Note:** The `"display"` block can be left empty as above. It is only needed if you want to customize how the block looks in hand or in the GUI — for world rendering it has no effect.

---

## Step 4: Blockstate JSON

Reference the two wrapper models in your blockstate file as usual:

```json
{
    "variants": {
        "half=lower": { "model": "yourmod:block/yourblock_lower" },
        "half=upper": { "model": "yourmod:block/yourblock_upper" }
    }
}
```

---

## Step 5: File Structure Summary

```
assets/yourmod/
├── blockstates/
│   └── yourblock.json              ← references lower + upper models
└── models/
    └── block/
        ├── yourblock_full.json     ← the one BlockBench model you maintain
        ├── yourblock_lower.json    ← wrapper: parent = full, no transform
        └── yourblock_upper.json   ← wrapper: parent = full, Y -1 transform
```

---

## What This Means in Practice

| File | Maintained by | Purpose |
|---|---|---|
| `yourblock_full.json` | You (BlockBench) | All geometry and textures live here |
| `yourblock_lower.json` | Hand-written, ~3 lines | Passes the model through unchanged |
| `yourblock_upper.json` | Hand-written, ~10 lines | Shifts the model down by 1 block |

When you want to change the shape of your block, you **only edit `yourblock_full.json`** in BlockBench. The two wrappers never need to change.

---

## Caveats

- **Ambient occlusion** should be disabled (`"ambientocclusion": false`) in `yourblock_full.json`, since elements extend beyond normal block bounds.
- **VoxelShapes** in your Java block class still need to be defined per-half — this approach only affects rendering, not collision or interaction.
- **The `transform` field** is a Forge extension and will not work on Fabric without a compatibility layer or equivalent approach.
- If your model has geometry that should not be visible from the other half's perspective (e.g. internal faces between the two halves), consider removing those faces in BlockBench to avoid overdraw.

---

## Comparison: Single Model vs Two Models

| | Two Separate Models | Single Model + Wrappers |
|---|---|---|
| BlockBench files to maintain | 2 | 1 |
| JSON files total | 2 | 3 (but 2 are trivial) |
| Risk of halves going out of sync | Yes | No |
| Complexity | Low | Low |
| Works on Fabric | Yes | Partially (transform is Forge-specific) |

---

## Application in CobbleSafari (multi-loader)

The project targets both Fabric and NeoForge. The **root transform** for the upper half is a NeoForge extension, so **double-block (half=lower/upper) refactors were not applied** to avoid Fabric-incompatible assets.

Instead, two patterns were used for blocks whose model or hitbox extends beyond one block:

### 1. Single geometry + texture wrappers (underground_pc)

The six underground_pc variants (empty, regular, bronze, silver, gold, platinum) shared identical geometry and only differed by texture. A single source of geometry was introduced:

- **`underground_pc_full.json`** – Contains all elements and `"ambientocclusion": false`. One BlockBench export to maintain.
- **`underground_pc_empty.json`**, **`underground_pc_regular.json`**, etc. – Minimal wrappers with `"parent": "cobblesafari:block/underground_pc_full"` and only a **textures** override.

Any future geometry change is done only in `underground_pc_full.json`; the six wrappers stay as thin texture overrides.

### 2. Ambient occlusion disabled (underground_secret, magnetic_*, timber large)

For blocks that are **one block in the world** but whose model extends beyond 0–16 (e.g. underground_secret Y 0–32, magnetic_crystal/cluster, underground_timber_vertical_large):

- **`"ambientocclusion": false`** was added at the root of the model JSON to avoid shading artifacts on geometry outside the normal block bounds.

No new wrapper or double-block logic was added; these blocks remain single-block with one model each.
