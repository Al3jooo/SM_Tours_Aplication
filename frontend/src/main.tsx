import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
// Fuentes empaquetadas con la app (OFL-1.1): no se cargan desde internet.
import '@fontsource-variable/fraunces'
import '@fontsource-variable/figtree'
import './i18n'
import './app/estilos.css'
import { App } from './app/App'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </StrictMode>,
)
