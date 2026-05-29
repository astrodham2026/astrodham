import urllib.request
import re

urls = [
    'https://paid.sriastrovastu.com/generated-report-files/assets/css/astrovastu.css',
    'https://paid.sriastrovastu.com/generated-report-files/assets/css/common_style.css',
    'https://paid.sriastrovastu.com/generated-report-files/assets/css/responsive.css'
]

out = []
for url in urls:
    filename = url.split('/')[-1]
    out.append(f"=== FILENAME: {filename} ===")
    try:
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )
        with urllib.request.urlopen(req) as response:
            content = response.read().decode('utf-8')
        
        # Search for .astro_vastu_logo or .hero_sec_img
        matches = re.findall(r'([^\n]*?(?:astro_vastu_logo|hero_sec_img|hero_sec|hero_wrapper)[^\n]*)', content)
        for m in matches:
            out.append(m.strip())
        
        # Also print any media query containing max-width or min-width near the class names
        # Let's search for blocks
        for m in re.finditer(r'(?:astro_vastu_logo|hero_sec_img)', content):
            start = max(0, m.start() - 300)
            end = min(len(content), m.end() + 300)
            snippet = content[start:end]
            out.append(f"--- Snippet around match in {filename} ---")
            out.append(snippet)
            out.append("-" * 30)
            
    except Exception as e:
        out.append(f"Failed to fetch {url}: {e}")

with open('search_downloaded_css_output.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))

print("CSS Search complete.")
