import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import '../../../i18n'
import { CasillaPoliticas } from '../registro/FormularioTurista'

/** Cuando exista la página de la política (RUTA_POLITICA_PRIVACIDAD), la casilla enlaza a ella. */
describe('Casilla de la Política de Privacidad', () => {
  it('con una ruta definida, «Política de Privacidad» es un enlace a esa ruta y la casilla sigue funcionando', () => {
    render(
      <MemoryRouter>
        <CasillaPoliticas id="politicas" marcada={false} onCambiar={() => {}} rutaPolitica="/politica-de-privacidad" />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: 'Política de Privacidad' })).toHaveAttribute(
      'href',
      '/politica-de-privacidad',
    )
    expect(screen.getByRole('checkbox', { name: /Acepto la Política de Privacidad/ })).toBeInTheDocument()
    expect(screen.queryByText(/Próximamente/)).not.toBeInTheDocument()
  })

  it('sin ruta, el texto queda como pendiente, igual que en el pie', () => {
    render(
      <MemoryRouter>
        <CasillaPoliticas id="politicas" marcada={false} onCambiar={() => {}} rutaPolitica={null} />
      </MemoryRouter>,
    )

    expect(screen.queryByRole('link')).not.toBeInTheDocument()
    expect(screen.getByText('Política de Privacidad')).toHaveClass('enlace-deshabilitado')
  })
})
