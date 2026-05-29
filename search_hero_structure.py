import re

with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    content = f.read()

match = re.search(r'class="hero_wrapper position-relative"', content)
if match:
    start = match.start()
    snippet = content[start-100:start+1500]
    with open('search_hero_structure_output.txt', 'w', encoding='utf-8') as f:
        f.write(snippet)
    print("Success")
else:
    print("Not found")
