from pathlib import Path
import math
import struct
import xml.etree.ElementTree as ET
import zlib

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def check(condition, label):
    if not condition:
        raise AssertionError(label)


def png_info(path):
    data = path.read_bytes()
    check(data[:8] == b"\x89PNG\r\n\x1a\n", f"{path}: PNG signature")
    width, height, depth, colour_type = struct.unpack(">IIBB", data[16:26])
    return width, height, depth, colour_type


def png_alpha_bounds(path):
    data = path.read_bytes()
    width, height, depth, colour_type = struct.unpack(">IIBB", data[16:26])
    check((depth, colour_type) == (8, 6), f"{path}: transparent RGBA image")
    chunks = []
    offset = 8
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        if kind == b"IDAT":
            chunks.append(data[offset + 8:offset + 8 + length])
        offset += length + 12
    pixels = zlib.decompress(b"".join(chunks))
    stride = width * 4
    previous = bytearray(stride)
    bounds = [width, height, 0, 0]
    row_offset = 0
    for y in range(height):
        filter_type = pixels[row_offset]
        row_offset += 1
        row = bytearray(pixels[row_offset:row_offset + stride])
        row_offset += stride
        for index in range(stride):
            left = row[index - 4] if index >= 4 else 0
            above = previous[index]
            upper_left = previous[index - 4] if index >= 4 else 0
            if filter_type == 1:
                row[index] = (row[index] + left) & 255
            elif filter_type == 2:
                row[index] = (row[index] + above) & 255
            elif filter_type == 3:
                row[index] = (row[index] + (left + above) // 2) & 255
            elif filter_type == 4:
                estimate = left + above - upper_left
                distances = (abs(estimate - left), abs(estimate - above), abs(estimate - upper_left))
                predictor = left if distances[0] <= distances[1] and distances[0] <= distances[2] \
                    else above if distances[1] <= distances[2] else upper_left
                row[index] = (row[index] + predictor) & 255
            else:
                check(filter_type == 0, f"{path}: supported PNG filter")
        for x in range(width):
            if row[x * 4 + 3]:
                bounds[0] = min(bounds[0], x)
                bounds[1] = min(bounds[1], y)
                bounds[2] = max(bounds[2], x + 1)
                bounds[3] = max(bounds[3], y + 1)
        previous = row
    return bounds


def contrast(first, second):
    def luminance(colour):
        channels = [int(colour[index:index + 2], 16) / 255 for index in (1, 3, 5)]
        channels = [value / 12.92 if value <= 0.04045 else ((value + 0.055) / 1.055) ** 2.4
                    for value in channels]
        return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]

    bright, dark = sorted((luminance(first), luminance(second)), reverse=True)
    return (bright + 0.05) / (dark + 0.05)


manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
application = manifest.find("application")
check(application.get(ANDROID + "icon") == "@mipmap/ic_launcher", "manifest uses adaptive icon")

adaptive = ET.parse(ROOT / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").getroot()
check(adaptive.find("background").get(ANDROID + "drawable") == "@color/park_ping_icon_background",
      "adaptive icon has a background")
check(adaptive.find("foreground").get(ANDROID + "drawable") == "@drawable/ic_launcher_foreground",
      "adaptive icon has a mascot foreground")
check(adaptive.find("monochrome").get(ANDROID + "drawable") == "@drawable/park_ping_monochrome",
      "adaptive icon has a themed monochrome layer")

foreground = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_foreground.png"
width, height, depth, colour_type = png_info(foreground)
check((width, height, depth, colour_type) == (432, 432, 8, 6), "launcher foreground is transparent RGBA")
left, top, right, bottom = png_alpha_bounds(foreground)
center = width / 2
extent_x = max(abs(left - center), abs(right - center))
extent_y = max(abs(top - center), abs(bottom - center))
check(math.hypot(extent_x, extent_y) <= 33 * width / 108,
      "launcher artwork stays within the central 66dp safe circle")
monochrome = ET.parse(ROOT / "app/src/main/res/drawable/park_ping_monochrome.xml").getroot()
mono_group = monochrome.find("group")
mono_path = mono_group.find("path")
check(mono_path.get(ANDROID + "fillType") == "evenOdd", "monochrome pin has a transparent centre")
check(mono_path.get(ANDROID + "pathData") ==
      "M54,8C34.1,8 18,24.1 18,44c0,24 36,56 36,56s36,-32 36,-56C90,24.1 73.9,8 54,8zM54,28a16,16 0,1 1,0 32a16,16 0,1 1,0 -32z",
      "monochrome source geometry matches the measured pin bounds")
mono_scale_x = float(mono_group.get(ANDROID + "scaleX"))
mono_scale_y = float(mono_group.get(ANDROID + "scaleY"))
mono_radius = math.hypot(36, 46)
check(float(mono_group.get(ANDROID + "pivotX")) == 54 and
      float(mono_group.get(ANDROID + "pivotY")) == 54 and
      max(mono_scale_x, mono_scale_y) * mono_radius <= 33,
      "monochrome artwork stays within the central 66dp safe circle")

notification = ET.parse(ROOT / "app/src/main/res/drawable/ic_notification.xml").getroot()
check(notification.find("path").get(ANDROID + "fillColor") == "#FFFFFFFF", "notification glyph is solid white")
for java_file in (
    ROOT / "app/src/main/java/uk/co/collom/parkping/MainActivity.java",
    ROOT / "app/src/main/java/uk/co/collom/parkping/MonitorService.java",
):
    source = java_file.read_text()
    check("R.drawable.ic_notification" in source, f"{java_file.name} uses the notification glyph")

check(contrast("#08786F", "#E3F5F0") >= 4.5, "light brand control text meets WCAG AA contrast")
check(contrast("#72D8C7", "#203B3A") >= 4.5, "dark brand control text meets WCAG AA contrast")
check(contrast("#FFFFFF", "#08786F") >= 4.5, "primary button text meets WCAG AA contrast")
main_source = (ROOT / "app/src/main/java/uk/co/collom/parkping/MainActivity.java").read_text()
check("#08786F" in main_source and "#72D8C7" in main_source, "app uses the checked brand colours")
check("purple = accent;" in main_source and "onPurple = onAccent;" in main_source,
      "legacy navigation color fields retain the teal brand colors")

play_icon = ROOT / "docs/store-assets/park-ping-play-icon.png"
check(png_info(play_icon)[:2] == (512, 512), "Play icon is 512 x 512")
feature = ROOT / "docs/store-assets/park-ping-feature-graphic.png"
feature_info = png_info(feature)
check(feature_info[:2] == (1024, 500), "feature graphic is 1024 x 500")
check(feature_info[3] == 2, "feature graphic has no alpha channel")

print("PASS: Android identity and Play asset checks")
