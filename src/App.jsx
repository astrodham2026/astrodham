import React, { useState, useEffect, useRef } from 'react';
import rawTemplate from '../SAV_report_template.html?raw';
import logoBase64Raw from './logo_base64.txt?raw';
import zodiacWheelBase64Raw from './zodiac_wheel_base64.txt?raw';
import newGaneshaBase64Raw from './new_ganesha_base64.txt?raw';
import bundledTemplateCSS from './bundled_template.css?raw';
import {
  Printer,
  Download,
  Edit3,
  ChevronDown,
  ChevronUp,
  RefreshCw,
  Compass,
  Check,
  Search,
  Trash2,
  Plus
} from 'lucide-react';

/* ── Constants ─────────────────────────────────────────────── */
const STORAGE_KEY   = 'SAV_DYNAMIC_BUILDER_V22';
const CUSTOM_IMAGES_KEY = 'ASTRODHAM_CUSTOM_IMAGES';
const logoBase64   = logoBase64Raw.trim();
const zodiacBase64 = zodiacWheelBase64Raw.trim();
const newGaneshaBase64 = newGaneshaBase64Raw.trim();

/* Theme tokens (used in both the shell and the injected iframe) */
const T = {
  purple:      '#3D0C6E',   // Deep Royal Purple
  purpleDark:  '#25074A',   // Darker Purple
  purpleMid:   '#5A1796',   // Mid Purple
  purpleLight: '#EDE0F8',   // Very Light Lavender
  gold:        '#C9A227',   // Metallic Gold
  goldLight:   '#F5E6B0',   // Light Gold Tint
  copper:      '#B87333',   // Warm Copper
  amber:       '#CC8822',   // Amber Gold
  white:       '#FFFFFF',
  offWhite:    '#FAF8FF',
  shellBg:     '#0E0520',   // App shell dark bg
  shellPanel:  '#1A0B36',   // Panel bg
  shellCard:   '#240D47',   // Card bg
  shellHover:  '#2E1258',   // Hover state
  shellBorder: 'rgba(201,162,39,0.18)',
};

/* ── Telugu zodiac sign names ───────────────────────────────── */
const SIGN_NAMES_TELUGU = [
  'మీనం','మేషం','వృషభం','మిథునం','కర్కాటకం','సింహం',
  'కన్య','తుల','వృశ్చికం','ధనుస్సు','మకరం','కుంభం'
];

const LAGNA_TO_CELL_INDEX = {
  'మీనం':0,'Pisces':0,'Meena':0,
  'మేషం':1,'Mesha':1,'Aries':1,
  'వృషభం':2,'Vrishabha':2,'Taurus':2,
  'మిథునం':3,'Mithuna':3,'Gemini':3,
  'కర్కాటకం':4,'Karkataka':4,'Cancer':4,
  'సింహం':5,'Simha':5,'Leo':5,
  'కన్య':6,'Kanya':6,'Virgo':6,
  'తుల':7,'Tula':7,'Libra':7,
  'వృశ్చికం':8,'Vrishchika':8,'Scorpio':8,
  'ధనుస్సు':9,'Dhanus':9,'Sagittarius':9,
  'మకరం':10,'Makara':10,'Capricorn':10,
  'కుంభం':11,'Kumbha':11,'Aquarius':11,
};

