"""
Generate small, pixel-readable potion HUD sprites.

These are intentionally separate from the large reference-style PNGs. A tiny
HUD icon cannot preserve all details from a 128px illustration, so this sprite
keeps only the shapes that still read at 16x16.

Run: python scripts/gen_potion_hud_sprite.py
"""
from pathlib import Path

from PIL import Image, ImageDraw


SIZE = 16
SCALE = 4
OUT_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vibevisuals/textures/gui"
OUT_DIR.mkdir(parents=True, exist_ok=True)


def ss():
    return Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))


def ellipse(draw, box, fill=None, outline=None, width=1):
    box = [round(v * SCALE) for v in box]
    draw.ellipse(box, fill=fill, outline=outline, width=max(1, width * SCALE))


def polygon(draw, points, fill):
    draw.polygon([(round(x * SCALE), round(y * SCALE)) for x, y in points], fill=fill)


def line(draw, points, fill, width=1):
    draw.line([(round(x * SCALE), round(y * SCALE)) for x, y in points], fill=fill, width=max(1, width * SCALE))


def rounded_rect(draw, box, radius, fill):
    box = [round(v * SCALE) for v in box]
    draw.rounded_rectangle(box, radius=round(radius * SCALE), fill=fill)


def down(image):
    return image.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def build():
    liquid = ss()
    frame = ss()
    ld = ImageDraw.Draw(liquid, "RGBA")
    fd = ImageDraw.Draw(frame, "RGBA")

    # Liquid mask: one large simple shape. Details like bubbles are removed
    # because they become noise at 16px.
    ellipse(ld, (2.7, 6.6, 11.4, 15.0), fill=(255, 255, 255, 255))
    polygon(ld, [(2.9, 10.5), (5.6, 9.1), (8.0, 9.5), (11.2, 8.4), (11.3, 15.0), (2.7, 15.0)], fill=(255, 255, 255, 255))

    # Glass body and neck. High contrast outline beats tiny decorative detail.
    ellipse(fd, (1.8, 4.8, 12.4, 15.4), fill=(160, 215, 204, 70))
    ellipse(fd, (1.8, 4.8, 12.4, 15.4), outline=(116, 170, 160, 230), width=1)
    ellipse(fd, (2.6, 5.6, 11.6, 14.6), outline=(210, 236, 226, 235), width=1)

    line(fd, [(9.2, 5.6), (11.7, 3.2)], fill=(116, 170, 160, 220), width=2)
    line(fd, [(9.7, 5.1), (12.2, 2.7)], fill=(211, 236, 226, 230), width=1)
    rounded_rect(fd, (11.2, 0.7, 14.4, 3.2), 0.8, (196, 82, 48, 255))
    line(fd, [(11.4, 1.0), (14.2, 2.8)], fill=(226, 119, 66, 210), width=1)

    # Tiny highlights that still survive at native size.
    line(fd, [(4.0, 6.9), (6.8, 6.0)], fill=(255, 255, 255, 235), width=1)
    ellipse(fd, (3.5, 11.3, 4.6, 12.8), fill=(255, 255, 255, 220))
    ellipse(fd, (8.5, 6.4, 9.4, 7.3), fill=(255, 255, 255, 210))

    # Sparkles: one-pixel readable, not fuzzy clusters.
    for x, y, c in [(1, 3, (255, 221, 81, 240)), (14, 13, (255, 221, 81, 240))]:
        polygon(fd, [(x, y - 1), (x + 0.4, y), (x, y + 1), (x - 0.4, y)], fill=c)
        polygon(fd, [(x - 1, y), (x, y + 0.4), (x + 1, y), (x, y - 0.4)], fill=c)

    liquid = down(liquid)
    frame = down(frame)
    preview_liquid = Image.new("RGBA", (SIZE, SIZE), (232, 24, 92, 255))
    preview_liquid.putalpha(liquid.getchannel("A"))
    preview = Image.alpha_composite(preview_liquid, frame)
    return frame, liquid, preview


def main():
    frame, liquid, preview = build()
    frame.save(OUT_DIR / "potion_frame_hud.png", "PNG")
    liquid.save(OUT_DIR / "potion_liquid_hud.png", "PNG")
    preview.save(OUT_DIR / "potion_bottle_hud.png", "PNG")
    print(f"wrote: {OUT_DIR / 'potion_frame_hud.png'}")
    print(f"wrote: {OUT_DIR / 'potion_liquid_hud.png'}")
    print(f"wrote: {OUT_DIR / 'potion_bottle_hud.png'}")


if __name__ == "__main__":
    main()
