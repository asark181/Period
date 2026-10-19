"""Convert the small SVG subset used in art/ into Android VectorDrawable XML.

Supports circle, ellipse, rect (with rx), path; fill / stroke / opacity / caps / joins.
    python art/tools/svg2vector.py art/mascot android/app/src/main/res/drawable mascot_
"""
import os
import re
import sys
import xml.etree.ElementTree as ET

NS = "{http://www.w3.org/2000/svg}"


def color(c, opacity):
    if not c or c == "none":
        return None
    c = c.lstrip("#")
    if len(c) == 3:
        c = "".join(ch * 2 for ch in c)
    a = round(float(opacity) * 255) if opacity is not None else 255
    return f"#{a:02X}{c.upper()}"


def circle_d(cx, cy, rx, ry):
    return (f"M{cx - rx:.2f},{cy:.2f} a{rx:.2f},{ry:.2f} 0 1,0 {2 * rx:.2f},0 "
            f"a{rx:.2f},{ry:.2f} 0 1,0 {-2 * rx:.2f},0 Z")


def rect_d(x, y, w, h, rx):
    rx = min(rx, w / 2, h / 2)
    if rx <= 0:
        return f"M{x},{y} h{w} v{h} h{-w} Z"
    return (f"M{x + rx:.2f},{y:.2f} H{x + w - rx:.2f} A{rx},{rx} 0 0 1 {x + w:.2f},{y + rx:.2f} "
            f"V{y + h - rx:.2f} A{rx},{rx} 0 0 1 {x + w - rx:.2f},{y + h:.2f} H{x + rx:.2f} "
            f"A{rx},{rx} 0 0 1 {x:.2f},{y + h - rx:.2f} V{y + rx:.2f} A{rx},{rx} 0 0 1 {x + rx:.2f},{y:.2f} Z")


def convert(src, dst, dp=None):
    root = ET.parse(src).getroot()
    vb = [float(v) for v in root.get("viewBox").split()]
    w, h = vb[2], vb[3]
    out_w, out_h = (dp, dp * h / w) if dp else (w, h)
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f"<!-- Generated from {os.path.relpath(src)} by art/tools/svg2vector.py. Edit the SVG, not this file. -->",
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{out_w:g}dp" android:height="{out_h:g}dp"',
        f'    android:viewportWidth="{w:g}" android:viewportHeight="{h:g}">',
    ]
    for el in root.iter():
        tag = el.tag.replace(NS, "")
        g = lambda k, d=0.0: float(el.get(k, d))
        if tag == "circle":
            d = circle_d(g("cx"), g("cy"), g("r"), g("r"))
        elif tag == "ellipse":
            d = circle_d(g("cx"), g("cy"), g("rx"), g("ry"))
        elif tag == "rect":
            d = rect_d(g("x"), g("y"), g("width"), g("height"), g("rx"))
        elif tag == "path":
            d = el.get("d")
        else:
            continue
        attrs = [f'android:pathData="{" ".join(d.split())}"']
        fill = color(el.get("fill", "#000000"), el.get("fill-opacity"))
        attrs.append(f'android:fillColor="{fill or "#00000000"}"')
        stroke = color(el.get("stroke"), el.get("stroke-opacity"))
        if stroke:
            attrs.append(f'android:strokeColor="{stroke}"')
            attrs.append(f'android:strokeWidth="{el.get("stroke-width", "1")}"')
            if el.get("stroke-linecap"):
                attrs.append(f'android:strokeLineCap="{el.get("stroke-linecap")}"')
            if el.get("stroke-linejoin"):
                attrs.append(f'android:strokeLineJoin="{el.get("stroke-linejoin")}"')
        lines.append("    <path " + "\n        ".join(attrs) + " />")
    lines.append("</vector>")
    open(dst, "w", encoding="utf-8").write("\n".join(lines) + "\n")


if __name__ == "__main__":
    src_dir, dst_dir, prefix = sys.argv[1:4]
    for f in sorted(os.listdir(src_dir)):
        if f.endswith(".svg"):
            convert(os.path.join(src_dir, f), os.path.join(dst_dir, prefix + f[:-4] + ".xml"))
            print("→", prefix + f[:-4])
