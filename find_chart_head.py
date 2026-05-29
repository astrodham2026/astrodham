import sys
import re
sys.stdout.reconfigure(encoding='utf-8')
html = open('SAV_report_template.html', 'r', encoding='utf-8').read()
matches = re.findall(r'<[^>]*class="[^"]*chart_head_name[^"]*"[^>]*>.*?<\/[^>]+>', html, re.IGNORECASE)
for m in matches[:5]:
    print(m)