/* ── Build initial iframe HTML (Run once globally) ────────── */
const getProcessedTemplate = () => {
  let html = rawTemplate;

  /* Replace title and favicon */
  html = html.replace(/<title>.*?<\/title>/i, '<title>Astrodham</title>');
  html = html.replace(
    /<link[^>]*rel="icon"[^>]*\/?>/i,
    `<link rel="icon" href="data:image/png;base64,${logoBase64}" type="image/png"><link rel="preconnect" href="https://fonts.googleapis.com"><link rel="preconnect" href="https://fonts.gstatic.com" crossorigin><link href="https://fonts.googleapis.com/css2?family=Noto+Serif+Telugu:wght@400;600;700&display=swap" rel="stylesheet">`
  );

  html = html.replaceAll('../../assets/', 'https://paid.sriastrovastu.com/generated-report-files/assets/');
  html = html.replaceAll('SRI ASTRO VASTU', 'ASTRODHAM');
  html = html.replaceAll('Sri Astro Vastu', 'ASTRODHAM');
  html = html.replaceAll('sri astro vastu', 'ASTRODHAM');
  html = html.replaceAll('SRI ASTRO VASTU OFFICIAL KUNDLI', 'ASTRODHAM OFFICIAL KUNDLI');
  html = html.replace(/<img[^>]*class="[^"]*copyright_logo[^"]*"[^>]*\/?>/gi, '');
  html = html.replace(/<img[^>]*astro-vastu-new-color-logo[^>]*\/?>/gi, '');
  html = html.replace(/<div[^>]*class="[^"]*astro_vastu_logo[^"]*"[^>]*>[\s\S]*?<\/div>/gi, '');
  /* Inject Zodiac wheel IMG inside hero_sec_img + Ganesha wrapper after it, both inside hero_sec */
  html = html.replace(
    /(<div[^>]*class="[^"]*hero_sec_img[^"]*"[^>]*>)[\s\S]*?(<\/div>)/i,
    '$1<img src="data:image/png;base64,' + zodiacBase64 + '" alt="Zodiac Wheel" class="zodiac_wheel_img" />$2\n<div class="new_ganesha_wrapper"><img src="data:image/png;base64,' + newGaneshaBase64 + '" alt="Lord Ganesha" class="new_ganesha_img" /></div>'
  );
  html = html.replaceAll('https://www.googletagmanager.com/ns.html?id=GTM-PTD4LZR', '');

  /* Inject clickable placeholders for Antar Dasha periods ONLY (avoiding other places with cstm_v_line) */
  const placeholderSVG = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='50' height='50'><rect width='50' height='50' fill='%23e0d8f0' rx='25'/><text x='25' y='32' font-size='24' font-weight='bold' text-anchor='middle' fill='%233D0C6E'>+</text></svg>";
  html = html.replace(
    /(yogini_date_htxt[^>]*>[\s\S]*?<\/p>\s*<\/div>\s*)(<div class="cstm_v_line"><\/div>)/gi,
    `$1<img src="${placeholderSVG}" alt="Add Planet Image" class="antar_dasha_img mx-3 shadow-sm" style="width:50px; height:50px; border-radius:50%; object-fit:contain; cursor:pointer; border:2px solid #3D0C6E;" title="Click to add custom image" />$2`
  );

  const layoutFixCSS = `
    @media (min-width: 992px) {
      .report-div { margin-left: 285px !important; width: calc(100% - 285px) !important; }
    }
    @media (max-width: 991px) {
      .report-div { margin-left: 0 !important; width: 100% !important; }
    }
    
    /* Table headers / Charts */
    .chart_head_name, .chart_head_name:hover,
    .basic_chart_head span, .basic_chart_head span:hover,
    .basic_table_head_txt, .basic_table_head_txt:hover,
    .basic_details_table th, .basic_details_table th:hover,
    .vmsht_table_wrapper .bg_purple, .vmsht_table_wrapper .bg_purple:hover {
      background-color: #2F0958 !important;
      color: #ffffff !important;
    }

    /* Prevent bg_purple from disappearing on hover */
    .bg_purple:hover, .basic_table_head_txt:hover {
      background: #2F0958 !important;
    }

    /* Aggressively remove Bootstrap color hover effect in ALL tables */
    table.table-hover tbody tr:hover > *,
    table tbody tr:hover > * {
      --bs-table-accent-bg: transparent !important;
      box-shadow: none !important;
      color: inherit !important;
    }
    
    /* Enforce purple headlines BUT exclude chart_head_name and basic_table_head_txt so they stay white */
    .sidebar-main-header:not(.chart_head_name):not(.basic_table_head_txt), 
    .sidebar-main-header:not(.chart_head_name):not(.basic_table_head_txt) *, 
    .sub-header:not(.chart_head_name):not(.basic_table_head_txt), 
    .sub-header:not(.chart_head_name):not(.basic_table_head_txt) * {
      color: #3D0C6E !important;
    }

    /* ── FIX 1: Telugu text — kill the 5px letter-spacing that breaks ligatures ── */
    .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .prm_txt,
    .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .lifeknd_txt,
    .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .report_txt,
    .prm_txt, .lifeknd_txt, .report_txt {
      letter-spacing: 0 !important;
      word-spacing: 0 !important;
      font-family: 'Noto Serif Telugu', 'Noto Sans Telugu', serif !important;
      font-feature-settings: 'liga' 1, 'calt' 1 !important;
      -webkit-font-feature-settings: 'liga' 1, 'calt' 1 !important;
      text-rendering: optimizeLegibility !important;
    }

    /* ── DESKTOP: Zodiac wheel (now an actual img) + Ganesha ── */
    @media (min-width: 768px) {
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .hero_sec {
        position: relative !important;
        width: 100% !important;
        min-height: 450px !important;
      }
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .hero_sec_img {
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        width: 100% !important;
        min-height: 450px !important;
      }
      .zodiac_wheel_img {
        width: 35% !important;
        height: auto !important;
        display: block !important;
        margin: 0 auto !important;
        position: relative !important;
        z-index: 5 !important;
      }
    }

    /* ── FIX 2: Hero section + Ganesha + Zodiac Wheel on mobile ── */
    @media (max-width: 767px) {

      /* Hero wrapper: flex column so shubh_labh → hero_sec → text stack vertically */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper {
        position: relative !important;
        display: flex !important;
        flex-direction: column !important;
        align-items: center !important;
        overflow: visible !important;
        padding: 0 !important;
      }

      /* shubh_labh: take it OUT of absolute, flow it at the TOP */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .shubh_labh_text {
        position: relative !important;
        left: auto !important;
        transform: none !important;
        top: auto !important;
        text-align: center !important;
        width: 100% !important;
        font-size: 24px !important;
        padding: 12px 0 8px !important;
        z-index: 20 !important;
        order: 1 !important;
      }

      /* Hero section: in flex flow, SECOND */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .hero_sec {
        width: 100% !important;
        height: 280px !important;
        aspect-ratio: unset !important;
        position: relative !important;
        flex-shrink: 0 !important;
        order: 2 !important;
      }

      /* Zodiac wheel: fill hero_sec properly on mobile */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .hero_sec_img {
        height: 280px !important;
        background-size: 85% !important;
        background-position: center center !important;
        background-repeat: no-repeat !important;
      }

      /* Ganesha: centered inside hero_sec (now a sibling inside hero_sec) */
      .new_ganesha_wrapper {
        position: absolute !important;
        top: 50% !important;
        left: 50% !important;
        transform: translate(-50%, -50%) !important;
        width: auto !important;
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        z-index: 10 !important;
      }
      .new_ganesha_img {
        max-height: 220px !important;
        width: auto !important;
        object-fit: contain !important;
      }

      /* Telugu title text: BELOW hero_sec, in flex flow, THIRD */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .hero_sec_txt {
        position: relative !important;
        bottom: auto !important;
        left: auto !important;
        transform: none !important;
        display: flex !important;
        flex-direction: column !important;
        align-items: center !important;
        text-align: center !important;
        padding: 12px 16px 4px !important;
        width: 100% !important;
        order: 3 !important;
      }

      /* Telugu title font sizes for mobile */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .prm_txt {
        font-size: 18px !important;
      }
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .lifeknd_txt {
        font-size: 20px !important;
      }
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .report_txt {
        font-size: 18px !important;
      }

      /* Line container: give small top margin */
      .astrovastu_main_body .astrovastu_wrapper .hero_wrapper .line_container {
        margin-top: 6px !important;
        order: 4 !important;
      }
    }
  `;
  const inlineCSS = `<style id="astrodham-bundled-css">${bundledTemplateCSS}\n${layoutFixCSS}</style>`;
  html = html.replace(/<link[^>]*rel=["']stylesheet["'][^>]*\/?>/gi, '');
  html = html.replace(/<link[^>]*stylesheet[^>]*\/?>/gi, '');
  html = html.replace(/<\/head>/i, inlineCSS + '</head>');

  const earlyRemoveScript = `
<script id="astrodham-remover">
(function(){
function removeNow(){
  ['.astro_vastu_logo','.astro_vastu_logo_wrap','.copyright_logo'].forEach(function(s){
    document.querySelectorAll(s).forEach(function(el){
      if(el.parentNode) el.parentNode.removeChild(el);
    });
  });
}
removeNow();
document.addEventListener('DOMContentLoaded', removeNow);
new MutationObserver(removeNow).observe(document.documentElement, {childList:true,subtree:true});
})();
<\/script>`;
  html = html.replace(/<head>/i, '<head>' + earlyRemoveScript);

  return html;
};

const INITIAL_IFRAME_HTML = getProcessedTemplate();

/* ── Replacement Images for Report ──────────────────────────── */
const REPLACEMENT_IMAGES = [
  { name: 'Aquarius', url: 'https://i.postimg.cc/jjcn5fpL/Aquarius.png' },
  { name: 'Aries', url: 'https://i.postimg.cc/xCdJbCQ8/Aries.png' },
  { name: 'Blue-Sapphire', url: 'https://i.postimg.cc/KYxgxfsC/Blue-Sapphire.png' },
  { name: 'Cancer', url: 'https://i.postimg.cc/3w10RGPc/Cancer.png' },
  { name: 'Capricorn', url: 'https://i.postimg.cc/GpKT2DWm/Capricorn.png' },
  { name: 'cats-eye', url: 'https://i.postimg.cc/YSMmMRyy/cats-eye.png' },
  { name: 'Diamond', url: 'https://i.postimg.cc/RFb31BCg/Diamond.png' },
  { name: 'Emerald', url: 'https://i.postimg.cc/0NPJPnH3/Emerald.png' },
  { name: 'Gemini', url: 'https://i.postimg.cc/KzYkgzbT/Gemini.png' },
  { name: 'Hessonite-Garnet', url: 'https://i.postimg.cc/Zq4y4HDM/Hessonite-Garnet.png' },
  { name: 'Leo', url: 'https://i.postimg.cc/q7w6RKPH/Leo.png' },
  { name: 'Libra', url: 'https://i.postimg.cc/25GL6WPt/Libra.png' },
  { name: 'Pearl', url: 'https://i.postimg.cc/4d37hdGv/Pearl.png' },
  { name: 'Pisces', url: 'https://i.postimg.cc/gJ0X6Jp0/Pisces.png' },
  { name: 'Red-Coral', url: 'https://i.postimg.cc/vBmg6Byz/Red-Coral.png' },
  { name: 'Ruby', url: 'https://i.postimg.cc/k456t4CT/Ruby.png' },
  { name: 'Sagittarius', url: 'https://i.postimg.cc/q7w6RK9Z/Sagittarius.png' },
  { name: 'Scorpio', url: 'https://i.postimg.cc/QMmKt72N/Scorpio.png' },
  { name: 'Taurus', url: 'https://i.postimg.cc/rmp0tmT0/Taurus.png' },
  { name: 'Virgo', url: 'https://i.postimg.cc/CK7n183h/Virgo.png' },
  { name: 'Yellow-Sapphire', url: 'https://i.postimg.cc/1tYg0QRc/Yellow-Sapphire.png' },
  { name: 'Budha', url: 'https://i.postimg.cc/ncRG0CT7/Budha.png' },
  { name: 'Chandra', url: 'https://i.postimg.cc/Yqqx61N1/Chandra.png' },
  { name: 'Guru', url: 'https://i.postimg.cc/vHqt3cXn/Guru.png' },
  { name: 'Ketu', url: 'https://i.postimg.cc/288xQ4dh/Ketu.png' },
  { name: 'Kuja', url: 'https://i.postimg.cc/Yqnzbh86/Kuja.png' },
  { name: 'Rahu', url: 'https://i.postimg.cc/bNNRxQ0x/Rahu.png' },
  { name: 'Shani', url: 'https://i.postimg.cc/GhhjFkPj/Shani.png' },
  { name: 'Shukra', url: 'https://i.postimg.cc/c44cRQ7m/Shukra.png' },
  { name: 'Surya', url: 'https://i.postimg.cc/ncc14vKq/Surya.png' }
];

/* ── Main component ─────────────────────────────────────────── */
export default function App() {
  const [charts, setCharts] = useState({
    d1:   ['రాహు, శుక్ర','కుజ','','','గురు','','కేతు','శని','','','','రవి, బుధ, చం'],
    moon: ['చం, రవి, బుధ','శుక్ర, రాహు','కుజ','','','గురు','','కేతు','శని','','',''],
    d9:   ['కేతు','శుక్ర','కుజ','','గురు','రవి','రాహు','','','','చం, శని','బుధ'],
  });

  const [chartsLagna, setChartsLagna] = useState({
    d1:   'సింహం',
    moon: 'కర్కాటకం',
    d9:   'తుల',
  });

  const [isDrawerOpen,   setIsDrawerOpen]   = useState(false);
  const [activeChartTab, setActiveChartTab] = useState('d1');
  const [iframeLoaded,   setIframeLoaded]   = useState(false);
  const [isSaved,        setIsSaved]        = useState(false);
  
  /* Image Manager State */
  const [customImages, setCustomImages] = useState(REPLACEMENT_IMAGES);
  const [isImagePickerOpen, setIsImagePickerOpen] = useState(false);
  const [imageSearchQuery, setImageSearchQuery] = useState('');
  const [newImgName, setNewImgName] = useState('');
  const [newImgUrl, setNewImgUrl] = useState('');
  
  const iframeRef = useRef(null);
  const selectedImgRef = useRef(null);

  /* 1. Restore saved state */
  useEffect(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      try {
        const p = JSON.parse(stored);
        if (p.charts)      setCharts(p.charts);
        if (p.chartsLagna) setChartsLagna(p.chartsLagna);
      } catch {}
    }
    
    const storedImages = localStorage.getItem(CUSTOM_IMAGES_KEY);
    if (storedImages) {
      try {
        setCustomImages(JSON.parse(storedImages));
      } catch {}
    }
  }, []);

  const saveCustomImages = (images) => {
    setCustomImages(images);
    localStorage.setItem(CUSTOM_IMAGES_KEY, JSON.stringify(images));
  };

  /* 2. Save state */
  const saveState = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ charts, chartsLagna }));
    setIsSaved(true);
    setTimeout(() => setIsSaved(false), 2000);
  };

  /* 3. Generate South-Indian kundali SVG */
  const generateChartSVG = (dataList, startSign) => {
    const cells = [
      { x:43.75,  y:43.75  }, { x:131.25, y:43.75  }, { x:218.75, y:43.75  }, { x:306.25, y:43.75  },
      { x:306.25, y:131.25 }, { x:306.25, y:218.75 }, { x:306.25, y:306.25 }, { x:218.75, y:306.25 },
      { x:131.25, y:306.25 }, { x:43.75,  y:306.25 }, { x:43.75,  y:218.75 }, { x:43.75,  y:131.25 },
    ];

    const startIdx   = LAGNA_TO_CELL_INDEX[startSign] || 1;
    const cellValues = Array(12).fill('');

    dataList.forEach((houseData, idx) => {
      const ci      = (startIdx + idx) % 12;
      let content   = houseData || '';
      if (idx === 0 && !content.toLowerCase().includes('asc') && !content.includes('ల'))
        content = content ? `Asc, ${content}` : 'Asc';
      cellValues[ci] = content;
    });

    let els = '';
    cells.forEach((cell, idx) => {
      const content = cellValues[idx] || '';
      const planets = content.split(/[\s,]+/).map(p => p.trim()).filter(Boolean);

      els += `<text x="${cell.x - 38}" y="${cell.y - 30}" text-anchor="start" font-size="9px" font-weight="bold" fill="#000000">${SIGN_NAMES_TELUGU[idx]}</text>`;

      planets.forEach((planet, pIdx) => {
        const isAsc = planet.toLowerCase() === 'asc' || planet === 'ల';
        const color = isAsc ? '#1a5fe0' : T.purple;
        const size  = isAsc ? '14px' : '13px';
        const lineH = 16;
        const startY = cell.y - ((planets.length - 1) * lineH) / 2;
        const yPos   = startY + pIdx * lineH;
        els += `<text x="${cell.x}" y="${yPos + 4}" text-anchor="middle" font-size="${size}" font-weight="bold" fill="${color}">${planet}</text>`;
      });
    });

    return `<svg viewBox="0 0 350 350" width="350" height="350" xmlns="http://www.w3.org/2000/svg">
      <rect x="0" y="0" width="350" height="350" stroke="${T.purple}" stroke-width="3" fill="none"/>
      <line x1="87.5"  y1="0"   x2="87.5"  y2="350" stroke-width="2" stroke="${T.purple}"/>
      <line x1="262.5" y1="0"   x2="262.5" y2="350" stroke-width="2" stroke="${T.purple}"/>
      <line x1="0"     y1="87.5"  x2="350" y2="87.5"  stroke-width="2" stroke="${T.purple}"/>
      <line x1="0"     y1="262.5" x2="350" y2="262.5" stroke-width="2" stroke="${T.purple}"/>
      <line x1="0"     y1="175"   x2="87.5" y2="175"  stroke-width="2" stroke="${T.purple}"/>
      <line x1="262.5" y1="175"   x2="350"  y2="175"  stroke-width="2" stroke="${T.purple}"/>
      <line x1="175"   y1="0"     x2="175"  y2="87.5" stroke-width="2" stroke="${T.purple}"/>
      <line x1="175"   y1="262.5" x2="175"  y2="350"  stroke-width="2" stroke="${T.purple}"/>
      ${els}
    </svg>`;
  };

  const toBase64SVG = (svg) => btoa(unescape(encodeURIComponent(svg)));

  /* 4. Push charts into live iframe */
  const updateIframeCharts = () => {
    const iframe = iframeRef.current;
    if (!iframe || !iframeLoaded) return;
    try {
      const d1b64   = toBase64SVG(generateChartSVG(charts.d1,   chartsLagna.d1));
      const moonb64 = toBase64SVG(generateChartSVG(charts.moon, chartsLagna.moon));
      const d9b64   = toBase64SVG(generateChartSVG(charts.d9,   chartsLagna.d9));
      const imgs    = iframe.contentDocument.querySelectorAll('.south_type_chart_cls');
      if (imgs.length >= 5) {
        imgs[0].src = `data:image/svg+xml;base64,${d1b64}`;
        imgs[1].src = `data:image/svg+xml;base64,${moonb64}`;
        imgs[2].src = `data:image/svg+xml;base64,${d9b64}`;
        imgs[3].src = `data:image/svg+xml;base64,${d1b64}`;
        imgs[4].src = `data:image/svg+xml;base64,${moonb64}`;
      }
    } catch (e) { console.error('Chart update error', e); }
  };

  useEffect(() => { updateIframeCharts(); }, [charts, chartsLagna, iframeLoaded]);

  /* 5. Inject styles + make editable when iframe loads */
  const handleIframeLoad = () => {
    const iframe = iframeRef.current;
    if (!iframe) return;
    try {
      const doc = iframe.contentDocument || iframe.contentWindow.document;

      /* Make body directly editable */
      doc.body.setAttribute('contenteditable', 'true');

      /* ── Injected theme overrides ── */
      const style = doc.createElement('style');
      style.innerHTML = `
        /* ── Astrodham Theme: Deep Royal Purple · Metallic Gold · Warm Copper · White ── */
        :root {
          --purple-primary: ${T.purple} !important;
          --purple-dark:    ${T.purpleDark} !important;
          --purple-light:   ${T.purpleLight} !important;
          --gold:           ${T.gold} !important;
          --copper:         ${T.copper} !important;
          --amber:          ${T.amber} !important;
          --pink-light:     ${T.offWhite} !important;
          --cream-bg:       ${T.offWhite} !important;
        }

        /* Header */
        .main-header {
          background: linear-gradient(135deg, ${T.purple} 0%, ${T.purpleDark} 100%) !important;
        }
        .main-header * { color: ${T.white} !important; }
        .main-header .shubh_labh_text,
        .main-header .text_gold {
          color: ${T.gold} !important;
        }

        /* Logo sizing */
        .header-logo {
          height: 50px !important;
          object-fit: contain !important;
        }

        /* Purple fills */
        .bg_purple, .bg-purple {
          background-color: ${T.purple} !important;
        }

        /* Gold fills */
        .bg_gold { background-color: ${T.gold} !important; }

        /* Text colors */
        .text_purple, .text-purple, .text-purple-700, .text-purple-800,
        .text_brown, .text-[#85076c] {
          color: ${T.purple} !important;
        }
        .shubh_labh_text, .text_gold {
          color: ${T.gold} !important;
        }
        .text_copper { color: ${T.copper} !important; }

        /* Borders */
        .border_purple,  .border-purple  { border-color: ${T.purple} !important; }
        .border_bottom_purple            { border-bottom: 2px solid ${T.purple} !important; }
        .border_right_purple             { border-right:  2px solid ${T.purple} !important; }

        /* Table headers / Charts */
        .chart_head_name, .chart_head_name:hover,
        .basic_chart_head span, .basic_chart_head span:hover,
        .basic_table_head_txt, .basic_table_head_txt:hover,
        .basic_details_table th, .basic_details_table th:hover,
        .vmsht_table_wrapper .bg_purple, .vmsht_table_wrapper .bg_purple:hover {
          background-color: #2F0958 !important;
          color: #ffffff !important;
        }

        /* Prevent bg_purple from disappearing on hover */
        .bg_purple:hover, .basic_table_head_txt:hover {
          background: #2F0958 !important;
        }

        /* Aggressively remove Bootstrap color hover effect in ALL tables */
        table.table-hover tbody tr:hover > *,
        table tbody tr:hover > * {
          --bs-table-accent-bg: transparent !important;
          box-shadow: none !important;
          color: inherit !important;
        }

        /* Enforce purple headlines BUT exclude chart_head_name and basic_table_head_txt so they stay white */
        .sidebar-main-header:not(.chart_head_name):not(.basic_table_head_txt), 
        .sidebar-main-header:not(.chart_head_name):not(.basic_table_head_txt) *, 
        .sub-header:not(.chart_head_name):not(.basic_table_head_txt), 
        .sub-header:not(.chart_head_name):not(.basic_table_head_txt) * {
          color: #3D0C6E !important;
        }

        /* Alternating rows */
        .bg_pink_light {
          background-color: ${T.offWhite} !important;
        }

        /* Zodiac wheel is now an actual <img>, no background-image needed */
        .hero_sec {
          position: relative !important;
        }
        .hero_sec_img {
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
        }
        .zodiac_wheel_img {
          display: block !important;
          margin: 0 auto !important;
        }

        /* Desktop zodiac sizing */
        @media (min-width: 768px) {
          .hero_sec {
            width: 100% !important;
            min-height: 450px !important;
          }
          .hero_sec_img {
            width: 100% !important;
            min-height: 450px !important;
          }
          .zodiac_wheel_img {
            width: 35% !important;
            height: auto !important;
            position: relative !important;
            z-index: 5 !important;
          }
        }

        /* Lord Ganesha div — permanently hidden */
        .astro_vastu_logo, .astro_vastu_logo_wrap {
          display: none !important;
        }

        /* New Ganesha Image styling — Ganesha is now INSIDE hero_sec */
        .new_ganesha_wrapper {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          z-index: 10;
          display: flex;
          justify-content: center;
          align-items: center;
          width: 100%;
        }
        .new_ganesha_img {
          width: auto;
          max-height: 85%;
          object-fit: contain;
        }

        /* Mobile: zodiac wheel sizing */
        @media (max-width: 767px) {
          .zodiac_wheel_img {
            width: 88% !important;
            height: auto !important;
          }
          .hero_sec_img {
            min-height: 280px !important;
          }
          .new_ganesha_img {
            max-height: 220px !important;
          }
        }

        /* Telugu text ligature fix — use system-ui (phone's native Telugu font) + kill letter-spacing */
        .prm_txt, .lifeknd_txt, .report_txt,
        .hero_sec_txt span {
          font-family: system-ui, -apple-system, 'Noto Serif Telugu', 'Noto Sans Telugu', sans-serif !important;
          letter-spacing: 0 !important;
          word-spacing: 0 !important;
          font-feature-settings: 'liga' 1, 'calt' 1 !important;
          -webkit-font-feature-settings: 'liga' 1, 'calt' 1 !important;
          text-rendering: optimizeLegibility !important;
        }

        /* Edit highlight */
        [contenteditable="true"]:focus {
          outline: 2px dashed ${T.gold} !important;
          background-color: rgba(201,162,39,0.06) !important;
          border-radius: 4px;
        }
        .south_type_chart_cls, svg, iframe { pointer-events: none; }
      `;
      doc.head.appendChild(style);

      /* Replace header logo with new Astrodham logo */
      doc.querySelectorAll('.header-logo').forEach(logo => {
        logo.src             = `data:image/png;base64,${logoBase64}`;
        logo.style.height    = '56px';
        logo.style.width     = 'auto';
        logo.style.maxWidth  = '180px';
        logo.style.objectFit = 'contain';
      });

      /* ── Purge function: COMPLETELY REMOVES from DOM ── */
      const purgeUnwanted = () => {
        /* Completely remove Lord Ganesha div from DOM */
        doc.querySelectorAll('.astro_vastu_logo, .astro_vastu_logo_wrap')
          .forEach(el => el.parentNode && el.parentNode.removeChild(el));

        /* Completely remove Sri Astro Vastu copyright logo and its wrapper */
        doc.querySelectorAll('.copyright_logo').forEach(el => {
          const parent = el.closest('.copyright_logo_sec, .footer-logo-sec, .col, .d-flex') || el.parentElement;
          if (parent && parent.parentNode) parent.parentNode.removeChild(parent);
          else if (el.parentNode) el.parentNode.removeChild(el);
        });
      };

      purgeUnwanted(); // run immediately

      /* ── MutationObserver: kills any re-injection by template CDN JS ── */
      let purgeCount = 0;
      const observer = new iframe.contentWindow.MutationObserver(() => {
        purgeUnwanted();
        purgeCount++;
        if (purgeCount >= 5) observer.disconnect(); // stop after initial cleanup
      });
      observer.observe(doc.body, { childList: true, subtree: true, attributes: false });

      /* ── Sidebar Sync Observer ── */
      const sidebarNav = doc.querySelector('#sidebar');
      if (sidebarNav) sidebarNav.setAttribute('contenteditable', 'false');
      const sidebarBackdrop = doc.querySelector('#sidebarBackdrop');
      if (sidebarBackdrop) sidebarBackdrop.setAttribute('contenteditable', 'false');

      const textObserver = new iframe.contentWindow.MutationObserver((mutations) => {
        mutations.forEach(mut => {
          let target = mut.target;
          if (target.nodeType === 3) target = target.parentNode; // text node
          if (target && (target.classList.contains('sidebar-main-header') || target.classList.contains('sub-header'))) {
             const id = target.id;
             if (id) {
                const link = doc.querySelector('.sidebar-nav a[href="#' + id + '"] span, .sidebar-nav a[href="#' + id + '"]');
                if (link) {
                   const icon = link.querySelector('i');
                   link.innerHTML = '';
                   if (icon) link.appendChild(icon);
                   link.appendChild(doc.createTextNode(' ' + target.innerText.trim()));
                }
             }
          }
        });
      });
      textObserver.observe(doc.body, { characterData: true, childList: true, subtree: true });

      /* ── Image Click Listener for Replacer ── */
      doc.addEventListener('click', (e) => {
        if (e.target && e.target.tagName && e.target.tagName.toLowerCase() === 'img') {
          // Ignore generated chart SVGs and the newly injected Ganesha
          if (e.target.classList.contains('south_type_chart_cls') || e.target.classList.contains('new_ganesha_img')) return;
          
          e.preventDefault();
          e.stopPropagation();
          selectedImgRef.current = e.target;
          setIsImagePickerOpen(true);
        }
      }, true);

      setIframeLoaded(true);
    } catch (e) { console.error('Iframe load error', e); }
  };

  /* 7. Export clean standalone HTML */
  const exportHtmlReport = () => {
    const iframe = iframeRef.current;
    if (!iframe) return;
    try {
      let doc = iframe.contentDocument.documentElement.outerHTML;
      doc = doc.replaceAll('contenteditable="true"',  '');
      doc = doc.replaceAll('contenteditable="false"', '');

      /* Embed local images as base64 for fully offline file */
      doc = doc.replaceAll('/zodiac_wheel.png', `data:image/png;base64,${zodiacBase64}`);

      let name = 'Kundli_Report';
      const el = iframe.contentDocument.querySelector('.name_txt');
      if (el?.innerText) name = el.innerText.trim().replace(/\s+/g, '_');

      const blob = new Blob([doc], { type: 'text/html;charset=utf-8;' });
      const a    = document.createElement('a');
      a.href     = URL.createObjectURL(blob);
      a.download = `ASTRODHAM_Report_${name}.html`;
      a.click();
    } catch (e) { console.error('Export error', e); }
  };

  /* ── Render ─────────────────────────────────────────────────── */
  return (
    <div style={{ background: T.shellBg }} className="min-h-screen text-white flex flex-col font-sans overflow-hidden">

      {/* ══ 1. HEADER ══════════════════════════════════════════ */}
      <header
        style={{ background: `linear-gradient(135deg, ${T.purpleDark} 0%, ${T.purple} 100%)`, borderBottom: `1px solid ${T.shellBorder}` }}
        className="h-16 px-6 flex items-center justify-between shadow-2xl z-50 sticky top-0"
      >
        {/* Logo + Title */}
        <div className="flex items-center gap-3">
          <div style={{ background: T.white }} className="h-12 px-2 rounded-xl shadow-lg flex items-center justify-center">
            <img src={`data:image/png;base64,${logoBase64}`} style={{ height: '44px', width: 'auto', maxWidth: '160px', objectFit: 'contain' }} alt="Astrodham" />
          </div>
          <div>
            <h1 style={{ color: T.gold }} className="font-extrabold text-sm tracking-widest leading-none">
              ASTRODHAM TEMPLATE EDITOR
            </h1>
            <p style={{ color: T.amber }} className="text-[10px] font-bold mt-1 uppercase tracking-wider">
              Premium Astrology Report Customizer
            </p>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-3">
          <span
            style={{ background: T.shellCard, color: T.gold, border: `1px solid ${T.shellBorder}` }}
            className="text-[10px] font-black uppercase tracking-wider px-3.5 py-1.5 rounded-full flex items-center gap-1.5"
          >
            <Edit3 size={11} /> DIRECT EDIT ACTIVE
          </span>

          <button
            onClick={() => setIsDrawerOpen(!isDrawerOpen)}
            style={isDrawerOpen
              ? { background: T.purple, border: `1px solid ${T.gold}`, color: T.gold }
              : { background: T.shellCard, color: '#c4b0e8', border: `1px solid ${T.shellBorder}` }
            }
            className="px-5 py-2 rounded-full font-bold text-xs uppercase tracking-wider flex items-center gap-2 transition-all"
          >
            <Compass size={14} /> Update Charts {isDrawerOpen ? <ChevronUp size={12} /> : <ChevronDown size={12} />}
          </button>

          <button
            onClick={() => { iframeRef.current?.contentWindow?.focus(); iframeRef.current?.contentWindow?.print(); }}
            style={{ background: T.shellCard, color: '#c4b0e8', border: `1px solid ${T.shellBorder}` }}
            className="px-4 py-2 rounded-full font-bold text-xs uppercase tracking-wider flex items-center gap-2 transition-all hover:brightness-125"
          >
            <Printer size={14} /> Print / PDF
          </button>

          <button
            onClick={exportHtmlReport}
            style={{ background: `linear-gradient(135deg, ${T.purple}, ${T.gold})` }}
            className="px-6 py-2 rounded-full font-bold text-xs uppercase tracking-widest flex items-center gap-2 shadow-lg text-white transition-all hover:brightness-110"
          >
            <Download size={14} /> Export Clean HTML
          </button>
        </div>
      </header>

      {/* ══ 2. CHART EDITOR DRAWER ══════════════════════════════ */}
      {isDrawerOpen && (
        <div
          style={{ background: T.shellPanel, borderBottom: `1px solid ${T.shellBorder}` }}
          className="p-6 z-40 shadow-inner flex flex-col gap-6 animate-slideDown"
        >
          {/* Drawer header */}
          <div style={{ borderBottom: `1px solid ${T.shellBorder}` }} className="flex justify-between items-center pb-3">
            <div className="flex items-center gap-2">
              <Compass style={{ color: T.gold }} size={20} />
              <h2 style={{ color: '#d4bfef' }} className="text-sm font-black tracking-widest uppercase">
                Astrological Birth Charts Editor
              </h2>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={saveState}
                className="bg-emerald-700 hover:bg-emerald-600 text-white text-[11px] font-bold uppercase tracking-wider px-4 py-1.5 rounded-lg flex items-center gap-1.5 transition-all"
              >
                {isSaved ? <Check size={12} /> : <RefreshCw size={12} />}
                {isSaved ? 'Saved!' : 'Save Config'}
              </button>
              <button
                onClick={() => setIsDrawerOpen(false)}
                style={{ color: '#a08cc0' }}
                className="hover:text-white text-xs font-bold px-3 py-1"
              >
                Close ✕
              </button>
            </div>
          </div>

          {/* Drawer body */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">

            {/* Column 1: Tab + Lagna selector */}
            <div className="flex flex-col gap-2">
              <label style={{ color: '#a08cc0' }} className="text-[10px] font-black uppercase tracking-widest">Select Active Chart</label>
              {[
                { id: 'd1',   label: 'Lagna Chart (D1) — లగ్న చక్రం' },
                { id: 'moon', label: 'Moon Chart — చంద్ర చక్రం' },
                { id: 'd9',   label: 'Navamsha Chart (D9) — నవాంశ చక్రం' },
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setActiveChartTab(tab.id)}
                  style={activeChartTab === tab.id
                    ? { background: T.purple, border: `1px solid ${T.gold}`, color: T.gold }
                    : { background: T.shellCard, border: `1px solid ${T.shellBorder}`, color: '#c4b0e8' }
                  }
                  className="w-full text-left p-3.5 rounded-xl font-bold text-xs uppercase tracking-wider transition-all"
                >
                  {tab.label}
                </button>
              ))}

              <div style={{ background: T.shellCard, border: `1px solid ${T.shellBorder}` }} className="mt-4 p-3 rounded-xl">
                <label style={{ color: '#a08cc0' }} className="text-[10px] font-black block mb-2 uppercase tracking-widest">Lagna Starting Zodiac Sign</label>
                <select
                  style={{ background: T.shellPanel, border: `1px solid ${T.shellBorder}`, color: T.gold }}
                  className="w-full rounded-lg p-2.5 font-bold text-xs focus:outline-none"
                  value={chartsLagna[activeChartTab]}
                  onChange={e => setChartsLagna(prev => ({ ...prev, [activeChartTab]: e.target.value }))}
                >
                  {SIGN_NAMES_TELUGU.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
            </div>

            {/* Column 2: Houses grid */}
            <div style={{ background: T.shellCard, border: `1px solid ${T.shellBorder}` }} className="lg:col-span-2 p-4 rounded-2xl">
              <label style={{ color: '#a08cc0' }} className="text-[10px] font-black block mb-3 uppercase tracking-widest">
                Houses 1–12 — Enter Planets / Signs
              </label>
              <div className="grid grid-cols-3 md:grid-cols-4 gap-3">
                {charts[activeChartTab].map((val, idx) => (
                  <div key={idx} style={{ background: T.shellPanel, border: `1px solid ${T.shellBorder}` }} className="p-2 rounded-xl">
                    <label style={{ color: '#a08cc0' }} className="text-[9px] font-black block mb-1">House {idx + 1}</label>
                    <input
                      style={{ background: T.shellCard, border: `1px solid ${T.shellBorder}`, color: T.gold }}
                      className="w-full rounded px-2 py-1 text-xs font-bold focus:outline-none"
                      placeholder="e.g. Su, Mo"
                      value={val}
                      onChange={e => {
                        const v = e.target.value;
                        setCharts(prev => ({
                          ...prev,
                          [activeChartTab]: prev[activeChartTab].map((x, i) => i === idx ? v : x),
                        }));
                      }}
                    />
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ══ 3. LIVE IFRAME PREVIEW ══════════════════════════════ */}
      <main style={{ background: '#08031a' }} className="flex-1 w-full h-full relative">
        {/* Floating hint banner */}
        <div
          style={{ background: `${T.purple}ee`, border: `1px solid ${T.gold}55`, color: T.white }}
          className="absolute top-4 left-1/2 -translate-x-1/2 backdrop-blur text-[11px] font-black px-6 py-2 rounded-full shadow-2xl z-20 pointer-events-none uppercase tracking-widest flex items-center gap-2"
        >
          <Edit3 size={12} style={{ color: T.gold }} />
          Click anywhere on the text below to edit it!
        </div>

        <iframe
          id="preview-iframe"
          ref={iframeRef}
          srcDoc={INITIAL_IFRAME_HTML}
          onLoad={handleIframeLoad}
          className="w-full border-none bg-white shadow-inner"
          style={{ height: 'calc(100vh - 4rem)' }}
          title="Astrodham Live Editable Template"
        />
      </main>

      {/* ══ 4. IMAGE REPLACER MODAL ══════════════════════════════ */}
      {isImagePickerOpen && (
        <div className="fixed inset-0 bg-black/60 z-[9999] flex items-center justify-center p-4">
          <div className="bg-[#FAF8FF] rounded-xl shadow-2xl w-full max-w-4xl flex flex-col max-h-[90vh] overflow-hidden border-2" style={{ borderColor: T.purple }}>
            {/* Modal Header */}
            <div className="p-4 flex justify-between items-center" style={{ background: T.purple }}>
              <h3 className="text-white font-bold text-lg flex items-center gap-2">
                <Compass size={20} />
                Select Replacement Image
              </h3>
              <button 
                onClick={() => setIsImagePickerOpen(false)} 
                className="text-white/70 hover:text-white font-bold text-xl px-2"
              >
                ✕
              </button>
            </div>
            
            {/* Search Box & Add New */}
            <div className="p-4 border-b bg-white flex flex-col gap-3">
              <div className="flex items-center gap-3">
                <Search className="text-gray-400" size={20} />
                <input
                  type="text"
                  placeholder="Search images (e.g., Aquarius, Ruby, Surya)..."
                  className="w-full p-2 outline-none text-lg text-[#3D0C6E]"
                  value={imageSearchQuery}
                  onChange={(e) => setImageSearchQuery(e.target.value)}
                  autoFocus
                />
              </div>
              
              <div className="flex gap-2 items-center bg-gray-50 p-2 rounded-lg border border-gray-200">
                <input 
                  type="text" 
                  placeholder="Image Name" 
                  className="p-1.5 px-3 rounded border text-sm w-1/3 outline-none focus:border-[#3D0C6E] text-[#3D0C6E]"
                  value={newImgName}
                  onChange={e => setNewImgName(e.target.value)}
                />
                <input 
                  type="text" 
                  placeholder="Image URL (https://...)" 
                  className="p-1.5 px-3 rounded border text-sm flex-1 outline-none focus:border-[#3D0C6E] text-[#3D0C6E]"
                  value={newImgUrl}
                  onChange={e => setNewImgUrl(e.target.value)}
                />
                <button 
                  className="bg-[#3D0C6E] text-white p-1.5 px-4 rounded text-sm font-bold hover:bg-[#2E1258] flex items-center gap-1 transition-all"
                  onClick={() => {
                    if(newImgName && newImgUrl) {
                      saveCustomImages([{name: newImgName.trim(), url: newImgUrl.trim()}, ...customImages]);
                      setNewImgName('');
                      setNewImgUrl('');
                    }
                  }}
                >
                  <Plus size={16} /> ADD
                </button>
              </div>
            </div>
            
            {/* Image Grid */}
            <div className="p-4 overflow-y-auto grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
              {customImages.filter(img => img.name.toLowerCase().includes(imageSearchQuery.toLowerCase())).map((img, i) => (
                <div 
                  key={i} 
                  className="bg-white p-3 rounded-lg shadow-sm cursor-pointer hover:shadow-md transition-all flex flex-col items-center border-2 border-transparent relative group"
                  style={{ borderColor: 'transparent' }}
                  onMouseEnter={(e) => e.currentTarget.style.borderColor = T.gold}
                  onMouseLeave={(e) => e.currentTarget.style.borderColor = 'transparent'}
                  onClick={(e) => {
                    // Prevent select if clicking delete
                    if (e.target.closest('button')) return;
                    if (selectedImgRef.current) {
                      selectedImgRef.current.src = img.url;
                      selectedImgRef.current.style.objectFit = 'contain';
                      
                      selectedImgRef.current.style.width = 'auto';
                      selectedImgRef.current.style.height = 'auto';
                      selectedImgRef.current.style.maxWidth = '100%';
                      selectedImgRef.current.style.maxHeight = '200px';

                      setIsImagePickerOpen(false);
                      selectedImgRef.current = null;
                    }
                  }}
                >
                  <button 
                    className="absolute top-1 right-1 p-1 bg-red-500 text-white rounded hover:bg-red-600 transition-all shadow"
                    title="Delete Image"
                    onClick={(e) => {
                      e.stopPropagation();
                      saveCustomImages(customImages.filter(x => x !== img));
                    }}
                  >
                    <Trash2 size={14} />
                  </button>

                  <div className="h-24 w-full flex items-center justify-center mb-3">
                    <img src={img.url} alt={img.name} className="max-h-full max-w-full object-contain" />
                  </div>
                  <span className="text-xs text-center font-bold truncate w-full" style={{ color: T.purple }}>
                    {img.name}
                  </span>
                </div>
              ))}
              
              {customImages.filter(img => img.name.toLowerCase().includes(imageSearchQuery.toLowerCase())).length === 0 && (
                <div className="col-span-full text-center py-10 text-gray-400 italic">
                  No matching images found.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
