import type { Config } from 'tailwindcss'

const config: Config = {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50:  '#F0FDFA',
          400: '#14B8A6',
          500: '#0D9488',
          600: '#0F766E',
        },
        status: {
          pending:   '#D97706',
          confirmed: '#2563EB',
          present:   '#7C3AED',
          done:      '#16A34A',
          cancelled: '#4B5563',
          noshow:    '#DC2626',
        },
        'status-bg': {
          pending:   '#FFFBEB',
          confirmed: '#EFF6FF',
          present:   '#F5F3FF',
          done:      '#F0FDF4',
          cancelled: '#F9FAFB',
          noshow:    '#FEF2F2',
        },
        success: '#16A34A',
        error:   '#DC2626',
        warning: '#D97706',
        info:    '#2563EB',
      },
      boxShadow: {
        sm: '0 1px 2px rgba(0,0,0,0.05)',
        md: '0 4px 6px rgba(0,0,0,0.07)',
        lg: '0 10px 15px rgba(0,0,0,0.10)',
        xl: '0 20px 25px rgba(0,0,0,0.12)',
      },
      borderRadius: {
        'radius-sm':   '4px',
        'radius-md':   '8px',
        'radius-lg':   '12px',
        'radius-xl':   '16px',
        'radius-full': '9999px',
      },
      fontFamily: {
        sans:  ['Inter', 'sans-serif'],
        brand: ['DM Sans', 'sans-serif'],
      },
    },
  },
  plugins: [],
}

export default config
