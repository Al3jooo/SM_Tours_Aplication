import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { SesionProvider } from '../features/auth/SesionProvider'
import { DisenoPrincipal } from './DisenoPrincipal'
import { InicioProvisionalPage } from './InicioProvisionalPage'
import { RutaProtegida } from './RutaProtegida'

/** El router (BrowserRouter o MemoryRouter en pruebas) lo aporta quien monta la App. */
export function App() {
  return (
    <SesionProvider>
      <Routes>
        <Route element={<DisenoPrincipal />}>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<RutaProtegida />}>
            <Route index element={<InicioProvisionalPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </SesionProvider>
  )
}
