"""
Extract a reference potion illustration into VibeVisuals HUD texture layers.

Input can have a white background (the original reference) or a flat chroma-key
background. The output keeps the original illustration as the frame layer and
turns the pink liquid into a white mask that Minecraft tints with the selected
accent theme.

Run:
  python scripts/extract_potion_icon_from_reference.py path/to/reference.png
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageChops, ImageFilter


SIZE = 128
OUT_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vibevisuals/textures/gui"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("--out-dir", type=Path, default=OUT_DIR)
    parser.add_argument("--pad", type=float, default=0.02)
    parser.add_argument("--min-detail-alpha", type=int, default=36)
    return parser.parse_args()


def estimate_background(image: Image.Image) -> tuple[int, int, int]:
    rgb = image.convert("RGB")
    w, h = rgb.size
    samples = [
        rgb.getpixel((0, 0)),
        rgb.getpixel((w - 1, 0)),
        rgb.getpixel((0, h - 1)),
        rgb.getpixel((w - 1, h - 1)),
    ]
    return tuple(sum(pixel[i] for pixel in samples) // len(samples) for i in range(3))


def remove_flat_background(image: Image.Image) -> Image.Image:
    rgba = image.convert("RGBA")
    bg = estimate_background(rgba)
    rgb = rgba.convert("RGB")
    if bg[1] > 170 and bg[0] < 100 and bg[2] < 100:
        alpha = Image.new("L", rgb.size, 255)
        src = rgb.load()
        dst = alpha.load()
        for y in range(rgb.height):
            for x in range(rgb.width):
                r, g, b = src[x, y]
                green_key = g > 120 and g - r > 45 and g - b > 45
                if green_key:
                    dst[x, y] = 0
    else:
        diff = ImageChops.difference(rgb, Image.new("RGB", rgb.size, bg)).convert("L")
        # White references have compression/AA noise around the edge. These
        # thresholds keep soft edges instead of clipping them into jagged stairs.
        alpha = diff.point(lambda v: 0 if v < 10 else 255 if v > 52 else int((v - 10) / 42 * 255))
    alpha = alpha.filter(ImageFilter.GaussianBlur(0.35))
    rgba.putalpha(alpha)
    return rgba


def fit_to_square(image: Image.Image, pad_ratio: float) -> Image.Image:
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    if bbox is None:
        return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    crop = image.crop(bbox)
    side = max(crop.size)
    pad = round(side * pad_ratio)
    square = Image.new("RGBA", (side + pad * 2, side + pad * 2), (0, 0, 0, 0))
    square.alpha_composite(crop, ((square.width - crop.width) // 2, (square.height - crop.height) // 2))
    return square.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def liquid_mask(icon: Image.Image) -> Image.Image:
    rgba = icon.convert("RGBA")
    mask = Image.new("L", rgba.size, 0)
    src = rgba.load()
    dst = mask.load()
    for y in range(rgba.height):
        for x in range(rgba.width):
            r, g, b, a = src[x, y]
            if a < 18:
                continue
            maxc = max(r, g, b)
            minc = min(r, g, b)
            saturation = maxc - minc
            pink_red = r > 145 and b > 55 and g < 120 and saturation > 55
            if pink_red:
                dst[x, y] = a

    mask = mask.filter(ImageFilter.MaxFilter(3))
    mask = mask.filter(ImageFilter.GaussianBlur(0.45))
    return mask


def remove_tiny_details(image: Image.Image, min_alpha: int) -> Image.Image:
    """Drop nearly transparent crumbs that look noisy when drawn at 10-14 px."""
    cleaned = image.copy()
    pixels = cleaned.load()
    for y in range(cleaned.height):
        for x in range(cleaned.width):
            r, g, b, a = pixels[x, y]
            if a < min_alpha:
                pixels[x, y] = (r, g, b, 0)
    return cleaned


def build_layers(icon: Image.Image, min_detail_alpha: int) -> tuple[Image.Image, Image.Image, Image.Image]:
    icon = remove_tiny_details(icon, min_detail_alpha)
    mask = liquid_mask(icon)

    liquid = Image.new("RGBA", icon.size, (255, 255, 255, 0))
    liquid.putalpha(mask)

    frame = icon.copy()
    frame_pixels = frame.load()
    mask_pixels = mask.load()
    for y in range(frame.height):
        for x in range(frame.width):
            if mask_pixels[x, y] > 24:
                r, g, b, a = frame_pixels[x, y]
                # Keep white glass highlights over the liquid; remove the
                # coloured potion fill so the user's accent can replace it.
                if not (r > 220 and g > 200 and b > 200):
                    frame_pixels[x, y] = (r, g, b, 0)

    preview_liquid = Image.new("RGBA", icon.size, (232, 24, 92, 255))
    preview_liquid.putalpha(mask)
    preview = Image.alpha_composite(preview_liquid, frame)
    return frame, liquid, preview


def main() -> None:
    args = parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)

    source = Image.open(args.source)
    icon = fit_to_square(remove_flat_background(source), args.pad)
    frame, liquid, preview = build_layers(icon, args.min_detail_alpha)

    frame.save(args.out_dir / "potion_frame.png", "PNG")
    liquid.save(args.out_dir / "potion_liquid.png", "PNG")
    preview.save(args.out_dir / "potion_bottle.png", "PNG")
    print(f"wrote: {args.out_dir / 'potion_frame.png'}")
    print(f"wrote: {args.out_dir / 'potion_liquid.png'}")
    print(f"wrote: {args.out_dir / 'potion_bottle.png'}")


if __name__ == "__main__":
    main()
