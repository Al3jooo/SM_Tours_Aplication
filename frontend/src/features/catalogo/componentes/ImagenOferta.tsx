import { useState } from 'react'
import type { ResultadoOferta } from '../catalogoApi'
import { imagenDeCategoria } from './etiquetas'

/**
 * Imagen de la tarjeta. Las rutas de la semilla (/imagenes/catalogo/<slug>.svg) todavía no existen: si la imagen
 * del elemento no carga, se usa la ilustración propia de su categoría. Es decorativa (alt vacío): el nombre ya está
 * en el texto de la tarjeta.
 */
export function ImagenOferta({ resultado }: { resultado: ResultadoOferta }) {
  const respaldo = imagenDeCategoria(resultado.categoria, resultado.tipo)
  const [origen, setOrigen] = useState(resultado.imagen ?? respaldo)

  return (
    <img
      className="imagen-oferta"
      src={origen}
      alt=""
      loading="lazy"
      width={320}
      height={200}
      onError={() => {
        if (origen !== respaldo) setOrigen(respaldo)
      }}
    />
  )
}
