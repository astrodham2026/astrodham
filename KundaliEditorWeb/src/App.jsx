import React, { useState, useEffect } from 'react';
import { 
  Download, 
  Eye, 
  Edit3, 
  RotateCcw,
  Award,
  Star
} from 'lucide-react';

const STORAGE_KEY = 'ASTRODHAM_STEP_2';

const INITIAL_STATE = {
  basic: {
    reportId: "AD-2026-001",
    name: "Rahul Sharma",
    dob: "15-08-1992",
    tob: "14:30:00",
    pob: "New Delhi, India",
    ayanamsa: "Lahiri (23° 45' 12\")",
    title: "ప్రీమియం జీవిత కుండలి నివేదిక"
  }
};

/**
 * Lord Ganesh SVG - Refined Golden Design
 */
const LordGaneshSVG = () => (
  <svg width="180" height="200" viewBox="0 0 200 220" fill="none" className="drop-shadow-2xl">
    {/* Head/Body Outline */}
    <path d="M100 20C125 20 145 40 145 70C145 100 100 125 100 125C100 125 55 100 55 70C55 40 75 20 100 20Z" stroke="#D4AF37" strokeWidth="2.5" />
    <path d="M55 70C35 70 25 90 25 110C25 140 50 170 100 170C150 170 175 140 175 110C175 90 165 70 145 70" stroke="#D4AF37" strokeWidth="2.5" strokeLinecap="round" />
    
    {/* Trunk Details */}
    <path d="M100 125V175M85 175H115" stroke="#D4AF37" strokeWidth="2.5" strokeLinecap="round" />
    <path d="M90 185C75 195 65 210 100 215C135 210 125 195 110 185" stroke="#D4AF37" strokeWidth="2.5" strokeLinecap="round" />
    
    {/* Ears */}
    <path d="M175 70 Q 205 70 205 115 Q 205 160 175 170" stroke="#D4AF37" strokeWidth="1.5" fill="none" />
    <path d="M25 70 Q -5 70 -5 115 Q -5 160 25 170" stroke="#D4AF37" strokeWidth="1.5" fill="none" />
    
    {/* Tilak/Eye */}
    <circle cx="100" cy="75" r="5" fill="#D4AF37" />
    <path d="M90 65 Q 100 55 110 65" stroke="#D4AF37" strokeWidth="1" />
    
    {/* Decorative Lines */}
    <path d="M70 40 Q 100 30 130 40" stroke="#D4AF37" strokeWidth="1" opacity="0.6" />
    <path d="M80 140 L 120 140" stroke="#D4AF37" strokeWidth="1" opacity="0.6" />
  </svg>
);

/**
 * Zodiac Wheel SVG - Premium Gold/Bronze Theme
 */
