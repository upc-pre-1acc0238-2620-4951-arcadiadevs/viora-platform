"""Renders the images of the transactional emails into src/main/resources/static/email.

    python -I scripts/email/render_email_assets.py <newsreader_variable.ttf> <output dir>

The font is the one the mobile app ships (app/src/main/res/font/newsreader_variable.ttf). The images are drawn
four times larger and scaled down, so the thin strokes come out smooth. Email clients do not run CSS animations,
so the olive sprig moves as an animated GIF; clients that only show the first frame still see a complete sprig.
"""
import math
import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

FOREST = (46, 74, 58)
HARVEST = (232, 185, 35)
HARVEST_100 = (255, 240, 180)
GREEN_100 = (233, 238, 235)
WHITE = (255, 255, 255)

# Isotype of the app (app/src/main/res/drawable/ic_isotype.xml), viewport 20 x 20
ISOTYPE = (
    "M9.65593,0.0112078C9.90109,-0.00346423 10.183,-0.00427934 10.4331,0.0120229C10.438,-0.000203777 10.4445,"
    "-0.000203777 10.4519,0.0120229C10.5802,0.0177287 10.7093,0.0234345 10.8401,0.0291403C10.8409,0.0193589 "
    "10.8417,0.0112078 10.8433,0.0112078C11.524,-0.001834 12.3698,0.189717 13.0342,0.396756C14.7781,0.939621 "
    "16.2679,1.99438 17.4471,3.26269C19.0177,4.91574 19.9624,7.18583 20,9.40538V10.6574C19.9289,15.3622 15.8977,"
    "19.5258 11.1588,19.9032C10.5189,19.9546 9.66574,19.9537 9.02424,19.9081C7.41518,19.7932 5.82655,19.2511 "
    "4.48145,18.3659C3.06933,17.3804 1.86233,16.1129 1.05658,14.5577C0.402005,13.2943 0.0457071,11.911 0.0113849,"
    "10.5441C-0.00250745,10.0118 -0.0114971,9.37359 0.0326315,8.82339C0.12334,7.69283 0.451036,6.35441 1.17752,"
    "5.4635C2.29953,4.0884 4.51822,3.47544 6.16487,4.16095C6.73691,4.39896 7.35634,4.85624 7.41355,5.50996C6.3708,"
    "5.83193 5.34114,6.17753 4.48553,6.87201C4.33435,6.99509 4.19298,7.1304 4.07775,7.28772C4.91865,6.70654 "
    "5.87967,6.21829 6.91097,6.16694C9.19994,6.12211 12.1116,6.22563 13.3488,8.58782C13.7166,9.32387 14.0623,"
    "10.0477 14.3859,10.7593C14.6841,11.3543 15.078,11.8964 15.5479,12.3683C15.5724,12.392 15.5904,12.414 "
    "15.5871,12.4319C15.5732,12.5044 14.9236,12.6177 14.7945,12.6389C14.3605,12.709 13.6986,12.7343 13.2663,"
    "12.6536C12.119,12.4392 11.4865,11.8499 10.7085,11.0389L9.35194,9.44206C8.7276,8.72394 8.02563,8.07104 "
    "7.23049,7.54448C7.11854,7.47275 7.19617,7.44503 7.30894,7.41732C7.76167,7.30809 8.56988,7.43688 9.01934,"
    "7.56567C10.4862,7.9879 11.6777,8.97581 12.4246,10.2987C11.6172,8.01317 8.99972,6.947 6.71239,6.91358C5.86414,"
    "6.90135 5.13357,7.08068 4.55173,7.73195C3.78111,8.59434 3.67161,9.82272 3.79255,10.9362C3.99848,12.8354 "
    "4.93499,14.2798 6.51463,15.3484C8.28386,16.5767 10.8262,16.7715 12.7539,15.8219C14.5885,14.918 15.8879,"
    "13.1476 16.2058,11.1342C16.3872,9.65888 16.272,8.20798 15.5103,6.82636C14.8427,5.61511 13.7591,4.61496 "
    "12.4834,4.06802C11.0035,3.43305 9.28901,3.43386 7.76984,3.92945C6.27192,2.6701 4.05242,2.74102 2.39433,"
    "3.62297C2.22844,3.67677 2.22435,3.65802 2.33059,3.51293C4.10309,1.4075 6.91914,-0.0230269 9.65593,0.0112078Z"
)

SUPERSAMPLE = 4


