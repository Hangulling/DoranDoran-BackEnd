module.exports = {
  mode: 'jit',
  darkMode: false,
  content: ['./src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    screens: {
      sm: '480px',
      md: '768px',
      lg: '1025px',
      xl: '1280px',
      xl2: '1360px',
    },
    extend: {
      lineHeight: {
        150: '1.5', // 150% 행간 추가
      },
    },
  },
  plugins: [require('daisyui')],

  daisyui: {
    themes: ['light'], // 'light' 테마만 사용
    darkTheme: light,
  },
}
