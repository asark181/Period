"""Render mascot moods and the sticker sheet to PNG for review.  python art/tools/preview.py out.png"""
import os, re, sys
import cairosvg

ART = os.path.join(os.path.dirname(__file__), "..")
MOODS = {
    "happy": ["body", "cheeks", "eyes_open", "mouth_smile"],
    "joy": ["body", "cheeks", "eyes_happy", "mouth_smile"],
    "sleepy": ["body", "cheeks", "eyes_sleep", "mouth_small", "zzz"],
    "period": ["body", "cheeks", "eyes_happy", "mouth_small", "bottle"],
}

def inner(path):
    s = open(path, encoding="utf-8").read()
    return re.search(r"<svg[^>]*>(.*)</svg>", s, re.S).group(1)

def mood_group(layers, x, y, size=200):
    body = "".join(inner(os.path.join(ART, "mascot", l + ".svg")) for l in layers)
    return f'<svg x="{x}" y="{y}" width="{size}" height="{size}" viewBox="0 0 200 200">{body}</svg>'

def main(out):
    parts, x = [], 10
    for name, layers in MOODS.items():
        parts.append(mood_group(layers, x, 10))
        parts.append(f'<text x="{x+100}" y="228" font-size="14" text-anchor="middle" fill="#666">{name}</text>')
        x += 210
    stickers = sorted(f for f in os.listdir(os.path.join(ART, "stickers")) if f.endswith(".svg"))
    sx, sy = 10, 250
    for f in stickers:
        s = open(os.path.join(ART, "stickers", f), encoding="utf-8").read()
        vb = re.search(r'viewBox="([^"]+)"', s).group(1).split()
        w, h = float(vb[2]), float(vb[3])
        scale = 90 / max(w, h)
        parts.append(f'<svg x="{sx}" y="{sy}" width="{w*scale}" height="{h*scale}" viewBox="{" ".join(vb)}">{inner(os.path.join(ART, "stickers", f))}</svg>')
        parts.append(f'<text x="{sx+45}" y="{sy+108}" font-size="11" text-anchor="middle" fill="#666">{f[:-4]}</text>')
        sx += 105
        if sx > 760: sx, sy = 10, sy + 125
    H = sy + 130
    doc = f'<svg xmlns="http://www.w3.org/2000/svg" width="860" height="{H}"><rect width="100%" height="100%" fill="#F7F8FC"/>{"".join(parts)}</svg>'
    cairosvg.svg2png(bytestring=doc.encode(), write_to=out, output_width=1720)

main(sys.argv[1])
