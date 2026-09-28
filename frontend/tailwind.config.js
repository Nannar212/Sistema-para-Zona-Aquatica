/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Paleta Zona Aquática — azul / blanco / amarillo / negro
        aqua: {
          50:  '#e6f1fb',
          100: '#b5d4f4',
          200: '#85b7eb',
          300: '#57a0e0',
          400: '#378add',
          500: '#185fa5',
          600: '#0c447c',
          700: '#093763',
          800: '#062949',
          900: '#031a30',
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
      fontFamily: {
        sans: ['Poppins', 'system-ui', '-apple-system', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
