from PIL import Image
import os
import shutil

src = r"C:\Users\cxh20\.workbuddy\clipboard-images\clipboard-2026-07-15T11-35-41-080Z-26da11cf.png"
base = r"C:\Users\cxh20\AndroidStudioProjects\Picocam\app\src\main\res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

im = Image.open(src).convert("RGBA")

for folder, size in sizes.items():
    out_dir = os.path.join(base, folder)
    os.makedirs(out_dir, exist_ok=True)
    # remove existing webp variants
    for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
        p = os.path.join(out_dir, name)
        if os.path.exists(p):
            os.remove(p)
    resized = im.resize((size, size), Image.LANCZOS)
    resized.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")
    resized.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")
    print(f"{folder}: {size}x{size}")

# Remove adaptive-icon XML so the launcher uses the legacy PNGs on all API levels.
# This keeps the image exactly as supplied, without Android adaptive shape masking
# cropping the rounded corners.
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    p = os.path.join(base, "mipmap-anydpi-v26", name)
    if os.path.exists(p):
        os.remove(p)
        print(f"removed {p}")

print("done")
