"""
Generate the high-resolution keyboard icon used by the Hot Keys HUD card.

The PNG is a white alpha mask on a transparent background. Minecraft tints it
at render time, so it follows dark/light text colours and accent themes.

Run: python scripts/gen_hotkeys_keyboard_icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


SIZE = 256
SCALE = 4
OUT_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vibevisuals/textures/gui"
OUT_DIR.mkdir(parents=True, exist_ok=True)


def sc(v):
    return round(v * SCALE)


def rounded_rect(draw, box, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(
        [sc(box[0]), sc(box[1]), sc(box[2]), sc(box[3])],
        radius=sc(radius),
        fill=fill,
        outline=outline,
        width=max(1, sc(width)),
    )


def line(draw, points, fill, width=1):
    draw.line([(sc(x), sc(y)) for x, y in points], fill=fill, width=max(1, sc(width)), joint="curve")


def main():
    big = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    d = ImageDraw.Draw(big, "RGBA")

    white = (255, 255, 255, 255)
    soft = (255, 255, 255, 180)

    # Cable, styled like the reference but simplified so it stays readable small.
    line(d, [(125, 31), (125, 56), (136, 82)], white, 11)
    line(d, [(125, 31), (125, 56), (136, 82)], soft, 5)

    # Keyboard shell.
    outer = (30, 82, 226, 210)
    inner = (43, 96, 213, 197)
    rounded_rect(d, outer, 20, fill=white)
    rounded_rect(d, inner, 12, fill=(0, 0, 0, 0))

    # Re-fill the inner cutout by drawing transparent into a mask-like layer.
    # Pillow cannot erase with rounded_rectangle on the current draw context
    # directly, so use an alpha punch pass.
    alpha = big.getchannel("A")
    punch = Image.new("L", big.size, 0)
    pd = ImageDraw.Draw(punch)
    pd.rounded_rectangle([sc(inner[0]), sc(inner[1]), sc(inner[2]), sc(inner[3])], radius=sc(12), fill=255)
    alpha = Image.composite(Image.new("L", big.size, 0), alpha, punch)
    big.putalpha(alpha)
    d = ImageDraw.Draw(big, "RGBA")

    # Outline again after the punch for clean anti-aliased edges.
    rounded_rect(d, outer, 20, outline=white, width=9)

    # Minimal key grid: enough detail to read as a keyboard, not enough to blur.
    key = white
    key_w = 25
    key_h = 22
    key_r = 4
    x0 = 56
    y0 = 112
    gap = 10
    for i in range(5):
        rounded_rect(d, (x0 + i * (key_w + gap), y0, x0 + i * (key_w + gap) + key_w, y0 + key_h), key_r, fill=key)

    y1 = 149
    rounded_rect(d, (56, y1, 88, y1 + key_h), key_r, fill=key)
    rounded_rect(d, (100, y1, 132, y1 + key_h), key_r, fill=key)
    rounded_rect(d, (144, y1, 200, y1 + key_h), key_r, fill=key)

    y2 = 181
    rounded_rect(d, (56, y2, 88, y2 + key_h), key_r, fill=key)
    rounded_rect(d, (101, y2, 177, y2 + key_h), key_r, fill=key)
    rounded_rect(d, (190, y2, 211, y2 + key_h), key_r, fill=key)

    # A small internal sheen gives the icon a premium glass-HUD feel while still
    # remaining a tintable alpha mask.
    sheen = Image.new("RGBA", big.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(sheen, "RGBA")
    line(sd, [(52, 94), (96, 94), (132, 94)], (255, 255, 255, 70), 4)
    big = Image.alpha_composite(big, sheen.filter(ImageFilter.GaussianBlur(sc(0.35))))

    out = big.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
    out.save(OUT_DIR / "hotkeys_keyboard_icon.png", "PNG")
    print(f"wrote: {OUT_DIR / 'hotkeys_keyboard_icon.png'}")
    print(f"size:  {SIZE}x{SIZE}")


if __name__ == "__main__":
    main()
