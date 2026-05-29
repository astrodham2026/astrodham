import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

pattern = r'(yogini_date_htxt[^>]*>.*?<\/p>\s*<\/div>\s*)(<div class="cstm_v_line"><\/div>)'
html_new = re.sub(pattern, r'SUCCESS', html, flags=re.IGNORECASE)

print("Number of replacements:", html_new.count('SUCCESS'))
