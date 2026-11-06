import { defineConfig } from 'vite'
// También carga los tipos de la sección test de la configuración.
import { configDefaults } from 'vitest/config'
import react from '@vitejs/plugin-react'

/** Pruebas que cambian la zona horaria del proceso (process.env.TZ): corren fuera de vmThreads (ver test.projects). */
const ZONA_HORARIA = [
  'src/features/agenda/__tests__/fechasEvento.test.ts',
  'src/features/reservas/__tests__/fechasBogota.test.ts',
]

export default defineConfig({
  plugins: [react()],
  server: {
    // El código usa siempre URL relativas (/api/v1/...). Todo entra por el API Gateway (8080), que enruta
    // /api/v1/auth a ms-auth y /api/v1/catalogo a ms-catalog (ADR-005); el proxy evita CORS en desarrollo.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    rolldownOptions: {
      output: {
        // Leaflet en su propio archivo (solo lo pide /explorar, que se carga de forma diferida): no engorda el archivo
        // principal y queda en caché aunque cambie el código de la página.
        codeSplitting: { groups: [{ name: 'leaflet', test: /node_modules[\\/]leaflet[\\/]/ }] },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
    restoreMocks: true,
    unstubGlobals: true,
    // Vitest vacía los .css por defecto; la guardia de contraste (src/app/tokens.test.ts) lee los tokens.
    css: { include: [/estilos\.css/] },
    // O6 (DEC-101): la primera prueba de cada archivo paga la carga diferida de su pantalla (Vitest transforma el
    // módulo dentro de la prueba) y, con toda la suite en paralelo, pasa de los 5 s por defecto en las PC del equipo.
    // 30 s es el límite con el que la suite completa pasó en todas las corridas medidas; una prueba colgada igual falla.
    testTimeout: 30_000,
    // Dos proyectos con la misma configuración (extends: true). Casi todo corre en vmThreads, que reutiliza el
    // entorno jsdom de cada hilo en vez de crearlo por archivo: la suite pasa de ~85 s a ~35 s. Las pruebas que
    // cambian process.env.TZ van en forks (un proceso por archivo), porque dentro de una VM el cambio de zona horaria no
    // llega a Date. Una prueba nueva que cambie TZ se agrega a ZONA_HORARIA.
    projects: [
      { extends: true, test: { name: 'vm', pool: 'vmThreads', exclude: [...configDefaults.exclude, ...ZONA_HORARIA] } },
      { extends: true, test: { name: 'zona-horaria', pool: 'forks', include: ZONA_HORARIA } },
    ],
  },
})