def isotype_polygon(size, origin):
    """Flattens the isotype path (only M, C, L, V and Z) into a polygon of the given size."""
    tokens = re.findall(r"[MCLVZ]|-?\d*\.?\d+(?:e-?\d+)?", ISOTYPE)
    points, i, command, pen = [], 0, None, (0.0, 0.0)
    while i < len(tokens):
        if tokens[i] in "MCLVZ":
            command = tokens[i]
            i += 1
            if command == "Z":
                continue
        if command in ("M", "L"):
            pen = (float(tokens[i]), float(tokens[i + 1]))
            points.append(pen)
            i += 2
        elif command == "V":
            pen = (pen[0], float(tokens[i]))
            points.append(pen)
            i += 1
        elif command == "C":
            p1 = (float(tokens[i]), float(tokens[i + 1]))
            p2 = (float(tokens[i + 2]), float(tokens[i + 3]))
            p3 = (float(tokens[i + 4]), float(tokens[i + 5]))
            for step in range(1, 13):
                t = step / 12
                points.append(tuple(
                    (1 - t) ** 3 * pen[k] + 3 * (1 - t) ** 2 * t * p1[k] + 3 * (1 - t) * t ** 2 * p2[k] + t ** 3 * p3[k]
                    for k in range(2)))
            pen = p3
            i += 6
    scale = size / 20
    return [(origin[0] + x * scale, origin[1] + y * scale) for x, y in points]


def render_logo(font_path, out):
    """Isotype and the word Viora in Newsreader, the display face of the app. 2x for sharp screens."""
    s = 2 * SUPERSAMPLE
    font = ImageFont.truetype(str(font_path), 30 * s)
    try:
        font.set_variation_by_axes([600])
    except OSError:
        pass
    width, height = 132, 40
    image = Image.new("RGB", (width * s, height * s), WHITE)
    draw = ImageDraw.Draw(image)
    draw.polygon(isotype_polygon(26 * s, (2 * s, 7 * s)), fill=FOREST)
    draw.text((36 * s, 20 * s), "Viora", font=font, fill=FOREST, anchor="lm")
    image.resize((width * 2, height * 2), Image.LANCZOS).save(out, optimize=True)


def quad(p0, p1, p2, t):
    return tuple((1 - t) ** 2 * p0[k] + 2 * (1 - t) * t * p1[k] + t ** 2 * p2[k] for k in range(2))


def quad_tangent(p0, p1, p2, t):
    return tuple(2 * (1 - t) * (p1[k] - p0[k]) + 2 * t * (p2[k] - p1[k]) for k in range(2))


def rotate(point, center, degrees):
    a = math.radians(degrees)
    x, y = point[0] - center[0], point[1] - center[1]
    return center[0] + x * math.cos(a) - y * math.sin(a), center[1] + x * math.sin(a) + y * math.cos(a)


def leaf_outline(base, angle, length, width):
    """Lanceolate olive leaf starting at base and pointing at angle (degrees, screen coordinates)."""
    a = math.radians(angle)
    ux, uy = math.cos(a), math.sin(a)
    nx, ny = -uy, ux
    side_a, side_b = [], []
    for step in range(25):
        u = step / 24
        along = u * length
        half = width / 2 * math.sin(math.pi * u) ** 0.75 * (1 - 0.25 * u)
        cx, cy = base[0] + ux * along, base[1] + uy * along
        side_a.append((cx + nx * half, cy + ny * half))
        side_b.append((cx - nx * half, cy - ny * half))
    tip = (base[0] + ux * length, base[1] + uy * length)
    return side_a + list(reversed(side_b)), tip


