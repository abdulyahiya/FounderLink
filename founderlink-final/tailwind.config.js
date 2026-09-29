/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // Dark surface stack (legacy — remapped to design tokens via CSS)
        dark: {
          900: '#0a0a0a',
          800: '#141414',
          700: '#1e1e1e',
          600: '#2a2a2a',
          500: '#3a3a3a',
          400: '#525252',
          300: '#737373',
        },
        // Brand: emerald green
        brand: {
          50:  '#ecfdf5',
          100: '#d1fae5',
          200: '#a7f3d0',
          300: '#6ee7b7',
          400: '#34d399',
          500: '#10b981',
          600: '#059669',
          DEFAULT: '#059669',
          700: '#047857',
          800: '#065f46',
          900: '#064e3b',
        },
        // Accent — maps to brand (replaces old iOS blue in feature files)
        accent: {
          DEFAULT: '#059669',
          hover:   '#047857',
          light:   '#10b981',
        },
      },
      fontFamily: {
        display: ["'Syne'", 'system-ui', 'sans-serif'],
        sans: ["'DM Sans'", 'system-ui', 'sans-serif'],
      },
      letterSpacing: {
        tightest: '-0.04em',
        tighter:  '-0.03em',
        tight:    '-0.02em',
      },
    },
  },
  plugins: [],
};
