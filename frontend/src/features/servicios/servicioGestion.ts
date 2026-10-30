import type { Categoria, CondicionAccesibilidad } from '../catalogo/catalogoApi'

/**
 * Forma de gestión de un servicio comercial (ms-catalog, iteración C; docs/sad/api/ms-catalog.md): la del detalle
 * propio del Prestador (WF-18) y la de la revisión del Administrador (WF-19, que suma `revisadoPor` y
 * `estadoCuentaPrestador`). Sin la regla de visibilidad pública.
 */
export const ESTADOS_PUBLICACION = ['Pendiente', 'Publicado', 'Rechazado'] as const
export type EstadoPublicacion = (typeof ESTADOS_PUBLICACION)[number]

/** Forma de gestión: la respuesta del 201 y de los detalles de gestión. */
export interface ServicioGestion {
  id: string
  idPrestador: string
  nombreComercial: string
  categoria: Categoria
  descripcion: string | null
  direccion: string | null
  coordenadas: { longitud: number; latitud: number } | null
  contacto: { telefono: string | null; correo: string | null } | null
  /** ms-catalog siempre envía una lista; un null (fuera de contrato) se lee como lista vacía. */
  condicionesAccesibilidad: CondicionAccesibilidad[] | null
  horarioAtencion: string | null
  /** En Alojamiento, el menor `precioNoche` (DEC-127). Número o texto decimal (BigDecimal). */
  precioBase: number | string | null
  moneda: string
  detalleCategoria: Record<string, unknown> | null
  /** Solo Tour (`diasOperacion` y `salidas`) y Alojamiento (`diasOperacion`); null en las demás (DEC-136). */
  oferta: { diasOperacion: string[]; salidas?: string[] | null } | null
  imagenes: string[]
  estadoPublicacion: EstadoPublicacion
  prestadorActivo: boolean
  /** Copia del RNT del prestador (DEC-126). */
  rnt: string | null
  fechaRegistro: string
  motivoRechazo: string | null
  fechaRevision: string | null
}
