// ESLint (configuración plana): errores comunes de JavaScript y TypeScript y las reglas de los hooks de React.
// Los tipos los sigue verificando tsc (npm run build); aquí no se usa el análisis con tipos, para que sea rápido.
import js from '@eslint/js'
import reactHooks from 'eslint-plugin-react-hooks'
import globals from 'globals'
import tseslint from 'typescript-eslint'

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'node_modules'] },
  {
    files: ['**/*.{ts,tsx}'],
    extends: [js.configs.recommended, tseslint.configs.recommended],
    plugins: { 'react-hooks': reactHooks },
    languageOptions: {
      ecmaVersion: 2023,
      globals: globals.browser,
    },
    rules: {
      // Las dos reglas clásicas de los hooks. Las reglas nuevas para el React Compiler (refs, set-state-in-effect,
      // purity…) quedan fuera por ahora: marcan patrones que hoy funcionan y adoptarlas implica refactorizar.
      'react-hooks/rules-of-hooks': 'error',
      'react-hooks/exhaustive-deps': 'warn',
      // Convención del proyecto: un nombre que empieza con _ se descarta a propósito (p. ej. al quitar un campo con
      // la desestructuración `{ campo: _campo, ...resto }`).
      '@typescript-eslint/no-unused-vars': [
        'error',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_', destructuredArrayIgnorePattern: '^_' },
      ],
    },
  },
  {
    files: ['scripts/**/*.mjs', '*.config.{js,ts}'],
    extends: [js.configs.recommended],
    languageOptions: {
      globals: globals.node,
    },
  },
)
