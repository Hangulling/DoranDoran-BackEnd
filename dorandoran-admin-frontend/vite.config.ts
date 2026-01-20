import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001, // 어드민 전용 포트
  },
  build: {
    outDir: 'dist',
  },
})
