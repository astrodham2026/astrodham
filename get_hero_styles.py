import urllib.request
import re

url = 'https://paid.sriastrovastu.com/generated-report-files/assets/css/astrovastu.css'
req = urllib.request.Request(
    url, 
    headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
)
with urllib.request.urlopen(req) as response:
    content = response.read().decode('utf-8')

classes = ['hero_wrapper', 'hero_sec', 'shubh_labh_text', 'hero_sec_txt', 'astro_vastu_logo', 'hero_sec_img']
out = []
for cls in classes:
    # Find CSS block matching class
    pattern = rf'\.{cls}\s*\{{[^}}]*\}}'
    matches = re.findall(pattern, content)
    out.append(f"=== {cls} ===")
    for m in matches:
        out.append(m)

# Also let's print responsive.css rules for the same classes
url_resp = 'https://paid.sriastrovastu.com/generated-report-files/assets/css/responsive.css'
req_resp = urllib.request.Request(
    url_resp, 
    headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
)
with urllib.request.urlopen(req_resp) as response:
    content_resp = response.read().decode('utf-8')

out.append("\n=== RESPONSIVE RULES ===")
for cls in classes:
    pattern = rf'\.{cls}\s*\{{[^}}]*\}}'
    matches = re.findall(pattern, content_resp)
    out.append(f"=== {cls} ===")
    for m in matches:
        out.append(m)

with open('get_hero_styles_output.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(out))

print("Done")
