import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

out = []

# Search for astro_vastu_logo in context
out.append("=== astro_vastu_logo HTML CONTEXT ===")
for m in re.finditer(r'astro_vastu_logo', content):
    start = max(0, m.start() - 300)
    end   = min(len(content), m.end() + 500)
    out.append(f"\n--- match at {m.start()} ---")
    out.append(content[start:end])

# Search for ganesha references
out.append("\n\n=== GANESHA REFERENCES ===")
for m in re.finditer(r'ganesha', content, re.IGNORECASE):
    start = max(0, m.start() - 100)
    end   = min(len(content), m.end() + 200)
    out.append(f"\n--- match at {m.start()} ---")
    out.append(content[start:end])

# Search for style blocks that might have background-image for this div
out.append("\n\n=== INLINE STYLE TAGS (first 5) ===")
style_tags = re.findall(r'<style[^>]*>[\s\S]*?</style>', content)
for i, s in enumerate(style_tags[:5]):
    if 'astro_vastu' in s or 'ganesha' in s.lower() or 'hero_sec' in s:
        out.append(f"\n--- style tag {i} ---")
        out.append(s[:2000])

with open('ganesha_debug.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))

print("Done — check ganesha_debug.txt")
