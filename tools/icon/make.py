#!/usr/bin/env python3
"""Practice (nikka, 日課): the launcher mark in the Kompakt stock style, a singing bowl (approved
2026-10-17 from a sheet of three; the other two are kept below for the record).
81 grid, shared ring r=19.25 x 2.5, glyph stroke 2.5 round, black on transparent.
Writes ic_launcher_mudita_bowl.xml + SVG + PNGs (512/144/72/48) here; the app's drawable is that XML."""
import math, os, subprocess
import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__)); os.chdir(HERE)
RING = open('ring.d').read().strip()
SW = 2.5


def arc(cx, cy, rx, ry, a0, a1, n=28):
    return [(cx + rx * math.cos(math.radians(a0 + (a1 - a0) * i / n)),
             cy + ry * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]


def d_open(p): return 'M' + ' '.join(f'{x:.2f},{y:.2f}' for x, y in p)
def d_closed(p): return d_open(p) + 'Z'


def bowl_parts(cx, cy, rx, ry_rim, depth, foot=True):
    """A singing bowl: the rim turned in a little, the body bellied wider than the rim and
    round underneath, so it reads as a bowl that rings rather than one you eat from."""
    rim = arc(cx, cy, rx, ry_rim, 0, 360, 48)
    belly = rx * 1.17
    body = []
    for i in range(41):
        t = i / 40                      # 0 at the left rim end, 1 at the right
        a = math.pi * t
        # width swells from the rim to the belly in the top third, then rounds in to the base
        y = cy + depth * math.sin(a) ** 0.85
        w = rx + (belly - rx) * math.sin(a) ** 0.35
        w *= 1.0 - 0.0 * t
        x = cx - w * math.cos(a)
        body.append((x, y))
    body[0] = (cx - rx, cy); body[-1] = (cx + rx, cy)
    out = [('open', rim), ('open', body)]
    if foot:
        out.append(('open', arc(cx, cy + depth - 1.0, rx * 1.05, 3.4, 15, 165, 20)))
    return out


def cand_bowl():
    """A singing bowl, wide and low, with its padded striker leaning on the right."""
    parts = bowl_parts(35.5, 36.0, 14.5, 3.9, 14.5, foot=False)
    parts.append(('open', [(54.0, 52.5), (59.6, 30.6)]))
    parts.append(('closed', arc(60.5, 27.0, 3.4, 3.4, 0, 360, 24)))
    return parts


def cand_bowl_page():
    """A day's page off a calendar, with a small bowl on it: a practice for each day."""
    x0, y0, w, h, r = 22.5, 22.0, 36.0, 37.0, 4.0
    page = (arc(x0 + w - r, y0 + r, r, r, -90, 0, 6) + arc(x0 + w - r, y0 + h - r, r, r, 0, 90, 6)
            + arc(x0 + r, y0 + h - r, r, r, 90, 180, 6) + arc(x0 + r, y0 + r, r, r, 180, 270, 6))
    parts = [('closed', page), ('open', [(x0, y0 + 9.0), (x0 + w, y0 + 9.0)]),
             ('open', [(x0 + 10, y0 - 3.5), (x0 + 10, y0 + 3.0)]),
             ('open', [(x0 + w - 10, y0 - 3.5), (x0 + w - 10, y0 + 3.0)])]
    parts += bowl_parts(40.5, 39.0, 8.5, 2.4, 11.0, foot=False)
    return parts


def cand_grid():
    """Days in rows, most of them filled: the month grid the app keeps, filled and outline only."""
    parts = []
    s, g = 6.5, 4.5
    fill = [[1, 1, 0, 1], [1, 1, 1, 1], [0, 1, 1, 1], [1, 1, 0, 0]]
    ox = 40.5 - (4 * s + 3 * g) / 2
    oy = 40.5 - (4 * s + 3 * g) / 2
    for r in range(4):
        for c in range(4):
            x, y = ox + c * (s + g), oy + r * (s + g)
            if r == 3 and c == 3: continue
            sq = [(x, y), (x + s, y), (x + s, y + s), (x, y + s)]
            parts.append(('filled' if fill[r][c] else 'closed', sq))
    return parts


CANDS = [('bowl', 'A  singing bowl', cand_bowl),
         ('page', 'B  bowl on a day page', cand_bowl_page),
         ('grid', 'C  days, filled and open', cand_grid)]


def write(name, parts):
    st = f'stroke="#000" stroke-width="{SW}" stroke-linecap="round" stroke-linejoin="round"'
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="81" height="81" viewBox="0 0 81 81">\n<path d="{RING}" fill="#000" fill-rule="evenodd"/>\n'
    xml_paths = []
    for kind, p in parts:
        if kind == 'open':
            svg += f'<path d="{d_open(p)}" fill="none" {st}/>\n'; xml_paths.append((d_open(p), '#00000000'))
        elif kind == 'closed':
            svg += f'<path d="{d_closed(p)}" fill="#fff" {st}/>\n'; xml_paths.append((d_closed(p), '#FFFFFFFF'))
        else:
            svg += f'<path d="{d_closed(p)}" fill="#000" {st}/>\n'; xml_paths.append((d_closed(p), '#FF000000'))
    svg += '</svg>\n'
    open(f'{name}.svg', 'w').write(svg)
    xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
           f'<!-- Practice (nikka): launcher icon in the Mudita Kompakt stock style (81 grid, ring r=19.25 x 2.5,\n'
           f'     glyph stroke 2.5, black on transparent). -->\n'
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="81dp"\n    android:height="81dp"\n'
           '    android:viewportWidth="81"\n    android:viewportHeight="81">\n'
           f'    <path\n        android:pathData="{RING}"\n        android:fillColor="#FF000000"\n        android:fillType="evenOdd"/>\n')
    for d, fill in xml_paths:
        xml += (f'    <path android:pathData="{d}" android:fillColor="{fill}" android:strokeColor="#FF000000" '
                f'android:strokeWidth="2.5" android:strokeLineCap="round" android:strokeLineJoin="round"/>\n')
    xml += '</vector>\n'
    open(f'ic_launcher_mudita_{name}.xml', 'w').write(xml)
    render(f'{name}.svg', name)


