import { render, screen } from '@testing-library/react'
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

export function renderApp(ruta = '/') {
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
