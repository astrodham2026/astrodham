import base64
import os

brain_dir = r"C:\Users\Vatap\.gemini\antigravity\brain\02059c75-240e-4bf7-90a5-08dcedc5c785"

logo_path = os.path.join(brain_dir, "media__1779815834574.png")
ganesha_path = os.path.join(brain_dir, "ganesha_1779818895286.png")
zodiac_path = os.path.join(brain_dir, "zodiac_wheel_1779818920582.png")

# 1. Convert Ganesha to Base64
if os.path.exists(ganesha_path):
    print("Found Ganesha:", ganesha_path)
    with open(ganesha_path, "rb") as image_file:
        ganesha_base64 = base64.b64encode(image_file.read()).decode('utf-8')
    with open("src/ganesha_base64.txt", "w", encoding="utf-8") as f:
        f.write(ganesha_base64)
    print("Ganesha base64 written successfully.")

# 2. Convert Zodiac Wheel to Base64
if os.path.exists(zodiac_path):
    print("Found Zodiac:", zodiac_path)
    with open(zodiac_path, "rb") as image_file:
        zodiac_base64 = base64.b64encode(image_file.read()).decode('utf-8')
    with open("src/zodiac_wheel_base64.txt", "w", encoding="utf-8") as f:
        f.write(zodiac_base64)
    print("Zodiac base64 written successfully.")

# 3. Convert Logo to Base64
if os.path.exists(logo_path):
    print("Found Logo:", logo_path)
    with open(logo_path, "rb") as image_file:
        logo_base64 = base64.b64encode(image_file.read()).decode('utf-8')
    with open("src/logo_base64.txt", "w", encoding="utf-8") as f:
        f.write(logo_base64)
    print("Logo base64 written successfully.")
else:
    # Let's search for another logo file if media__1779815834574.png is not the only one
    print("Logo path not found, searching for other candidates in", brain_dir)
