"""
Generate the layered potion HUD icon.

Outputs:
  potion_frame.png   - glass, cork, highlights, sparkles
  potion_liquid.png  - white liquid mask, tinted by MenuTheme.ACCENT_USER
  potion_bottle.png  - preview composite with a magenta liquid

Run: python scripts/gen_potion_icon.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


SIZE = 128
WORK_SIZE = 192
SCALE = 4
ROTATION_DEGREES = -34
OUT_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vibevisuals/textures/gui"
OUT_DIR.mkdir(parents=True, exist_ok=True)


def rgba(color, alpha=None):
    if alpha is None:
        return color
    return (color[0], color[1], color[2], alpha)


def rounded_line(draw, points, fill, width):
    draw.line(points, fill=fill, width=width, joint="curve")
    radius = width // 2
    for x, y in (points[0], points[-1]):
        draw.ellipse([x - radius, y - radius, x + radius, y + radius], fill=fill)


def four_point_star(draw, cx, cy, r, fill):
    draw.polygon(
        [
            (cx, cy - r),
            (cx + r * 0.28, cy - r * 0.28),
            (cx + r, cy),
            (cx + r * 0.28, cy + r * 0.28),
            (cx, cy + r),
            (cx - r * 0.28, cy + r * 0.28),
            (cx - r, cy),
            (cx - r * 0.28, cy - r * 0.28),
        ],
        fill=fill,
    )


def body_mask(size):
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    cx, cy = 94 * SCALE, 111 * SCALE
    rx, ry = 48 * SCALE, 45 * SCALE
    inset = 6 * SCALE
    d.ellipse([cx - rx + inset, cy - ry + inset, cx + rx - inset, cy + ry - inset], fill=255)
    return mask


def build_unrotated_layers():
    canvas_size = WORK_SIZE * SCALE
    frame = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    liquid = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    d = ImageDraw.Draw(frame, "RGBA")

    cx, cy = 94 * SCALE, 111 * SCALE
    rx, ry = 48 * SCALE, 45 * SCALE
    neck_w = 29 * SCALE
    neck_top = 43 * SCALE
    neck_bottom = 78 * SCALE
    cork_top = 25 * SCALE
    cork_bottom = 47 * SCALE
    cork_w = 38 * SCALE

    glass_fill = (154, 210, 202, 76)
    glass_edge = (181, 222, 210, 230)
    glass_shadow = (102, 160, 154, 125)
    glass_dot = (108, 169, 160, 120)
    white = (255, 255, 255, 230)

    # Neck and lip sit behind the round body.
    d.rounded_rectangle(
        [cx - neck_w // 2, neck_top, cx + neck_w // 2, neck_bottom],
        radius=8 * SCALE,
        fill=rgba(glass_fill, 68),
        outline=glass_edge,
        width=5 * SCALE,
    )
    d.rounded_rectangle(
        [cx - (neck_w // 2 + 5 * SCALE), neck_top - 5 * SCALE, cx + (neck_w // 2 + 5 * SCALE), neck_top + 8 * SCALE],
        radius=8 * SCALE,
        fill=rgba(glass_edge, 210),
    )

    # Cork with a tiny warm shadow and highlight, close to the reference style.
    d.rounded_rectangle(
        [cx - cork_w // 2, cork_top, cx + cork_w // 2, cork_bottom],
        radius=8 * SCALE,
        fill=(195, 86, 48, 255),
    )
    d.rounded_rectangle(
        [cx - cork_w // 2, cork_top, cx + cork_w // 2, cork_top + 7 * SCALE],
        radius=8 * SCALE,
        fill=(214, 111, 65, 190),
    )
    d.rounded_rectangle(
        [cx - cork_w // 2, cork_bottom - 6 * SCALE, cx + cork_w // 2, cork_bottom],
        radius=5 * SCALE,
        fill=(139, 61, 39, 95),
    )

    # Body glass.
    body_box = [cx - rx, cy - ry, cx + rx, cy + ry]
    d.ellipse(body_box, fill=glass_fill)
    d.ellipse(body_box, outline=glass_shadow, width=8 * SCALE)
    d.ellipse(
        [body_box[0] + 3 * SCALE, body_box[1] + 3 * SCALE, body_box[2] - 3 * SCALE, body_box[3] - 3 * SCALE],
        outline=glass_edge,
        width=5 * SCALE,
    )

    # Liquid mask: white shape with a softly curved surface and small transparent bubbles.
    mask = body_mask(canvas_size)
    liquid_shape = Image.new("L", (canvas_size, canvas_size), 0)
    md = ImageDraw.Draw(liquid_shape)
    top_y = cy + 7 * SCALE
    md.rectangle([0, top_y, canvas_size, canvas_size], fill=240)
    md.ellipse([cx - 39 * SCALE, top_y - 11 * SCALE, cx + 39 * SCALE, top_y + 13 * SCALE], fill=240)
    md.rectangle([0, 0, canvas_size, top_y - 9 * SCALE], fill=0)
    liquid_shape = Image.composite(liquid_shape, Image.new("L", (canvas_size, canvas_size), 0), mask)
    bubbles = ImageDraw.Draw(liquid_shape)
    for bx, by, br, alpha in [
        (72, 119, 3, 0),
        (84, 132, 2, 40),
        (103, 127, 2, 70),
        (116, 142, 3, 45),
        (65, 141, 2, 0),
    ]:
        bubbles.ellipse(
            [
                (bx - br) * SCALE,
                (by - br) * SCALE,
                (bx + br) * SCALE,
                (by + br) * SCALE,
            ],
            fill=alpha,
        )
    liquid.paste(Image.new("RGBA", (canvas_size, canvas_size), (255, 255, 255, 255)), mask=liquid_shape)

    # Glass highlights.
    rounded_line(
        d,
        [(62 * SCALE, 86 * SCALE), (76 * SCALE, 80 * SCALE), (94 * SCALE, 78 * SCALE)],
        (255, 255, 255, 218),
        8 * SCALE,
    )
    d.ellipse([58 * SCALE, 120 * SCALE, 66 * SCALE, 128 * SCALE], fill=(255, 255, 255, 218))
    d.ellipse([71 * SCALE, 101 * SCALE, 77 * SCALE, 107 * SCALE], fill=(255, 255, 255, 190))
    rounded_line(
        d,
        [(92 * SCALE, 49 * SCALE), (108 * SCALE, 51 * SCALE)],
        (255, 255, 255, 150),
        5 * SCALE,
    )

    # Small edge details keep the icon readable when downscaled in the HUD.
    for bx, by, br in [
        (51, 106, 2),
        (54, 137, 2),
        (97, 155, 2),
        (124, 146, 2),
        (136, 113, 2),
    ]:
        d.ellipse(
            [
                (bx - br) * SCALE,
                (by - br) * SCALE,
                (bx + br) * SCALE,
                (by + br) * SCALE,
            ],
            fill=glass_dot,
        )

    return frame, liquid


def rotate_and_crop(layer):
    rotated = layer.rotate(ROTATION_DEGREES, resample=Image.Resampling.BICUBIC, center=(96 * SCALE, 96 * SCALE))
    left = (WORK_SIZE * SCALE - SIZE * SCALE) // 2
    cropped = rotated.crop((left, left, left + SIZE * SCALE, left + SIZE * SCALE))
    return cropped.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def add_final_sparkles(frame):
    big = frame.resize((SIZE * SCALE, SIZE * SCALE), Image.Resampling.NEAREST)
    d = ImageDraw.Draw(big, "RGBA")
    gold = (255, 207, 74, 230)
    pale_gold = (255, 230, 128, 190)
    for x, y, r, color in [
        (24, 22, 7, gold),
        (34, 32, 5, pale_gold),
        (106, 98, 6, gold),
        (115, 110, 4, pale_gold),
    ]:
        four_point_star(d, x * SCALE, y * SCALE, r * SCALE, color)
    return big.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def tint_liquid(liquid, color):
    tint = Image.new("RGBA", liquid.size, color)
    tint.putalpha(liquid.getchannel("A"))
    return tint


def main():
    frame, liquid = build_unrotated_layers()

    # A tiny soft glow behind the bottle lives in the frame layer. It stays
    # subtle enough not to fight the user's chosen accent colour.
    glow = Image.new("RGBA", frame.size, (0, 0, 0, 0))
    glow_alpha = body_mask(frame.size[0]).filter(ImageFilter.GaussianBlur(5 * SCALE))
    glow.paste(Image.new("RGBA", frame.size, (137, 197, 187, 70)), mask=glow_alpha)
    frame = Image.alpha_composite(glow, frame)

    liquid_out = rotate_and_crop(liquid)
    frame_out = add_final_sparkles(rotate_and_crop(frame))

    preview = Image.alpha_composite(tint_liquid(liquid_out, (232, 24, 92, 255)), frame_out)

    liquid_out.save(OUT_DIR / "potion_liquid.png", "PNG")
    frame_out.save(OUT_DIR / "potion_frame.png", "PNG")
    preview.save(OUT_DIR / "potion_bottle.png", "PNG")

    print(f"wrote: {OUT_DIR / 'potion_liquid.png'}")
    print(f"wrote: {OUT_DIR / 'potion_frame.png'}")
    print(f"wrote: {OUT_DIR / 'potion_bottle.png'}")
    print(f"size:  {SIZE}x{SIZE}")


if __name__ == "__main__":
    main()
