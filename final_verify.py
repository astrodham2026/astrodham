import re, sys
sys.stdout.reconfigure(encoding='utf-8')

html = open('SAV_report_template.html','r',encoding='utf-8').read()
css  = open('src/bundled_template.css','r',encoding='utf-8').read()

html = html.replace('../../assets/','https://paid.sriastrovastu.com/generated-report-files/assets/')
html = re.sub(r'<img[^>]*copyright_logo[^>]*/?>', '', html, flags=re.I)
html = re.sub(r'<div[^>]*astro_vastu_logo[^>]*>[\s\S]*?</div>', '', html, flags=re.I)
html = re.sub(r'<script[^>]+src=["\'][^"\']*["\'][^>]*>\s*</script>', '', html, flags=re.I)
html = re.sub(r'<link[^>]*stylesheet[^>]*/?>', '', html, flags=re.I)

g  = len(re.findall(r'astro_vastu_logo', html, re.I))
l  = len(re.findall(r'<link[^>]*stylesheet[^>]*>', html, re.I))
s  = len(re.findall(r'<script[^>]+src=', html, re.I))
cg = len(re.findall(r'background-image[^;]*ganesha', css, re.I))

print("=== FINAL VERIFICATION ===")
print(f"astro_vastu_logo in HTML : {g}   -> {'GONE' if g==0 else 'STILL PRESENT'}")
print(f"CDN stylesheet links left: {l}   -> {'ALL REMOVED' if l==0 else 'STILL PRESENT'}")
print(f"External script tags left: {s}   -> {'ALL REMOVED' if s==0 else 'STILL PRESENT'}")
print(f"Ganesha bg-image in CSS  : {cg}  -> {'CLEAN - NO GANESHA' if cg==0 else 'STILL HAS GANESHA'}")
print("")

for rule in re.findall(r'[^}]*astro_vastu_logo[^}]*\{[^}]*\}', css, re.I)[:5]:
    print(f"CSS rule found: {rule.strip()[:300]}")
