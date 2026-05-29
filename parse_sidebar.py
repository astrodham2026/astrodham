import re
import sys

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

sidebar_links = re.findall(r'<a[^>]+href="([^"]+)"[^>]*>([^<]+)</a>', html)
print("Sidebar Links Sample:")
for l in sidebar_links[:5]:
    if l[0].startswith('#'):
        print(l)

headings = re.findall(r'<h[1-6][^>]*id="([^"]+)"[^>]*>(.*?)</h[1-6]>', html, re.IGNORECASE)
print("\nHeadings with ID Sample:")
for h in headings[:5]:
    print(h)
