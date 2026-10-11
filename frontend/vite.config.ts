/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    // El código usa siempre URL relativas (/api/v1/...). Todo entra por el API Gateway (8080), que enruta
    // /api/v1/auth a ms-auth y /api/v1/catalogo a ms-catalog (ADR-005); el proxy evita CORS en desarrollo.
    proxy: {
      '/api': 'http://localhost:8080',
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
