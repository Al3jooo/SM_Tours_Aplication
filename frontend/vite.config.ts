/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // El código usa siempre URL relativas (/api/v1/...): en desarrollo el proxy evita CORS, y
    // más adelante basta con apuntar este destino al API Gateway sin tocar el código.
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    restoreMocks: true,
    unstubGlobals: true,
    // Vitest vacía los .css por defecto; la guardia de contraste (src/app/tokens.test.ts) lee los tokens.
    css: { include: [/estilos\.css/] },
  },
})
