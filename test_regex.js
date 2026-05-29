const fs = require('fs');
let html = fs.readFileSync('SAV_report_template.html', 'utf-8');

const regex = /(yogini_date_htxt[^>]*>.*?<\/p>\s*<\/div>\s*)(<div class="cstm_v_line"><\/div>)/gi;

const newHtml = html.replace(regex, '$1SUCCESS');
console.log('Matches:', newHtml.split('SUCCESS').length - 1);
