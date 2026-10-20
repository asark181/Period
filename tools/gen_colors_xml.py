# 由 Tokens.kt 生成 colors.xml（保证同值）。用法：python3 tools/gen_colors_xml.py [--check]
import re, sys, os
root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
tok_path = os.path.join(root, "android/app/src/main/java/com/nuanyue/app/ui/theme/Tokens.kt")
col_path = os.path.join(root, "android/app/src/main/res/values/colors.xml")
t = open(tok_path).read()
vals = {k: v for k, v in re.findall(r"val (\w+)\s*=\s*Color\(0xFF([0-9A-Fa-f]{6})\)", t)}
M = [
    ("bg", "Bg", "页面暖白底"),
    ("bg_wash", "BgWash", "顶部晕染起点"),
    ("surface", "Surface", "卡片/小组件底"),
    ("surface_sunken", "SurfaceSunken", "次级底"),
    ("divider", "Hairline", "分割线/1dp 描边"),
    ("ink", "Ink", "主文字"),
    ("ink_secondary", "InkSecondary", "次文字 ≥4.5:1"),
    ("ink_tertiary", "InkTertiary", "仅图标/禁用"),
    ("tab_unselected", "TabUnselected", "未选 tab 文字"),
    ("weekend", "Weekend", "六/日 表头"),
    ("rose", "Rose", "品牌玫瑰，仅非文字图形"),
    ("period_fill", "PeriodFill", "经期日色块"),
    ("on_period", "OnPeriod", "经期日数字"),
    ("primary_button", "PrimaryButton", "主按钮底"),
    ("on_primary_button", "OnPrimaryButton", "主按钮文字"),
    ("rose_strong", "RoseStrong", "玫瑰色文字"),
    ("rose_container", "RoseContainer", "浅玫瑰底/徽章底"),
    ("rose_outline", "RoseOutline", "通用浅玫瑰描边"),
    ("predicted_outline", "PredictedOutline", "预测经期 1.5dp 虚线"),
    ("fertile", "Fertile", "易孕浅底"),
    ("ovulation_dot", "Ovulation", "排卵圆点"),
    ("today_ring", "TodayRing", "今天外环"),
]
lines = [
    '<?xml version="1.0" encoding="utf-8"?>',
    "<!-- 暖月颜色资源（供桌面小组件 RemoteViews 等无法使用 Compose 的场景）。",
    "     由 spec/gen_colors_xml.py 从 tokens.kt 生成，必须与 tokens.kt 同值；改色请先改 tokens.kt 再重新生成。仅浅色模式。 -->",
    "<resources>",
]
for name, tok, desc in M:
    lines.append(f'    <color name="ny_{name}">#FF{vals[tok].upper()}</color> <!-- {tok}：{desc} -->')
lines.append("</resources>")
out = "\n".join(lines) + "\n"
if "--check" in sys.argv:
    ok = os.path.exists(col_path) and open(col_path).read() == out
    print("colors.xml in sync" if ok else "OUT OF SYNC")
    sys.exit(0 if ok else 1)
open(col_path, "w").write(out)
print(col_path)