const ZodiacWheelSVG = () => {
  const signs = ["CAPRICORN", "SAGITTARIUS", "SCORPIO", "LIBRA", "VIRGO", "LEO", "CANCER", "GEMINI", "TAURUS", "ARIES", "PISCES", "AQUARIUS"];
  return (
    <div className="relative group">
      <svg width="420" height="420" viewBox="0 0 500 500" className="transition-transform duration-[10s] linear animate-[spin_60s_linear_infinite]">
        <defs>
          <radialGradient id="goldGradient" cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor="#fdfc97" />
            <stop offset="50%" stopColor="#d4af37" />
            <stop offset="100%" stopColor="#8b4513" />
          </radialGradient>
        </defs>
        <circle cx="250" cy="250" r="245" fill="none" stroke="#d4af37" strokeWidth="1" opacity="0.3" />
        <circle cx="250" cy="250" r="235" fill="none" stroke="#d4af37" strokeWidth="4" />
        <circle cx="250" cy="250" r="225" fill="none" stroke="#d4af37" strokeWidth="0.5" opacity="0.5" />
        <circle cx="250" cy="250" r="170" fill="none" stroke="#d4af37" strokeWidth="1" opacity="0.4" />
        
        {[...Array(12)].map((_, i) => {
          const rad = (i * 30 - 90) * Math.PI / 180;
          return <line key={i} x1="250" y1="250" x2={250 + 235 * Math.cos(rad)} y2={250 + 235 * Math.sin(rad)} stroke="#d4af37" strokeWidth="0.8" opacity="0.4" />;
        })}
        
        {signs.map((name, i) => {
          const angle = i * 30 - 75;
          const rad = angle * Math.PI / 180;
          const x = 250 + 205 * Math.cos(rad);
          const y = 250 + 205 * Math.sin(rad);
          return (
            <text key={i} x={x} y={y} textAnchor="middle" transform={`rotate(${angle + 90}, ${x}, ${y})`} fontSize="10" fontWeight="900" fill="#8b4513" className="tracking-widest">
              {name}
            </text>
          );
        })}
        
        <circle cx="250" cy="250" r="45" fill="white" stroke="#d4af37" strokeWidth="2" className="shadow-lg" />
        <path d="M250 230 L 260 250 L 250 270 L 240 250 Z" fill="#d4af37" />
      </svg>
      {/* Non-rotating Center Star */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <Star size={32} fill="#d4af37" className="text-[#d4af37] drop-shadow-md" />
      </div>
    </div>
  );
};

export default function App() {
  const [data, setData] = useState(INITIAL_STATE);
  const [isEditMode, setIsEditMode] = useState(true);
  const [saveStatus, setSaveStatus] = useState('idle');

  useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try { setData(JSON.parse(saved)); } catch (e) { console.error("Restore failed", e); }
    }
  }, []);

  useEffect(() => {
    setSaveStatus('saving');
    const timer = setTimeout(() => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
      setSaveStatus('saved');
      setTimeout(() => setSaveStatus('idle'), 2000);
    }, 800);
    return () => clearTimeout(timer);
  }, [data]);

  const updateField = (category, field, value) => {
    setData(prev => ({
      ...prev,
      [category]: { ...prev[category], [field]: value }
    }));
  };

  return (
    <div className="min-h-screen bg-slate-200 font-sans text-slate-900 pb-20">
      
      {/* ADMIN CONTROL HEADER */}
      <header className="sticky top-0 z-50 bg-[#800080] text-white px-6 py-4 flex items-center justify-between shadow-2xl border-b border-white/10">
        <div className="flex items-center gap-4">
          <div className="w-10 h-10 bg-white rounded-full flex items-center justify-center text-[#800080] font-black shadow-inner">
            ॐ
          </div>
          <div>
            <h1 className="text-lg font-black tracking-widest uppercase">SRI ASTRO VASTU</h1>
            <p className="text-[9px] font-bold text-purple-200 tracking-tighter opacity-80 uppercase">Premium Kundali Visual Editor</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2 px-3 py-1 bg-black/20 rounded-full border border-white/10">
            <div className={`w-1.5 h-1.5 rounded-full ${saveStatus === 'saved' ? 'bg-green-400 shadow-[0_0_8px_#4ade80]' : 'bg-amber-400 animate-pulse'}`} />
            <span className="text-[10px] text-white/70 uppercase font-bold tracking-widest">
              {saveStatus === 'saved' ? 'Synced' : 'Auto-Saving'}
            </span>
          </div>

          <button 
            onClick={() => setIsEditMode(!isEditMode)}
            className={`flex items-center gap-2 px-6 py-2 rounded-full text-[11px] font-black uppercase tracking-widest shadow-lg transition-all active:scale-95 ${
              isEditMode ? 'bg-green-500 hover:bg-green-600' : 'bg-indigo-500 hover:bg-indigo-600'
            }`}
          >
            {isEditMode ? <><Eye size={14}/> View Result</> : <><Edit3 size={14}/> Edit Mode</>}
          </button>
        </div>
      </header>

      {/* REPORT CONTENT */}
      <main className="flex flex-col items-center pt-8">
        <div id="report-canvas" className="space-y-10">
          
          {/* PAGE 1: COVER PAGE (Reflecting Uploaded Image) */}
          <div className="report-page bg-white shadow-2xl relative flex flex-col items-center overflow-hidden">
            
            {/* Background Texture & Decoration */}
            <div className="absolute inset-0 bg-[#fffdf5] pointer-events-none" />
            <div className="absolute inset-0 opacity-[0.05] pointer-events-none" style={{ backgroundImage: 'radial-gradient(#8b008b 1px, transparent 1px)', backgroundSize: '60px 60px' }} />
            
            {/* Artistic Curves (Simplified SVG doodles) */}
            <svg className="absolute inset-0 w-full h-full opacity-10 pointer-events-none">
              <path d="M-100 100 Q 200 300 500 100 T 1100 300" stroke="#d4af37" fill="none" strokeWidth="2" />
              <path d="M-100 800 Q 300 600 600 800 T 1200 600" stroke="#d4af37" fill="none" strokeWidth="1" />
              <circle cx="10%" cy="20%" r="40" stroke="#d4af37" fill="none" opacity="0.5" />
              <circle cx="85%" cy="15%" r="60" stroke="#d4af37" fill="none" opacity="0.3" />
              <path d="M80% 80% L 85% 85% M 85% 80% L 80% 85%" stroke="#d4af37" strokeWidth="2" />
            </svg>

            <div className="absolute top-[-10%] right-[-10%] w-96 h-96 bg-yellow-50 rounded-full blur-[120px] opacity-60" />
            <div className="absolute bottom-[-5%] left-[-5%] w-80 h-80 bg-orange-50 rounded-full blur-[100px] opacity-60" />

            {/* Top Branding Bar */}
            <div className="w-full bg-[#8b008b] py-6 px-10 flex items-center justify-between z-10 shadow-lg">
                <div className="flex items-center gap-6">
                  <div className="w-16 h-16 bg-white rounded-full flex items-center justify-center p-1.5 shadow-xl border-2 border-white/20">
                     <div className="w-full h-full border-2 border-[#8b008b] rounded-full flex flex-col items-center justify-center font-bold text-[#8b008b] leading-none">
                        <span className="text-[10px] font-black tracking-widest">SRI</span>
                        <span className="text-xl font-black">ॐ</span>
                        <span className="text-[6px] font-bold">ASTRO VASTU</span>
                     </div>
                  </div>
                  <h2 className="text-white text-4xl font-black tracking-[0.25em] cinzel drop-shadow-md">SRI ASTRO VASTU</h2>
                </div>
                <div className="flex flex-col items-end opacity-40">
                   <div className="w-20 h-[2px] bg-white/30 mb-1" />
                   <div className="w-12 h-[2px] bg-white/30" />
                </div>
            </div>

            {/* Cover Body */}
            <div className="flex-1 flex flex-col items-center justify-between w-full py-12 z-10">
              
              {/* Devanagari Header */}
              <h3 className="text-[#8b008b] text-7xl font-black devanagari-text tracking-wider drop-shadow-sm">|| शुभ लाभ ||</h3>
              
              {/* Lord Ganesh */}
              <div className="my-4">
                <LordGaneshSVG />
              </div>

              {/* Zodiac Wheel */}
              <div className="relative flex items-center justify-center scale-110">
                <ZodiacWheelSVG />
                <div className="absolute inset-0 bg-gradient-to-tr from-orange-400/5 to-purple-400/5 rounded-full" />
              </div>

              {/* Footer Telugu Titles */}
              <div className="text-center mt-10">
                  {isEditMode ? (
                    <input 
                     className="text-center text-[#8b008b] text-6xl font-black telugu-text bg-transparent border-b border-purple-100 focus:border-purple-400 outline-none w-full max-w-3xl px-4 py-2"
                     value={data.basic.title}
                     onChange={(e) => updateField('basic', 'title', e.target.value)}
                    />
                  ) : (
                    <h4 className="text-[#8b008b] text-6xl font-black telugu-text tracking-[0.1em] leading-[1.4] drop-shadow-sm whitespace-pre-wrap">
                      {data.basic.title.split(' ').join('  ')}
                    </h4>
                  )}
                 
                 <div className="mt-12 flex items-center justify-center gap-10">
                    <div className="h-[2px] w-32 bg-gradient-to-r from-transparent via-[#8b008b]/40 to-transparent" />
                    <div className="text-[#8b008b] font-black text-2xl animate-bounce">✦</div>
                    <div className="h-[2px] w-32 bg-gradient-to-l from-transparent via-[#8b008b]/40 to-transparent" />
                 </div>

                 {/* Native Name in Edit Mode */}
                 <div className="mt-8">
                    {isEditMode ? (
                      <input 
                        className="text-center text-slate-500 text-3xl font-black uppercase tracking-[0.5em] bg-transparent border-none outline-none focus:ring-0 placeholder:opacity-30"
                        placeholder="RECIPIENT NAME"
                        value={data.basic.name}
                        onChange={(e) => updateField('basic', 'name', e.target.value)}
                      />
                    ) : (
                      <p className="text-slate-400 text-xl font-black uppercase tracking-[1.5em] transition-all duration-1000">
                        {data.basic.name}
                      </p>
                    )}
                 </div>
              </div>

            </div>

            {/* Bottom Credit */}
            <div className="absolute bottom-6 w-full text-center">
              <p className="text-[10px] font-black text-[#800080]/30 uppercase tracking-[0.8em] cinzel">ASTRODHAM PREMIUM TECHNOLOGY</p>
            </div>

          </div>

        </div>
      </main>

      {/* STYLES */}
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Cinzel:wght@700;900&family=Inter:wght@400;700;900&family=Noto+Serif+Telugu:wght@400;700;900&family=Noto+Sans+Devanagari:wght@400;700;900&display=swap');
        
        .cinzel { font-family: 'Cinzel', serif; }
        .telugu-text { font-family: 'Noto Serif Telugu', serif; }
        .devanagari-text { font-family: 'Noto Sans Devanagari', sans-serif; }
        
        .report-page {
          width: 210mm;
          height: 297mm;
          background: white;
          margin: 0 auto;
          box-sizing: border-box;
          transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
          transform-origin: top center;
        }

        @media (max-width: 950px) {
          .report-page { transform: scale(0.7); margin-bottom: -90mm; }
        }
        @media (max-width: 650px) {
          .report-page { transform: scale(0.45); margin-bottom: -160mm; }
        }
        
        @media print {
          body { background: white; padding: 0; }
          header { display: none !important; }
          .report-page { margin: 0; box-shadow: none; transform: none !important; border: none; }
          .pb-20 { padding-bottom: 0 !important; }
        }
      `}</style>
    </div>
  );
}
