import { render, screen } from '@testing-library/react'
import type { ComponentProps } from 'react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { App } from '../app/App'

/** Expone la ruta y la consulta actuales para comprobar la navegación (oculto; solo para pruebas). */
function UbicacionActual() {
  const { pathname, search } = useLocation()
  return (
    <output data-testid="ubicacion" hidden>
      {pathname + search}
    </output>
  )
}

/** Ruta inicial: texto ('/explorar?texto=x') o una entrada con estado ({ pathname, state }), como la de RutaProtegida. */
type EntradaInicial = NonNullable<ComponentProps<typeof MemoryRouter>['initialEntries']>[number]

export function renderApp(ruta: EntradaInicial = '/') {
  const usuario = userEvent.setup()
  const resultado = render(
    <MemoryRouter initialEntries={[ruta]}>
      <App />
      <UbicacionActual />
    </MemoryRouter>,
  )
  return { usuario, ...resultado }
}

/** Ruta y consulta actuales de la app montada con renderApp. */
export function ubicacionActual(): string {
  return screen.getByTestId('ubicacion').textContent ?? ''
}
