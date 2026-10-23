import { Suspense, lazy, type ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from '../features/auth/LoginPage'
import { RegistroPage } from '../features/auth/registro/RegistroPage'
import { SesionProvider } from '../features/auth/SesionProvider'
import { EstadoCatalogo } from '../features/catalogo/componentes/EstadoCatalogo'
import { InicioPage } from '../features/catalogo/InicioPage'
import { LimiteErrores } from '../shared/components/LimiteErrores'
import { navegador } from '../shared/navegador'
import { DisenoAcceso } from './DisenoAcceso'
import { DisenoPrincipal } from './DisenoPrincipal'
import { RutaProtegida } from './RutaProtegida'

/**
 * Exploración (WF-05) se carga de forma diferida: Leaflet y su CSS solo se descargan al entrar a /explorar. Mientras
 * llega, se ve el estado de carga del catálogo; si la descarga falla, un aviso con «Reintentar» (recarga la página,
 * porque React.lazy guarda el fallo). El encabezado, el pie y la sesión quedan fuera del límite de errores.
 */
const ExplorarPage = lazy(() => import('../features/catalogo/ExplorarPage').then((m) => ({ default: m.ExplorarPage })))

/** Detalle (WF-06), reserva (WF-10) y «Mis reservas» (WF-11): también diferidas, cada una en su propio archivo. */
const DetalleServicioPage = lazy(() =>
  import('../features/catalogo/detalle/DetalleServicioPage').then((m) => ({ default: m.DetalleServicioPage })),
)
const ReservaPage = lazy(() => import('../features/reservas/ReservaPage').then((m) => ({ default: m.ReservaPage })))
const MisReservasPage = lazy(() =>
  import('../features/reservas/MisReservasPage').then((m) => ({ default: m.MisReservasPage })),
)
/** «Administrar cuentas» (WF-12): diferida y solo para el Administrador (DEC-51). */
const AdministrarCuentasPage = lazy(() =>
  import('../features/admin/AdministrarCuentasPage').then((m) => ({ default: m.AdministrarCuentasPage })),
)
/** «Contenido público» (DEC-70): preguntas frecuentes (WF-13) y términos (WF-15), públicas; su gestión, del Administrador. */
const PreguntasFrecuentesPage = lazy(() =>
  import('../features/contenido/PreguntasFrecuentesPage').then((m) => ({ default: m.PreguntasFrecuentesPage })),
)
const TerminosPage = lazy(() =>
  import('../features/contenido/terminos/TerminosPage').then((m) => ({ default: m.TerminosPage })),
)
const AdministrarPreguntasPage = lazy(() =>
  import('../features/admin/AdministrarPreguntasPage').then((m) => ({ default: m.AdministrarPreguntasPage })),
)
const AdministrarAvisosPage = lazy(() =>
  import('../features/admin/AdministrarAvisosPage').then((m) => ({ default: m.AdministrarAvisosPage })),
)

/** Una página diferida: estado de carga mientras llega y aviso con «Reintentar» si la descarga falla. */
function Diferida({ children }: { children: ReactNode }) {
  return (
    <LimiteErrores alFallar={<EstadoCatalogo tipo="seccionNoCargada" onReintentar={navegador.recargar} />}>
      <Suspense fallback={<EstadoCatalogo tipo="cargando" />}>{children}</Suspense>
    </LimiteErrores>
  )
}

/**
 * El router (BrowserRouter o MemoryRouter en pruebas) lo aporta quien monta la App.
 * Inicio (WF-04), exploración (WF-05), detalle (WF-06), reserva (WF-10), preguntas frecuentes (WF-13) y términos
 * (WF-15) son públicas, con el diseño de la plataforma (confirmar una reserva pide la sesión dentro de WF-10); «Mis
 * reservas» (WF-11) es privada y «Administrar cuentas» (WF-12), la gestión de preguntas frecuentes (WF-13) y la de
 * avisos (WF-14), privadas y solo para el Administrador. El inicio de sesión (WF-01) y el registro (WF-02, WF-03) usan
 * el diseño de acceso.
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
              <Diferida>
                <ExplorarPage />
              </Diferida>
            }
          />
          <Route
            path="/servicios/:id"
            element={
              <Diferida>
                <DetalleServicioPage />
              </Diferida>
            }
          />
          <Route
            path="/servicios/:id/reservar"
            element={
              <Diferida>
                <ReservaPage />
              </Diferida>
            }
          />
          <Route
            path="/preguntas-frecuentes"
            element={
              <Diferida>
                <PreguntasFrecuentesPage />
              </Diferida>
            }
          />
          <Route
            path="/terminos"
            element={
              <Diferida>
                <TerminosPage />
              </Diferida>
            }
          />
          <Route element={<RutaProtegida />}>
            <Route
              path="/mis-reservas"
              element={
                <Diferida>
                  <MisReservasPage />
                </Diferida>
              }
            />
          </Route>
          <Route element={<RutaProtegida rol="Administrador" />}>
            <Route
              path="/admin/cuentas"
              element={
                <Diferida>
                  <AdministrarCuentasPage />
                </Diferida>
              }
            />
            <Route
              path="/admin/preguntas-frecuentes"
              element={
                <Diferida>
                  <AdministrarPreguntasPage />
                </Diferida>
              }
            />
            <Route
              path="/admin/avisos"
              element={
                <Diferida>
                  <AdministrarAvisosPage />
                </Diferida>
              }
            />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
        <Route element={<DisenoAcceso />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/registro" element={<RegistroPage />} />
        </Route>
      </Routes>
    </SesionProvider>
  )
}
