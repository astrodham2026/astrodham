import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

out_lines = []
out_lines.append("--- IMG TAGS ---")
img_tags = re.findall(r'<img[^>]+>', content)
for i, tag in enumerate(img_tags):
    # Only keep first 200 chars to avoid base64 bloat
    out_lines.append(f"{i}: {tag[:200]}")

out_lines.append("\n--- ALL UNIQUE SOURCES ---")
srcs = re.findall(r'src=["\']([^"\']+)["\']', content)
for src in sorted(list(set(srcs))):
    if len(src) < 300:
        out_lines.append(src)

out_lines.append("\n--- DIV CLASSES ---")
div_classes = re.findall(r'class=["\']([^"\']+)["\']', content)
classes = set()
for c in div_classes:
    for cls in c.split():
        classes.add(cls)
for cls in sorted(list(classes)):
    if any(x in cls.lower() for x in ['logo', 'img', 'zodiac', 'ganesh', 'wheel', 'hero']):
        out_lines.append(cls)

with open('search_images_output.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out_lines))
