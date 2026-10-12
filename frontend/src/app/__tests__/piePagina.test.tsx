import { screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { paginaOferta, respuestaJson, simularFetch } from '../../test/fetchSimulado'
import { renderApp } from '../../test/renderApp'

describe('Pie de página', () => {
  it('muestra el aviso académico sobre las imágenes en español y en inglés', async () => {
    simularFetch({ 'GET /api/v1/catalogo/oferta': () => respuestaJson(200, paginaOferta([])) })
    const { usuario } = renderApp('/')

    const pie = screen.getByRole('contentinfo')
    expect(
      within(pie).getByText(
        'Prototipo académico — Universidad del Magdalena. Las imágenes son de referencia y pueden pertenecer a terceros; no se usan con fines comerciales.',
      ),
    ).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Inglés' }))

    expect(
      within(pie).getByText(
        'Academic prototype — Universidad del Magdalena. Images are for reference only and may belong to third parties; they are not used for commercial purposes.',
      ),
    ).toBeInTheDocument()
  })
})
