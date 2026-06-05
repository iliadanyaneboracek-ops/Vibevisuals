"""
Generate the Codex wheelchair cosmetic billboard texture.

This is intentionally a respectful, minimal Minecraft-ish character sitting in
a wheelchair. It is used as a client-side cosmetic only; it does not change
hitboxes or gameplay.

Run: python scripts/gen_codex_wheelchair_cosmetic.py
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter


SIZE = 512
OUT_DIR = Path(__file__).resolve().parents[1] / "src/main/resources/assets/vibevisuals/textures/entity"
OUT_DIR.mkdir(parents=True, exist_ok=True)


def rr(draw, box, radius, fill, outline=None, width=1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def main():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")

    # Soft ground shadow keeps the cosmetic anchored without being heavy.
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow, "RGBA")
    sd.ellipse((142, 438, 392, 488), fill=(0, 0, 0, 62))
    img = Image.alpha_composite(img, shadow.filter(ImageFilter.GaussianBlur(10)))
    d = ImageDraw.Draw(img, "RGBA")

    frame = (49, 62, 81, 255)
    frame_hi = (126, 154, 185, 255)
    tire = (20, 25, 34, 255)
    rim = (188, 214, 235, 255)
    accent = (100, 165, 255, 255)
    accent_dark = (49, 93, 166, 255)
    cloth = (236, 242, 250, 255)
    cloth_shadow = (188, 202, 220, 255)
    skin = (244, 210, 178, 255)
    hair = (40, 47, 61, 255)
    visor = (111, 220, 217, 255)

    # Wheelchair frame and wheels.
    d.ellipse((82, 286, 266, 470), fill=tire)
    d.ellipse((110, 314, 238, 442), fill=(0, 0, 0, 0), outline=rim, width=20)
    d.ellipse((148, 352, 200, 404), fill=frame_hi)
    for angle in range(0, 360, 45):
        import math
        cx, cy = 174, 378
        r = 58
        x = cx + int(math.cos(math.radians(angle)) * r)
        y = cy + int(math.sin(math.radians(angle)) * r)
        d.line((cx, cy, x, y), fill=frame_hi, width=5)

    d.ellipse((300, 345, 408, 453), fill=tire)
    d.ellipse((319, 364, 389, 434), fill=(0, 0, 0, 0), outline=rim, width=12)

    d.line((180, 268, 335, 330), fill=frame, width=18)
    d.line((206, 350, 348, 350), fill=frame, width=14)
    d.line((242, 250, 330, 348), fill=frame_hi, width=10)
    d.line((336, 332, 392, 302), fill=frame_hi, width=10)
    d.line((354, 384, 414, 384), fill=frame_hi, width=9)

    # Seat and backrest.
    rr(d, (198, 248, 330, 307), 16, accent_dark)
    rr(d, (205, 230, 348, 276), 14, accent)
    rr(d, (233, 180, 330, 270), 18, (52, 71, 101, 255))
    rr(d, (244, 190, 326, 262), 14, (65, 91, 133, 255))

    # Seated body.
    rr(d, (205, 156, 310, 274), 28, cloth_shadow)
    rr(d, (218, 151, 318, 263), 26, cloth)
    d.polygon([(220, 250), (306, 250), (350, 323), (279, 335)], fill=cloth_shadow)
    d.polygon([(236, 244), (311, 248), (337, 304), (276, 314)], fill=cloth)
    rr(d, (250, 277, 343, 321), 14, (50, 59, 75, 255))

    # Arms resting naturally.
    d.line((216, 196, 171, 266), fill=skin, width=22)
    d.line((306, 195, 349, 270), fill=skin, width=22)
    d.ellipse((154, 250, 186, 282), fill=skin)
    d.ellipse((334, 254, 366, 286), fill=skin)

    # Head and Codex-ish face/visor.
    d.ellipse((199, 63, 313, 177), fill=skin)
    d.pieslice((191, 50, 320, 152), 185, 356, fill=hair)
    rr(d, (218, 105, 294, 132), 11, visor)
    d.ellipse((232, 111, 242, 121), fill=(23, 52, 73, 255))
    d.ellipse((270, 111, 280, 121), fill=(23, 52, 73, 255))
    d.arc((236, 125, 278, 151), 18, 162, fill=(118, 82, 71, 180), width=4)

    # Small friendly accent mark on the hoodie.
    d.polygon([(270, 184), (285, 207), (270, 231), (255, 207)], fill=accent)
    d.polygon([(270, 192), (278, 207), (270, 222), (262, 207)], fill=(255, 255, 255, 190))

    # Clean outline pass.
    outline = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    alpha = img.getchannel("A").filter(ImageFilter.MaxFilter(7)).filter(ImageFilter.GaussianBlur(1.2))
    outline.paste((10, 15, 23, 165), mask=alpha)
    img = Image.alpha_composite(outline, img)

    img.save(OUT_DIR / "codex_wheelchair.png", "PNG")
    print(f"wrote: {OUT_DIR / 'codex_wheelchair.png'}")
    print(f"size:  {SIZE}x{SIZE}")


if __name__ == "__main__":
    main()