def render(svg, name):
    for s in (512, 144, 72, 48):
        subprocess.run(['rsvg-convert', '-w', str(s), '-h', str(s), '-b', 'white', svg, '-o', f'{name}-{s}.png'], check=True)


def ink(png):
    a = 1 - np.asarray(Image.open(png).convert('L'), dtype=float) / 255.0
    return a[a > 0.08].mean()


def sheet():
    cmp = [('stock-meditation', 'Mudita Meditation'), ('fukuyaku', 'Medicine'), ('koyomi', 'Calendar'),
           ('tokei', 'Clock'), ('solitaire', 'Solitaire')]
    for c, _ in cmp:
        render(f'cmp/{c}.svg', f'cmp/{c}')
    W, H = 1760, 908
    im = Image.new('RGB', (W, H), 'white'); dr = ImageDraw.Draw(im)
    f = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 24)
    fs = ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf', 15)
    dr.text((40, 18), 'Practice / nikka: launcher candidates (Mudita stock style)', fill='black', font=f)
    for i, (n, label, _) in enumerate(CANDS):
        big = Image.open(f'{n}-512.png').resize((512, 512))
        im.paste(big, (40 + i * 560, 60)); dr.text((40 + i * 560, 582), label, fill='black', font=f)
    row = [(n, l.split('  ')[0] + ' ' + n) for n, l, _ in CANDS] + [(f'cmp/{c}', l) for c, l in cmp]
    for size, y in ((72, 660), (48, 800)):
        dr.text((40, y - 28), f'{size} px', fill='black', font=f if size == 72 else f)
        for j, (p, l) in enumerate(row):
            x = 40 + j * 150 + (40 if j >= 3 else 0)
            im.paste(Image.open(f'{p}-{size}.png'), (x, y))
            if size == 48: dr.text((x, y + 62), l, fill=(70, 70, 70), font=fs)
    im.save('sheet.png')


def grown(parts, k):
    """The glyph scaled about its own centre; the stroke stays SW, the ring is not touched."""
    xs = [x for _, p in parts for x, _ in p]; ys = [y for _, p in parts for _, y in p]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    return [(kind, [(cx + (x - cx) * k, cy + (y - cy) * k) for x, y in p]) for kind, p in parts]


if __name__ == '__main__':
    # 2026-10-21: David found the bowl and mallet small inside the square; nudged 10%, still
    # inside the range of Mudita's own stock icons (about 45-54% of the square, longest side).
    write('bowl', grown(cand_bowl(), 1.10))
    print('bowl', f'ink48 {ink("bowl-48.png"):.3f}')
