import re

# 1. Read base64 strings
with open('src/logo_base64.txt', 'r', encoding='utf-8') as f:
    logo_base64 = f.read().strip()

with open('src/ganesha_base64.txt', 'r', encoding='utf-8') as f:
    ganesha_base64 = f.read().strip()

with open('src/zodiac_wheel_base64.txt', 'r', encoding='utf-8') as f:
    zodiac_wheel_base64 = f.read().strip()

# 2. Read raw HTML template
with open('SAV_report_template.html', 'r', encoding='utf-8') as f:
    html = f.read()

# 3. Replace local asset paths to online CDN so it opens offline perfectly
html = html.replace('../../assets/', 'https://paid.sriastrovastu.com/generated-report-files/assets/')

# 4. Replace title and branding text dynamically
html = html.replace('SRI ASTRO VASTU', 'ASTRODHAM')
html = html.replace('Sri Astro Vastu', 'ASTRODHAM')
html = html.replace('sri astro vastu', 'ASTRODHAM')
html = html.replace('SRI ASTRO VASTU OFFICIAL KUNDLI', 'ASTRODHAM OFFICIAL KUNDLI')
html = html.replace('astro-logo.webp', 'logo.png')

# 5. Strip GTM tracking code
html = html.replace('https://www.googletagmanager.com/ns.html?id=GTM-PTD4LZR', '')

# 6. Replace Header Logo with base64 embedded tag
header_logo_tag = '<img class="header-logo" src="https://paid.sriastrovastu.com/generated-report-files/assets/images/astro-logo.webp" alt="">'
new_header_logo = f'<img class="header-logo" src="data:image/png;base64,{logo_base64}" style="height: 50px; object-fit: contain;" alt="Astrodham Logo">'
html = html.replace(header_logo_tag, new_header_logo)

# 7. Replace bottom Copyright Logo with elegant ASTRODHAM text
copyright_logo_tag = '<img class="copyright_logo" src="https://paid.sriastrovastu.com/generated-report-files/assets/images/astro-vastu-new-color-logo.webp" alt="logo" />'
new_copyright_brand = '<span class="text_purple font700" style="font-size: 18px; letter-spacing: 1px;">ASTRODHAM</span>'
html = html.replace(copyright_logo_tag, new_copyright_brand)

# 8. Inject Astrodham Custom Stylesheet inside <head> with Direct Editing and Focus Outline styles
custom_css = f"""
  <style>
    :root {{
      --purple-primary: #3A0F5A !important;   /* Astrodham Deep Purple */
      --purple-dark: #2A0644 !important;      /* Astrodham Dark Purple */
      --purple-light: #F4E8FC !important;     /* Astrodham Very Light Purple */
      --pink-light: #FDF9F2 !important;       /* Light gold/cream background */
      --cream-bg: #FCF9F5 !important;         /* Light cream */
    }}
    .main-header {{
      background: linear-gradient(135deg, #3A0F5A, #2A0644) !important;
    }}
    .bg_purple, .bg-purple {{
      background-color: #3A0F5A !important;
    }}
    .text_purple, .text-purple, .text-purple-700, .text-purple-800, .text-[#85076c], .text_brown {{
      color: #3A0F5A !important;
    }}
    .border_purple, .border-purple {{
      border-color: #3A0F5A !important;
    }}
    .border_bottom_purple {{
      border-bottom: 2px solid #3A0F5A !important;
    }}
    .border_right_purple {{
      border-right: 2px solid #3A0F5A !important;
    }}
    .shubh_labh_text, .text_brown, .basic_table_head_txt {{
      color: #D4AF37 !important; /* Astrodham Rich Gold */
    }}
    .bg_pink_light {{
      background-color: #FDF9F2 !important; /* Astrodham very light gold/cream tint */
    }}
    .chart_head_name, .basic_chart_head span, .basic_table_head_txt, .basic_details_table th, .vmsht_table_wrapper .bg_purple {{
      background-color: #3A0F5A !important;
      color: #D4AF37 !important; /* Gold text on purple headers */
    }}
    
    /* High specificity responsive Ganesha logo override */
    /* Premium interactive focus outline for inline editable text */
    [contenteditable="true"]:focus {{
      outline: 2px dashed #D4AF37 !important;
      background-color: rgba(253, 249, 242, 0.6) !important;
      border-radius: 4px;
    }}

    /* Ensure birth charts / SVGs are not contenteditable */
    .south_type_chart_cls, svg, iframe, #astrodham-edit-panel {{
      contenteditable: false !important;
    }}

    /* Hide the floating editor control panel during printing to PDF */
    @media print {{
      #astrodham-edit-panel {{
        display: none !important;
      }}
    }}
  </style>
"""

# Find head closing tag </head> and inject custom css right before it
if '</head>' in html:
    html = html.replace('</head>', f'{custom_css}</head>')
else:
    html = html.replace('<head>', f'<head>{custom_css}')

# 9. Make the body editable inline in the offline HTML file
html = html.replace('<body class="astrovastu_main_body">', '<body class="astrovastu_main_body" contenteditable="true">')

# 10. Inject floating control panel at the bottom of the body for easy offline printing/saving
floating_panel = """
  <div id="astrodham-edit-panel" style="position: fixed; bottom: 25px; right: 25px; z-index: 99999; background: linear-gradient(135deg, #3A0F5A, #2A0644); border: 2px solid #D4AF37; padding: 18px; border-radius: 16px; box-shadow: 0 12px 36px rgba(0,0,0,0.6); font-family: sans-serif; text-align: center; color: white; width: 230px;" contenteditable="false">
    <h4 style="margin: 0 0 6px 0; color: #D4AF37; font-size: 14px; font-weight: 900; letter-spacing: 1px; text-transform: uppercase;">ASTRODHAM EDITOR</h4>
    <p style="margin: 0 0 14px 0; font-size: 10px; color: #F4E8FC; line-height: 1.4;">Click anywhere directly on the text of this report to edit it offline!</p>
    <button onclick="window.print()" style="background: linear-gradient(90deg, #D4AF37, #FDF9F2); color: #3A0F5A; border: none; padding: 10px 20px; border-radius: 30px; font-weight: bold; font-size: 12px; cursor: pointer; transition: all 0.3s; box-shadow: 0 4px 10px rgba(0,0,0,0.3); width: 100%; text-transform: uppercase; letter-spacing: 0.5px;">PRINT / SAVE AS PDF</button>
  </div>
"""

if '</body>' in html:
    html = html.replace('</body>', f'{floating_panel}</body>')
else:
    html += floating_panel

# 11. Save as ASTRODHAM TEMPLATE.html
with open('ASTRODHAM TEMPLATE.html', 'w', encoding='utf-8') as f:
    f.write(html)

print("Offline Editable file 'ASTRODHAM TEMPLATE.html' generated successfully!")
