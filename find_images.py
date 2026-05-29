import re
with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

matches = re.findall(r'<img[^>]+src=[\'"]([^\'"]+)[\'"][^>]*>', html, re.IGNORECASE)
for m in sorted(list(set(matches))):
    print(m)
