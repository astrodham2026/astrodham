import re

# Read the minified HTML
with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Beautify the HTML
def beautify_html(html):
    # Simple HTML formatter
    indent = 0
    result = []
    for char in html:
        if char == '>':
            result.append('>')
            if '<' not in ''.join(result[-5:]):
                result.append('\n')
                result.append('  ' * indent)
        elif char == '<':
            result.append('\n')
            result.append('  ' * indent)
            indent += 1
            result.append('<')
        else:
            result.append(char)
    return ''.join(result)

# Extract unique classes for CSS recreation
classes = re.findall(r'class="([^"]*)"', html)
all_classes = set()
for cls in classes:
    all_classes.update(cls.split())

# Extract unique IDs
ids = re.findall(r'id="([^"]*)"', html)

print("=== Unique CSS Classes Found ===")
for c in sorted(all_classes):
    print(c)

print("\n=== Unique IDs Found ===")
for i in sorted(set(ids)):
    print(i)
