import re

html = open('SAV_report_template.html', 'r', encoding='utf-8').read()

lines = html.split('\n')
for i, line in enumerate(lines):
    if 'sidebar' in line.lower() or 'report-div' in line.lower() or 'astrovastu_main' in line.lower():
        print(f"Line {i+1}: {line.strip()[:200]}")

