import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

links = re.findall(r'<link[^>]+href=["\']([^"\']+)["\']', content)
print("Found stylesheets:")
for link in links:
    print(link)
