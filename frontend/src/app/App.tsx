import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { SesionProvider } from '../features/auth/SesionProvider'
import { ExplorarPage } from '../features/catalogo/ExplorarPage'
import { InicioPage } from '../features/catalogo/InicioPage'
import { DisenoPrincipal } from './DisenoPrincipal'

/**
 * El router (BrowserRouter o MemoryRouter en pruebas) lo aporta quien monta la App.
 * Inicio (WF-04) y exploración (WF-05) son públicas. Las pantallas privadas futuras (prestador, gestor) irán dentro
 * de <Route element={<RutaProtegida />}>.
 */
export function App() {
  return (
    <SesionProvider>
      <Routes>
        <Route element={<DisenoPrincipal />}>
          <Route index element={<InicioPage />} />
          <Route path="/explorar" element={<ExplorarPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </SesionProvider>
  )
}