def render_sprig(out, frames=40, frame_ms=55):
    """An olive sprig that sways in the breeze, drawn in the line style of the app phase illustrations."""
    width, height, s = 168, 132, 2 * SUPERSAMPLE
    stem = ((28, 112), (66, 96), (142, 30))
    leaves = [  # (position on the stem, side, length, width, phase)
        (0.16, -1, 30, 9, 0.0), (0.27, 1, 32, 9.5, 0.9), (0.40, -1, 33, 10, 1.7),
        (0.52, 1, 31, 9.5, 2.6), (0.64, -1, 30, 9, 3.4), (0.76, 1, 27, 8.5, 4.3), (0.88, -1, 23, 7.5, 5.1),
    ]
    olives = [(0.46, 1, 0.5), (0.70, 1, 2.2)]  # (position, side, phase); they hang by gravity, over the leaves
    images = []
    for frame in range(frames):
        phase = 2 * math.pi * frame / frames
        sway = 2.2 * math.sin(phase)
        pivot = stem[0]
        img = Image.new("RGB", (width * s, height * s), WHITE)
        draw = ImageDraw.Draw(img)

        def p(point):
            x, y = rotate(point, pivot, sway)
            return x * s, y * s

        stroke = round(1.5 * s)
        stem_points = [p(quad(*stem, t / 40)) for t in range(41)]
        draw.line(stem_points, fill=FOREST, width=stroke, joint="curve")
        for t, side, length, leaf_width, phase_offset in leaves:
            attach = quad(*stem, t)
            tx, ty = quad_tangent(*stem, t)
            stem_angle = math.degrees(math.atan2(ty, tx))
            angle = stem_angle + side * 42 + 6 * math.sin(phase + phase_offset)
            outline, tip = leaf_outline(attach, angle, length, leaf_width)
            draw.polygon([p(q) for q in outline], fill=GREEN_100)
            draw.line([p(q) for q in outline + outline[:1]], fill=FOREST, width=stroke, joint="curve")
            mid = (attach[0] + (tip[0] - attach[0]) * 0.8, attach[1] + (tip[1] - attach[1]) * 0.8)
            draw.line([p(attach), p(mid)], fill=FOREST, width=max(1, round(0.8 * s)))
        for t, side, phase_offset in olives:
            attach = quad(*stem, t)
            swing = 90 + 8 * math.sin(phase + phase_offset) - sway
            a = math.radians(swing)
            centre = (attach[0] + math.cos(a) * 16, attach[1] + math.sin(a) * 16)
            draw.line([p(attach), p((attach[0] + math.cos(a) * 8, attach[1] + math.sin(a) * 8))], fill=FOREST,
                      width=stroke)
            olive = [rotate((centre[0] + 6.5 * math.cos(k / 30 * 2 * math.pi),
                             centre[1] + 9 * math.sin(k / 30 * 2 * math.pi)), centre, swing - 90) for k in range(30)]
            draw.polygon([p(q) for q in olive], fill=HARVEST_100, outline=HARVEST)
            draw.line([p(q) for q in olive + olive[:1]], fill=HARVEST, width=stroke, joint="curve")
            shine = rotate((centre[0] - 2.2, centre[1] - 3.2), centre, swing - 90)
            draw.ellipse([p(shine)[0] - 1.2 * s, p(shine)[1] - 1.2 * s, p(shine)[0] + 1.2 * s, p(shine)[1] + 1.2 * s],
                         fill=WHITE)
        top = quad(*stem, 1.0)
        draw.line(stem_points[-3:] + [p(top)], fill=FOREST, width=stroke)
        # breeze strokes, like the motion marks of a hand drawing, fading in and out with the sway
        breeze = 0.5 + 0.5 * math.sin(phase - 0.6)
        if breeze > 0.35:
            shift = 3 * breeze
            for (x0, y0, x1, y1) in ((150, 52, 158, 50), (148, 62, 160, 60)):
                draw.line([(x0 * s + shift * s, y0 * s), (x1 * s + shift * s, y1 * s)], fill=FOREST,
                          width=max(1, round(1.1 * s)))
        images.append(img.resize((width * 2, height * 2), Image.LANCZOS).quantize(colors=48, method=Image.MEDIANCUT))
    images[0].save(out, save_all=True, append_images=images[1:], duration=frame_ms, loop=0, optimize=True,
                   disposal=1)


def render_copy_icon(out):
    """Material "content_copy" drawn in the email's light ink on a transparent background, 2x for sharp screens."""
    s = 2 * SUPERSAMPLE
    size = 24
    image = Image.new("RGBA", (size * s, size * s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    stroke = round(2 * s)
    ink = (249, 246, 241, 255)
    draw.rounded_rectangle([8 * s, 2 * s, 21 * s, 17 * s], radius=2.5 * s, outline=ink, width=stroke)
    draw.line([(4 * s, 7 * s), (4 * s, 20 * s)], fill=ink, width=stroke)
    draw.line([(4 * s, 21 * s), (15 * s, 21 * s)], fill=ink, width=stroke)
    draw.ellipse([3 * s, 20 * s, 5 * s, 22 * s], fill=ink)
    image.resize((size * 2, size * 2), Image.LANCZOS).save(out, optimize=True)


def main():
    font_path, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    out_dir.mkdir(parents=True, exist_ok=True)
    render_logo(font_path, out_dir / "viora-logo.png")
    render_sprig(out_dir / "olive-sprig.gif")
    render_copy_icon(out_dir / "copy.png")
    for name in ("viora-logo.png", "olive-sprig.gif", "copy.png"):
        print(name, (out_dir / name).stat().st_size, "bytes")


if __name__ == "__main__":
    main()
