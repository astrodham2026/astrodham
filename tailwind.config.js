/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        purple: {
          primary:  '#3D0C6E',
          dark:     '#25074A',
          mid:      '#5A1796',
          light:    '#EDE0F8',
        },
        gold: {
          DEFAULT: '#C9A227',
          light:   '#F5E6B0',
        },
        copper:  '#B87333',
        amber:   '#CC8822',
        shell: {
          bg:     '#0E0520',
          panel:  '#1A0B36',
          card:   '#240D47',
          hover:  '#2E1258',
        },
      },
      keyframes: {
        slideDown: {
          from: { opacity: '0', transform: 'translateY(-16px)' },
          to:   { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        slideDown: 'slideDown 0.25s ease both',
      },
      fontFamily: {
        sans: ['Inter', 'Noto Sans Telugu', 'ui-sans-serif', 'system-ui'],
      },
    },
  },
  plugins: [],
}
