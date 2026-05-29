import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

print(f"Original length: {len(html)}")

# Simulate getProcessedTemplate steps
html = html.replace('../../assets/', 'https://paid.sriastrovastu.com/generated-report-files/assets/')

# Check 1: Does astro_vastu_logo div exist before removal?
matches = re.findall(r'<div[^>]*class="[^"]*astro_vastu_logo[^"]*"[^>]*>[\s\S]*?</div>', html, re.IGNORECASE)
print(f"\nBEFORE removal - astro_vastu_logo div matches: {len(matches)}")
for m in matches:
    print(f"  Match: {repr(m[:200])}")

# Apply removal
html2 = re.sub(r'<div[^>]*class="[^"]*astro_vastu_logo[^"]*"[^>]*>[\s\S]*?</div>', '', html, flags=re.IGNORECASE)

# Check 2: After removal
matches2 = re.findall(r'astro_vastu_logo', html2, re.IGNORECASE)
print(f"\nAFTER removal - astro_vastu_logo occurrences: {len(matches2)}")
for m in matches2:
    pass

# Check 3: Are there <link> stylesheet tags?
links = re.findall(r'<link[^>]*rel=["\']stylesheet["\'][^>]*>', html2, re.IGNORECASE)
print(f"\nStylesheet <link> tags found: {len(links)}")
for l in links:
    print(f"  {l[:150]}")

# Check 4: Are there <script src> tags?
scripts = re.findall(r'<script[^>]+src=["\'][^"\']*["\'][^>]*>', html, re.IGNORECASE)
print(f"\nScript tags with src (before strip): {len(scripts)}")
for s in scripts[:10]:
    print(f"  {s[:150]}")

# Check 5: Does </head> exist for kill CSS injection?
has_head_close = '</head>' in html.lower()
print(f"\n</head> found: {has_head_close}")

# Check 6: Show hero_wrapper section
hero = re.search(r'hero_wrapper[\s\S]{0,2000}', html2)
if hero:
    print(f"\nHero section context:")
    print(html2[hero.start():hero.start()+1000])
