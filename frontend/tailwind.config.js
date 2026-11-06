/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        // ─────────────────────────────────────────────────────────────
        // Tokens semánticos (estilo shadcn/ui). Los valores viven como
        // variables CSS en src/index.css, así que basta cambiarlos ahí
        // para retonar toda la app: bg-background, text-foreground,
        // bg-primary, text-muted-foreground, border-border…
        // ─────────────────────────────────────────────────────────────
        border: 'hsl(var(--border) / <alpha-value>)',
        input: 'hsl(var(--input) / <alpha-value>)',
        ring: 'hsl(var(--ring) / <alpha-value>)',
        background: 'hsl(var(--background) / <alpha-value>)',
        foreground: 'hsl(var(--foreground) / <alpha-value>)',
        primary: {
          DEFAULT: 'hsl(var(--primary) / <alpha-value>)',
          foreground: 'hsl(var(--primary-foreground) / <alpha-value>)',
          hover: 'hsl(var(--primary-hover) / <alpha-value>)',
        },
        // Cian brillante: CTA del hero, anillos de foco y destellos.
        // Lleva texto marino encima (6.27:1), nunca texto blanco.
        brillo: {
          DEFAULT: 'hsl(var(--brillo) / <alpha-value>)',
          foreground: 'hsl(var(--brillo-foreground) / <alpha-value>)',
          hover: 'hsl(var(--brillo-hover) / <alpha-value>)',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary) / <alpha-value>)',
          foreground: 'hsl(var(--secondary-foreground) / <alpha-value>)',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted) / <alpha-value>)',
          foreground: 'hsl(var(--muted-foreground) / <alpha-value>)',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent) / <alpha-value>)',
          foreground: 'hsl(var(--accent-foreground) / <alpha-value>)',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive) / <alpha-value>)',
          foreground: 'hsl(var(--destructive-foreground) / <alpha-value>)',
        },
        success: {
          DEFAULT: 'hsl(var(--success) / <alpha-value>)',
          foreground: 'hsl(var(--success-foreground) / <alpha-value>)',
        },
        warning: {
          DEFAULT: 'hsl(var(--warning) / <alpha-value>)',
          foreground: 'hsl(var(--warning-foreground) / <alpha-value>)',
        },
        card: {
          DEFAULT: 'hsl(var(--card) / <alpha-value>)',
          foreground: 'hsl(var(--card-foreground) / <alpha-value>)',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover) / <alpha-value>)',
          foreground: 'hsl(var(--popover-foreground) / <alpha-value>)',
        },

        // ─────────────────────────────────────────────────────────────
        // Escalas de marca. `aqua` ahora es azul marino profundo y
        // `cian` el turquesa de acción. Se conservan (y `sol` también)
        // para que las pantallas del panel que aún usan aqua-500 /
        // btn-sol sigan funcionando mientras se migran a los tokens.
        // ─────────────────────────────────────────────────────────────
        aqua: {
          50:  '#eff8fb',
          100: '#ceeaf3',
          200: '#a0d2e3',
          300: '#6eb3cf',
          400: '#3484b2',
          500: '#175282',
          600: '#0f3961',
          700: '#0b2b4c',
          800: '#07203b',
          900: '#051629',
        },
        cian: {
          50:  '#ecfcfe',
          100: '#c6f4fa',
          200: '#84e6f5',
          300: '#3fd8f3',
          400: '#0cbcdf',
          500: '#0986a5',
          600: '#096b86',
          700: '#09556d',
          800: '#084154',
          900: '#072e3c',
        },
        sol: {
          50:  '#fef6e0',
          100: '#fde7b0',
          200: '#fac775',
          300: '#f4b53f',
          400: '#ef9f27',
          500: '#d9871a',
          600: '#a86711',
          700: '#7c4c0d',
          800: '#513208',
          900: '#2e1c04',
        },
        tinta: {
          50:  '#f5f7fa',
          100: '#e9edf2',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
        },
      },

      borderRadius: {
        lg: 'var(--radius)',                      // 0.75rem
        xl: 'calc(var(--radius) + 0.25rem)',      // 1rem
        '2xl': 'calc(var(--radius) + 0.75rem)',   // 1.5rem
        '3xl': 'calc(var(--radius) + 1.25rem)',   // 2rem
      },

      fontFamily: {
        sans: ['Poppins', 'system-ui', '-apple-system', 'sans-serif'],
      },

      // Sombras suaves y azuladas, no grises: la carta blanca flota
      // sobre un fondo agua sin que se vea un halo sucio.
      boxShadow: {
        agua: '0 1px 2px 0 hsl(var(--sombra) / 0.04), 0 4px 16px -4px hsl(var(--sombra) / 0.10)',
        'agua-md': '0 2px 4px 0 hsl(var(--sombra) / 0.04), 0 12px 28px -8px hsl(var(--sombra) / 0.16)',
        'agua-lg': '0 4px 8px 0 hsl(var(--sombra) / 0.05), 0 24px 48px -12px hsl(var(--sombra) / 0.22)',
        brillo: '0 8px 28px -6px hsl(var(--brillo) / 0.45)',
      },

      transitionTimingFunction: {
        // Entra rápido y frena largo, como el agua al asentarse.
        agua: 'cubic-bezier(0.22, 1, 0.36, 1)',
      },

      keyframes: {
        'ola-desliz': {
          from: { transform: 'translateX(0)' },
          to:   { transform: 'translateX(-50%)' },
        },
        flotar: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%':      { transform: 'translateY(-8px)' },
        },
        'aparecer-abajo': {
          from: { opacity: '0', transform: 'translateY(12px)' },
          to:   { opacity: '1', transform: 'none' },
        },
        'aparecer-escala': {
          from: { opacity: '0', transform: 'scale(0.96)' },
          to:   { opacity: '1', transform: 'none' },
        },
        'pulso-brillo': {
          '0%, 100%': { boxShadow: '0 0 0 0 hsl(var(--brillo) / 0.45)' },
          '50%':      { boxShadow: '0 0 0 10px hsl(var(--brillo) / 0)' },
        },
      },
      animation: {
        'ola-lenta': 'ola-desliz 22s linear infinite',
        'ola-rapida': 'ola-desliz 14s linear infinite',
        flotar: 'flotar 4s ease-in-out infinite',
        'aparecer-abajo': 'aparecer-abajo 0.4s cubic-bezier(0.22, 1, 0.36, 1) both',
        'aparecer-escala': 'aparecer-escala 0.3s cubic-bezier(0.22, 1, 0.36, 1) both',
        'pulso-brillo': 'pulso-brillo 2.4s ease-out infinite',
      },
    },
  },
  plugins: [],
}
