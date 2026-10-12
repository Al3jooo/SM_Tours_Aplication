import { fireEvent, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { CASCADA_CERRADA, HOTEL, TOUR_CAFE, paginaOferta, respuestaJson, simularFetch } from '../../../test/fetchSimulado'
import { renderApp } from '../../../test/renderApp'

const OFERTA = 'GET /api/v1/catalogo/oferta'

async function tarjeta(nombre: string) {
  return screen.findByRole('article', { name: nombre })
}

/** La imagen es decorativa (alt=""), así que no tiene rol accesible: se busca en el DOM de la tarjeta. */
function imagenDe(articulo: HTMLElement): HTMLImageElement {
  const imagen = articulo.querySelector('img')
  if (!imagen) throw new Error('La tarjeta no tiene imagen')
  return imagen
}

describe('Imagen de la tarjeta de oferta', () => {
  it('declara 600×400 (3:2), carga diferida y alt vacío, también al pasar al respaldo de la categoría', async () => {
    simularFetch({ [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL])) })
    renderApp('/explorar')

    const imagen = imagenDe(await tarjeta('Hotel Casa Tayrona'))
    expect(imagen).toHaveAttribute('src', '/imagenes/catalogo/hotel-casa-tayrona.webp')
    const atributos = () => ({
      width: imagen.getAttribute('width'),
      height: imagen.getAttribute('height'),
      loading: imagen.getAttribute('loading'),
      decoding: imagen.getAttribute('decoding'),
      alt: imagen.getAttribute('alt'),
    })
    const esperados = { width: '600', height: '400', loading: 'lazy', decoding: 'async', alt: '' }
    expect(atributos()).toEqual(esperados)

    // Si la foto no carga, se usa la ilustración de la categoría con las mismas dimensiones.
    fireEvent.error(imagen)
    expect(imagen).toHaveAttribute('src', '/imagenes/catalogo/categorias/alojamiento.svg')
    expect(atributos()).toEqual(esperados)
  })

  it('cada tarjeta nombra el lugar en su h3 y la imagen no es un enlace', async () => {
    simularFetch({ [OFERTA]: () => respuestaJson(200, paginaOferta([HOTEL, TOUR_CAFE, CASCADA_CERRADA])) })
    renderApp('/explorar')
    await tarjeta('Hotel Casa Tayrona')

    for (const { nombre } of [HOTEL, TOUR_CAFE, CASCADA_CERRADA]) {
      const articulo = screen.getByRole('article', { name: nombre })
      expect(within(articulo).getByRole('heading', { level: 3 })).toHaveTextContent(nombre)
      const imagen = imagenDe(articulo)
      expect(imagen.closest('a, button, [role="link"], [role="button"], [onclick]')).toBeNull()
      // Ningún enlace de la tarjeta queda sin nombre accesible.
      for (const enlace of within(articulo).queryAllByRole('link')) {
        expect(enlace).toHaveAccessibleName()
      }
    }
  })
})
