import { useCallback, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { longitudTexto } from '../admin/textoAcotado'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import type { EstadoPublicacion, ServicioGestion } from '../servicios/servicioGestion'
import { aNumero } from './borradorServicio'
import { LONGITUD_MAXIMA, erroresAnidados, idDeCampo, type ErroresServicio } from './rutasCampos'
import { consultarMiServicio } from './serviciosPrestadorApi'
import { useMiServicio } from './useMisServicios'

/**
 * Edición del Prestador sobre un servicio propio (iteración D, sesión B): RF32 «Actualizar tarifas» (DEC-198) y RF43,
 * datos comerciales del servicio (DEC-199). Solo `Pendiente` y `Publicado`, sin nueva aprobación (DEC-185, DEC-189);
 * la oferta no se edita (DEC-183). Cada PUT lleva la `version` leída (DEC-186). Las rutas de los errores son las del
 * contrato de edición (`precioBase`, `unidades[1].precioNoche`, `contacto.correo`), sin el prefijo del registro.
 */

export const ESTADOS_EDITABLES: readonly EstadoPublicacion[] = ['Pendiente', 'Publicado']

export function esEditable(estado: EstadoPublicacion): boolean {
  return ESTADOS_EDITABLES.includes(estado)
}

/** Misma forma que valida ms-catalog (DEC-138), como en el registro. */
const FORMA_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
/** Un precio con 2 decimales como mucho (DEC-138, `ms-catalog.md`): coma o punto decimal. */
const FORMA_PRECIO = /^\d+([.,]\d{1,2})?$/
/** Tope de `precioBase` y `precioNoche` (DEC-138): cabe en la reserva de ms-booking. */
export const PRECIO_MAXIMO = 99999999.99

const textoDe = (valor: unknown) => (valor === null || valor === undefined ? '' : String(valor))

// ------------------------------------------------------------------ RF32, precio

/** Una unidad de Alojamiento en el formulario de precio: nombre y tipo de solo lectura, el precio como se escribe. */
export interface UnidadPrecio {
  idUnidad: string
  nombre: string
  tipoHabitacion: string
  precioNoche: string
}

export interface BorradorPrecio {
  precioBase: string
  unidades: UnidadPrecio[]
}

export const rutaPrecioNoche = (indice: number) => `unidades[${indice}].precioNoche`

/** Las unidades del detalle de gestión, en su orden. */
export function unidadesDe(servicio: ServicioGestion): UnidadPrecio[] {
  const unidades = (servicio.detalleCategoria as { unidades?: unknown } | null)?.unidades
  if (!Array.isArray(unidades)) return []
  return unidades.map((unidad: Record<string, unknown>) => ({
    idUnidad: textoDe(unidad.idUnidad),
    nombre: textoDe(unidad.nombre),
    tipoHabitacion: textoDe(unidad.tipoHabitacion),
    precioNoche: textoDe(unidad.precioNoche),
  }))
}

export function borradorPrecioDe(servicio: ServicioGestion): BorradorPrecio {
  return { precioBase: textoDe(servicio.precioBase), unidades: unidadesDe(servicio) }
}

/**
 * Después de releer (409 SERVICIO_MODIFICADO, DEC-200): las unidades en el orden vigente, cada una con lo que el
 * Prestador escribió. Una unidad nueva llega con su precio vigente.
 */
export function fusionarPrecio(borrador: BorradorPrecio, vigente: ServicioGestion): BorradorPrecio {
  const escritos = new Map(borrador.unidades.map((unidad) => [unidad.idUnidad, unidad.precioNoche]))
  return {
    precioBase: borrador.precioBase,
    unidades: unidadesDe(vigente).map((unidad) => ({ ...unidad, precioNoche: escritos.get(unidad.idUnidad) ?? unidad.precioNoche })),
  }
}

/** El «desde» de Alojamiento (DEC-127, DEC-163): el menor `precioNoche` válido; null si no hay ninguno. */
export function precioDesdeUnidades(unidades: UnidadPrecio[]): number | null {
  const precios = unidades.map((unidad) => aNumero(unidad.precioNoche)).filter((precio): precio is number => precio !== null && precio > 0)
  return precios.length === 0 ? null : Math.min(...precios)
}

/**
 * Validación del precio en el cliente (DEC-198): obligatorio, mayor que 0, hasta 99 999 999,99 y 2 decimales como
 * mucho (más decimales → FORMATO). Son las reglas del contrato; el servidor sigue siendo la fuente.
 */
function validarPrecioTexto(errores: ErroresServicio, ruta: string, valor: string) {
  const limpio = valor.trim()
  if (limpio === '') errores[ruta] = 'REQUERIDO'
  else if (!FORMA_PRECIO.test(limpio)) errores[ruta] = 'FORMATO'
  else if ((aNumero(limpio) ?? 0) <= 0) errores[ruta] = 'VALOR_MINIMO'
  else if ((aNumero(limpio) ?? 0) > PRECIO_MAXIMO) errores[ruta] = 'VALOR_MAXIMO'
}

export function validarPrecio(servicio: ServicioGestion, borrador: BorradorPrecio): ErroresServicio {
  const errores: ErroresServicio = {}
  if (servicio.categoria === 'Alojamiento') {
    borrador.unidades.forEach((unidad, indice) => validarPrecioTexto(errores, rutaPrecioNoche(indice), unidad.precioNoche))
  } else {
    validarPrecioTexto(errores, 'precioBase', borrador.precioBase)
  }
  return errores
}

/** Rutas con control, en el orden del formulario. */
export function rutasPrecio(servicio: ServicioGestion, borrador: BorradorPrecio): string[] {
  if (servicio.categoria !== 'Alojamiento') return ['precioBase']
  return ['unidades', ...borrador.unidades.map((_unidad, indice) => rutaPrecioNoche(indice))]
}

/** Cuerpo de RF32 sin la `version`: solo el campo de su categoría; en Alojamiento, todas las unidades una vez. */
export function cuerpoPrecio(servicio: ServicioGestion, borrador: BorradorPrecio): Record<string, unknown> {
  if (servicio.categoria === 'Alojamiento') {
    return { unidades: borrador.unidades.map((unidad) => ({ idUnidad: unidad.idUnidad, precioNoche: aNumero(unidad.precioNoche) })) }
  }
  return { precioBase: aNumero(borrador.precioBase) }
}

/** Valores comparables (DEC-200): el precio base o el de cada unidad, por su id. */
export function valoresPrecio(servicio: ServicioGestion): Record<string, number | null> {
  if (servicio.categoria !== 'Alojamiento') return { precioBase: aNumero(textoDe(servicio.precioBase)) }
  return Object.fromEntries(unidadesDe(servicio).map((unidad) => [`unidad:${unidad.idUnidad}`, aNumero(unidad.precioNoche)]))
}

// ------------------------------------------------------------------ RF43, datos comerciales del servicio

export interface BorradorDatosComerciales {
  nombreComercial: string
  telefono: string
  correo: string
}

export const RUTAS_DATOS_COMERCIALES = ['nombreComercial', 'contacto.telefono', 'contacto.correo'] as const

export function borradorDatosDe(servicio: ServicioGestion): BorradorDatosComerciales {
  return {
    nombreComercial: servicio.nombreComercial,
    telefono: textoDe(servicio.contacto?.telefono),
    correo: textoDe(servicio.contacto?.correo),
  }
}

/** Las reglas del registro (RF09, DEC-138): obligatorios, longitudes y la forma del correo. */
export function validarDatosComerciales(borrador: BorradorDatosComerciales): ErroresServicio {
  const errores: ErroresServicio = {}
  const obligatorio = (ruta: string, valor: string) => {
    const longitud = longitudTexto(valor)
    if (longitud === 0) errores[ruta] = 'REQUERIDO'
    else if (longitud > LONGITUD_MAXIMA[ruta]) errores[ruta] = 'LONGITUD_MAXIMA'
  }
  obligatorio('nombreComercial', borrador.nombreComercial)
  obligatorio('contacto.telefono', borrador.telefono)
  obligatorio('contacto.correo', borrador.correo)
  if (!errores['contacto.correo'] && !FORMA_CORREO.test(borrador.correo.trim())) errores['contacto.correo'] = 'FORMATO'
  return errores
}

/** Cuerpo de RF43 sin la `version`: solo el nombre y el contacto. */
export function cuerpoDatosComerciales(borrador: BorradorDatosComerciales): Record<string, unknown> {
  return {
    nombreComercial: borrador.nombreComercial.trim(),
    contacto: { telefono: borrador.telefono.trim(), correo: borrador.correo.trim() },
  }
}

export function valoresDatosComerciales(servicio: ServicioGestion): Record<string, string> {
  return {
    nombreComercial: servicio.nombreComercial,
    'contacto.telefono': textoDe(servicio.contacto?.telefono),
    'contacto.correo': textoDe(servicio.contacto?.correo),
  }
}

// ------------------------------------------------------------------ flujo común

/** Estado de la navegación al detalle propio: el anuncio que muestra una vez (éxito o ESTADO_CAMBIADO, DEC-201). */
export interface EstadoAnuncio {
  anuncio: string
}

/** Cómo se atiende el rechazo de un PUT de edición (DEC-200, DEC-201). */
export type RechazoEdicion =
  | { tipo: 'campos'; campos: ErroresServicio; general: string | null }
  | { tipo: 'modificado' }
  | { tipo: 'estadoCambiado'; estadoActual: string | null }
  | { tipo: 'noEncontrado' }
  | { tipo: 'sesion' }
  | { tipo: 'general'; clave: string }

export function clasificarRechazoEdicion(error: unknown, conControl: readonly string[]): RechazoEdicion {
  if (!(error instanceof ApiError)) return { tipo: 'general', clave: 'GENERICO' }
  // Un 401 ya cerró la sesión (httpClient): RutaProtegida lleva al login.
  if (error.status === 401) return { tipo: 'sesion' }
  if (error.codigo === 'SERVICIO_MODIFICADO') return { tipo: 'modificado' }
  if (error.codigo === 'ESTADO_CAMBIADO') {
    const estado = error.problema?.estadoActual
    return { tipo: 'estadoCambiado', estadoActual: typeof estado === 'string' ? estado : null }
  }
  if (error.status === 404) return { tipo: 'noEncontrado' }
  if (error.codigo === 'DATOS_INVALIDOS') {
    const todos = erroresAnidados(error.problema?.campos)
    const propios = Object.fromEntries(Object.entries(todos).filter(([ruta]) => conControl.includes(ruta)))
    const sinControl = Object.keys(todos).length - Object.keys(propios).length
    // Un error sin control (una unidad que ya no es del servicio, `version`…) va al mensaje general, con «Recargar».
    return { tipo: 'campos', campos: propios, general: sinControl > 0 || Object.keys(todos).length === 0 ? 'DATOS_INVALIDOS' : null }
  }
  if (error.status === 403 && error.codigo === 'ACCESO_DENEGADO') return { tipo: 'general', clave: 'ACCESO_DENEGADO' }
  if (servicioNoDisponible(error)) return { tipo: 'general', clave: 'SERVICIO_NO_DISPONIBLE' }
  if (error.codigo === ERROR_RED) return { tipo: 'general', clave: 'ERROR_RED' }
  return { tipo: 'general', clave: 'GENERICO' }
}

/** Claves con las que se cambia el mensaje y la acción de «Recargar» (descarta lo escrito y vuelve a leer). */
const CON_RECARGA = new Set(['DATOS_INVALIDOS', 'RELECTURA'])

interface OpcionesEdicion<B, V> {
  borradorDe: (servicio: ServicioGestion) => B
  /** Tras releer (DEC-200): lo escrito sobre el servicio vigente. Por omisión, lo escrito tal cual. */
  fusionar?: (borrador: B, vigente: ServicioGestion) => B
  validar: (servicio: ServicioGestion, borrador: B) => ErroresServicio
  rutas: (servicio: ServicioGestion, borrador: B) => string[]
  cuerpo: (servicio: ServicioGestion, borrador: B) => Record<string, unknown>
  enviar: (id: string, cuerpo: Record<string, unknown>) => Promise<ServicioGestion>
  /** Valores comparables del servicio: los que cambiaron entre la lectura y la relectura se muestran como «Valor actual». */
  valores: (servicio: ServicioGestion) => Record<string, V>
  /** Clave i18n del anuncio después del 200. */
  anuncioExito: string
}

/**
 * Carga, envío y conflictos de una edición (DEC-198 a DEC-201):
 * - el servicio se lee con `GET /prestador/servicios/{id}`; un `Rechazado` no muestra el formulario;
 * - un solo PUT en curso (doble clic) y la `version` leída en cada envío;
 * - 200: vuelve al detalle propio con el anuncio;
 * - 409 SERVICIO_MODIFICADO: relee el servicio sin tocar lo escrito y marca el «Valor actual» de lo que cambió; el
 *   reenvío es otro clic del Prestador, ya con la versión nueva (DEC-200);
 * - 409 ESTADO_CAMBIADO (o una relectura que ya no es editable): vuelve al detalle con el aviso (DEC-201);
 * - 404: «no encontrado», sin formulario.
 */
export function useEdicionServicio<B, V>(id: string, opciones: OpcionesEdicion<B, V>) {
  const { t } = useTranslation()
  const navegar = useNavigate()
  const carga = useMiServicio(id)
  const leido = carga.estado === 'ok' ? carga.datos : null
  const [releido, setReleido] = useState<ServicioGestion | null>(null)
  /** Lo escrito; null mientras no se cambió nada (entonces se muestran los valores del servicio). */
  const [escrito, setEscrito] = useState<B | null>(null)
  const [errores, setErrores] = useState<ErroresServicio>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [actuales, setActuales] = useState<Record<string, V>>({})
  const [perdido, setPerdido] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)
  const mensajeGeneral = useRef<HTMLDivElement>(null)
  const op = useRef(opciones)
  op.current = opciones

  const servicio = releido ?? leido
  // Se deriva en el mismo render que el servicio: el formulario aparece con el título, sin un render intermedio.
  const borrador = escrito ?? (servicio ? op.current.borradorDe(servicio) : null)

  const volverAlDetalle = (anuncio: string) => {
    const estado: EstadoAnuncio = { anuncio }
    navegar(`/prestador/servicios/${encodeURIComponent(id)}`, { state: estado })
  }
  const anuncioEstado = (estado: string | null) =>
    estado
      ? t('prestador.edicion.errores.ESTADO_CAMBIADO', { estado: t(`prestador.estados.${estado}`, { defaultValue: estado }) })
      : t('prestador.edicion.errores.ESTADO_CAMBIADO_SIN_ESTADO')

  const mostrarGeneral = (clave: string) => {
    setGeneral(clave)
    setTimeout(() => mensajeGeneral.current?.focus(), 0)
  }
  const enfocarPrimero = (encontrados: ErroresServicio, rutas: string[]) => {
    const primera = rutas.find((ruta) => encontrados[ruta])
    if (primera) setTimeout(() => document.getElementById(idDeCampo(primera))?.focus(), 0)
  }

  /** 409 SERVICIO_MODIFICADO (DEC-200): relee y conserva lo escrito. */
  const releer = async (anterior: ServicioGestion) => {
    try {
      const vigente = await consultarMiServicio(id)
      if (!esEditable(vigente.estadoPublicacion)) {
        volverAlDetalle(anuncioEstado(vigente.estadoPublicacion))
        return
      }
      const antes = op.current.valores(anterior)
      const ahora = op.current.valores(vigente)
      const cambios = Object.fromEntries(Object.entries(ahora).filter(([clave, valor]) => antes[clave] !== valor))
      const fusionar = op.current.fusionar
      const conservado = escrito ?? op.current.borradorDe(anterior)
      setReleido(vigente)
      setEscrito(fusionar ? fusionar(conservado, vigente) : conservado)
      setActuales(cambios)
      mostrarGeneral(Object.keys(cambios).length > 0 ? 'SERVICIO_MODIFICADO' : 'SERVICIO_MODIFICADO_SIN_CAMBIOS')
    } catch (fallo) {
      if (fallo instanceof ApiError && fallo.status === 401) return
      if (fallo instanceof ApiError && fallo.status === 404) setPerdido(true)
      else mostrarGeneral('RELECTURA')
    }
  }

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault()
    if (enCurso.current || !servicio || !borrador) return
    const rutas = op.current.rutas(servicio, borrador)
    setGeneral(null)
    const locales = op.current.validar(servicio, borrador)
    setErrores(locales)
    if (Object.keys(locales).length > 0) {
      enfocarPrimero(locales, rutas)
      return
    }
    enCurso.current = true
    setEnviando(true)
    try {
      await op.current.enviar(id, { ...op.current.cuerpo(servicio, borrador), version: servicio.version })
      volverAlDetalle(t(op.current.anuncioExito))
    } catch (fallo) {
      const rechazo = clasificarRechazoEdicion(fallo, rutas)
      switch (rechazo.tipo) {
        case 'sesion':
          break
        case 'modificado':
          await releer(servicio)
          break
        case 'estadoCambiado':
          volverAlDetalle(anuncioEstado(rechazo.estadoActual))
          break
        case 'noEncontrado':
          setPerdido(true)
          break
        case 'campos':
          setErrores(rechazo.campos)
          if (rechazo.general) mostrarGeneral(rechazo.general)
          else enfocarPrimero(rechazo.campos, rutas)
          break
        case 'general':
          mostrarGeneral(rechazo.clave)
          break
      }
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  /** Cambia el borrador y limpia los errores de esas rutas. */
  const cambiar = (cambio: (actual: B) => B, rutas: string[]) => {
    if (!servicio) return
    const base = servicio
    setEscrito((actual) => cambio(actual ?? op.current.borradorDe(base)))
    setErrores((actuales) => Object.fromEntries(Object.entries(actuales).filter(([ruta]) => !rutas.includes(ruta))))
  }

  /** «Descartar cambios»: lo escrito vuelve a los valores del servicio vigente. */
  const descartar = () => {
    setEscrito(null)
    setErrores({})
    setGeneral(null)
    setActuales({})
  }

  /** «Recargar»: descarta lo escrito y vuelve a leer el servicio. */
  const recargarCarga = carga.recargar
  const recargar = useCallback(() => {
    setReleido(null)
    setEscrito(null)
    setErrores({})
    setGeneral(null)
    setActuales({})
    recargarCarga()
  }, [recargarCarga])

  return {
    carga,
    servicio,
    borrador,
    errores,
    general,
    conRecarga: general !== null && CON_RECARGA.has(general),
    actuales,
    perdido,
    enviando,
    mensajeGeneral,
    guardar,
    cambiar,
    descartar,
    recargar,
  }
}
