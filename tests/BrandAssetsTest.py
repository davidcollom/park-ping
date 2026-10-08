from pathlib import Path
import math
import struct
import xml.etree.ElementTree as ET

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
icon_source = ET.parse(ROOT / "docs/brand/adaptive-icon.svg").getroot()
image = icon_source.find("{http://www.w3.org/2000/svg}image")
x, y, image_width, image_height = (float(image.get(name)) for name in ("x", "y", "width", "height"))
radius = math.hypot(max(abs(x - width / 2), abs(x + image_width - width / 2)),
                    max(abs(y - height / 2), abs(y + image_height - height / 2)))
check(radius <= width / 2, "mascot artwork stays inside circle and squircle mask safe area")
monochrome = ET.parse(ROOT / "app/src/main/res/drawable/park_ping_monochrome.xml").getroot()
check(monochrome.find("path").get(ANDROID + "fillType") == "evenOdd", "monochrome pin has a transparent centre")

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

play_icon = ROOT / "docs/store-assets/park-ping-play-icon.png"
check(png_info(play_icon)[:2] == (512, 512), "Play icon is 512 x 512")
feature = ROOT / "docs/store-assets/park-ping-feature-graphic.png"
feature_info = png_info(feature)
check(feature_info[:2] == (1024, 500), "feature graphic is 1024 x 500")
check(feature_info[3] == 2, "feature graphic has no alpha channel")

print("PASS: Android identity and Play asset checks")
