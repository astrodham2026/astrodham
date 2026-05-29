import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Let's search for style tags and see if they define .astro_vastu_logo or .hero_sec_img
style_blocks = re.findall(r'<style[^>]*>([^<]*)</style>', content, re.IGNORECASE)

print(f"Found {len(style_blocks)} style blocks.")

out = []
for i, block in enumerate(style_blocks):
    out.append(f"=== STYLE BLOCK {i} ===")
    lines = block.split('\n')
    for line in lines:
        if any(cls in line for cls in ['astro_vastu_logo', 'hero_sec_img', 'hero_sec', 'hero_wrapper']):
            out.append(line.strip())

# Also search for inline styles on elements with these classes
matches = re.findall(r'<[^>]+class="[^"]*(?:astro_vastu_logo|hero_sec_img)[^"]*"[^>]*>', content)
out.append("\n=== INLINE ELEMENTS ===")
for m in matches:
    out.append(m)

with open('search_css_rules_output.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))

print("Search complete.")
