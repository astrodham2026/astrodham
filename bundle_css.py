import urllib.request
import re

headers = {'User-Agent': 'Mozilla/5.0'}

css_files = {
    'astrovastu':    'https://paid.sriastrovastu.com/generated-report-files/assets/css/astrovastu.css',
    'common_style':  'https://paid.sriastrovastu.com/generated-report-files/assets/css/common_style.css',
    'responsive':    'https://paid.sriastrovastu.com/generated-report-files/assets/css/responsive.css',
    'bootstrap':     'https://paid.sriastrovastu.com/generated-report-files/assets/bootstrap/bootstrap.min.css',
}

combined = []

for name, url in css_files.items():
    print(f"Fetching {name}...")
    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req, timeout=30) as r:
            css = r.read().decode('utf-8', errors='replace')
        print(f"  Got {len(css)} chars")

        # Remove any background-image rules related to ganesha or astro_vastu_logo
        # Find and neutralize .astro_vastu_logo rules
        original_len = len(css)
        
        # Pattern: .astro_vastu_logo { ... } — replace background-image inside
        css = re.sub(
            r'(\.astro_vastu_logo[^{]*\{[^}]*?)background[^:]*:[^;]*ganesha[^;]*;',
            r'\1background-image:none;',
            css, flags=re.IGNORECASE | re.DOTALL
        )
        css = re.sub(
            r'(\.astro_vastu_logo[^{]*\{[^}]*?)background-image\s*:[^;]+;',
            r'\1background-image:none;',
            css, flags=re.IGNORECASE | re.DOTALL
        )
        
        print(f"  Cleaned (was {original_len}, now {len(css)})")
        combined.append(f"/* === {name} === */\n{css}")
    except Exception as e:
        print(f"  ERROR: {e}")
        combined.append(f"/* === {name} — failed to fetch === */")

final_css = '\n\n'.join(combined)
with open('src/bundled_template.css', 'w', encoding='utf-8') as f:
    f.write(final_css)

print(f"\nDone! Wrote {len(final_css)} chars to src/bundled_template.css")
