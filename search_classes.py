import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

out = []
for tag in ['astro_vastu_logo', 'hero_sec_img']:
    matches = [m.start() for m in re.finditer(tag, content)]
    out.append(f"=== MATCHES FOR {tag} ===")
    for m in matches:
        snippet = content[max(0, m-200):min(len(content), m+300)]
        # Filter non-ascii to be safe
        snippet_ascii = snippet.encode('ascii', errors='replace').decode('ascii')
        out.append(snippet_ascii)
        out.append("-" * 40)

with open('search_classes_output.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))
