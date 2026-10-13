import { Suspense, lazy } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { SesionProvider } from '../features/auth/SesionProvider'
import { EstadoCatalogo } from '../features/catalogo/componentes/EstadoCatalogo'
import { InicioPage } from '../features/catalogo/InicioPage'
import { LimiteErrores } from '../shared/components/LimiteErrores'
import { navegador } from '../shared/navegador'
import { DisenoAcceso } from './DisenoAcceso'
import { DisenoPrincipal } from './DisenoPrincipal'

/**
 * Exploración (WF-05) se carga de forma diferida: Leaflet y su CSS solo se descargan al entrar a /explorar. Mientras
 * llega, se ve el estado de carga del catálogo; si la descarga falla, un aviso con «Reintentar» (recarga la página,
 * porque React.lazy guarda el fallo). El encabezado, el pie y la sesión quedan fuera del límite de errores.
 */
const ExplorarPage = lazy(() => import('../features/catalogo/ExplorarPage').then((m) => ({ default: m.ExplorarPage })))

/**
 * El router (BrowserRouter o MemoryRouter en pruebas) lo aporta quien monta la App.
 * Inicio (WF-04) y exploración (WF-05) son públicas, con el diseño de la plataforma; el inicio de sesión (WF-01), con
 * el de acceso. Las pantallas privadas futuras (prestador, gestor) irán dentro de <Route element={<RutaProtegida />}>.
 */
export function App() {
  return (
    <SesionProvider>
      <Routes>
        <Route element={<DisenoPrincipal />}>
          <Route index element={<InicioPage />} />
          <Route
            path="/explorar"
            element={
              <LimiteErrores alFallar={<EstadoCatalogo tipo="seccionNoCargada" onReintentar={navegador.recargar} />}>
                <Suspense fallback={<EstadoCatalogo tipo="cargando" />}>
                  <ExplorarPage />
                </Suspense>
              </LimiteErrores>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
        <Route element={<DisenoAcceso />}>
          <Route path="/login" element={<LoginPage />} />
        </Route>
      </Routes>
    </SesionProvider>
  )
}
