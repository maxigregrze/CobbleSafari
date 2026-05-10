#!/usr/bin/env python3
"""
Add 1px transparent padding to all 16x16 PNG files, producing 18x18 outputs.
Usage: python add_padding.py <input_dir> [output_dir]
  - input_dir:  folder containing your 16x16 .png files
  - output_dir: (optional) folder for the 18x18 results
                defaults to <input_dir>/padded/
Used to conver the 600 tooltip "donut power" icons to 18x18 for the mob_effect icons.
"""

import sys
import os
from pathlib import Path
from PIL import Image


def add_padding(input_dir: str, output_dir: str | None = None) -> None:
    input_path = Path(input_dir)
    if not input_path.is_dir():
        print(f"Error: '{input_dir}' is not a valid directory.")
        sys.exit(1)

    output_path = Path(output_dir) if output_dir else input_path / "padded"
    output_path.mkdir(parents=True, exist_ok=True)

    png_files = list(input_path.glob("*.png"))
    if not png_files:
        print(f"No .png files found in '{input_dir}'.")
        sys.exit(0)

    print(f"Found {len(png_files)} PNG file(s). Processing...")

    skipped = 0
    processed = 0

    for png_file in png_files:
        img = Image.open(png_file).convert("RGBA")

        if img.size != (16, 16):
            print(f"  Skipping '{png_file.name}' — size is {img.size}, expected (16, 16).")
            skipped += 1
            continue

        # Create an 18x18 fully transparent canvas
        canvas = Image.new("RGBA", (18, 18), (0, 0, 0, 0))

        # Paste the original image at position (1, 1)
        canvas.paste(img, (1, 1))

        out_file = output_path / png_file.name
        canvas.save(out_file, "PNG")
        processed += 1

    print(f"\nDone! {processed} image(s) padded → '{output_path}'")
    if skipped:
        print(f"       {skipped} image(s) skipped (wrong size).")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    in_dir  = sys.argv[1]
    out_dir = sys.argv[2] if len(sys.argv) >= 3 else None
    add_padding(in_dir, out_dir)
