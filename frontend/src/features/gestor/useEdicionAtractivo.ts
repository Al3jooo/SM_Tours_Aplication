import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, ERROR_RED, servicioNoDisponible } from '../../shared/api/httpClient'
import { clasificarError, type EstadoCarga } from '../prestador/useMisServicios'
import { erroresAnidados, idDeCampo, type ErroresServicio } from '../prestador/rutasCampos'
import { actualizarAtractivo, consultarAtractivoGestion, type AtractivoGestion } from './atractivosGestorApi'
import { borradorDe, construirDatos, rutasConControl, validarBorrador, type BorradorAtractivo } from './borradorAtractivo'
import type { EstadoAnuncioGestor } from './criteriosAtractivosGestor'

/** Claves con las que el mensaje general ofrece «Recargar» (descarta lo escrito y vuelve a leer). */
const CON_RECARGA = new Set(['DATOS_INVALIDOS', 'RELECTURA'])

/** Lo que se compara entre la lectura y la relectura para marcar el «Valor actual» (DEC-231, como DEC-200). */
function valores(atractivo: AtractivoGestion): Record<string, string> {
  const b = borradorDe(atractivo)
  return {
    nombre: b.nombre,
    categoria: b.categoria,
    descripcion: b.descripcion,
    'coordenadas.latitud': b.latitud,
    'coordenadas.longitud': b.longitud,
    limiteCapacidadCarga: b.limiteCapacidadCarga,
    normasAmbientales: b.normasAmbientales,
    horarioAtencion: b.horarioAtencion,
    condicionesAccesibilidad: [...b.condicionesAccesibilidad].sort().join(', '),
  }
}

/** Un atractivo para el Gestor (`GET /gestor/atractivos/{id}`); `recargar` es «Reintentar». */
export function useAtractivoGestion(id: string) {
  const [estado, setEstado] = useState<EstadoCarga<AtractivoGestion>>({ estado: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    setEstado({ estado: 'cargando' })
    consultarAtractivoGestion(id)
      .then((datos) => {
        if (vigente) setEstado({ estado: 'ok', datos })
      })
      .catch((error: unknown) => {
        if (vigente) setEstado({ estado: 'error', error: clasificarError(error) })
      })
    return () => {
      vigente = false
    }
  }, [id, intento])

  const recargar = useCallback(() => setIntento((n) => n + 1), [])
  return { ...estado, recargar } as EstadoCarga<AtractivoGestion> & { recargar: () => void }
}

/**
 * Edición de WF-09 (CU «Actualizar atractivo»; DEC-231, con la forma de `useEdicionServicio`):
 * - un solo PUT en curso y la `version` leída en cada envío (DEC-207);
 * - 200: vuelve al panel con «Cambios guardados»;
 * - 409 ATRACTIVO_MODIFICADO: relee sin tocar lo escrito y marca el «Valor actual» de lo que cambió; el reenvío es otro
 *   clic, ya con la versión nueva;
 * - 404: «no encontrado», sin formulario. Un 401 ya cerró la sesión (httpClient).
 * El estado se cambia con su diálogo (DEC-210): `alCambiarEstado` actualiza la versión sin tocar lo escrito.
 */
export function useEdicionAtractivo(id: string) {
  const navegar = useNavigate()
  const carga = useAtractivoGestion(id)
  const leido = carga.estado === 'ok' ? carga.datos : null
  const [releido, setReleido] = useState<AtractivoGestion | null>(null)
  const [escrito, setEscrito] = useState<BorradorAtractivo | null>(null)
  const [errores, setErrores] = useState<ErroresServicio>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [actuales, setActuales] = useState<Record<string, string>>({})
  const [perdido, setPerdido] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const enCurso = useRef(false)
  const mensajeGeneral = useRef<HTMLDivElement>(null)

  const atractivo = releido ?? leido
  const borrador = escrito ?? (atractivo ? borradorDe(atractivo) : null)
  const rutas = rutasConControl(false)

  const mostrarGeneral = (clave: string) => {
    setGeneral(clave)
    setTimeout(() => mensajeGeneral.current?.focus(), 0)
  }
  const enfocarPrimero = (encontrados: ErroresServicio) => {
    const primera = rutas.find((ruta) => encontrados[ruta])
    if (primera) setTimeout(() => document.getElementById(idDeCampo(primera))?.focus(), 0)
  }

  const releer = async (anterior: AtractivoGestion, conservado: BorradorAtractivo) => {
    try {
      const vigente = await consultarAtractivoGestion(id)
      const antes = valores(anterior)
      const ahora = valores(vigente)
      const cambios = Object.fromEntries(Object.entries(ahora).filter(([clave, valor]) => antes[clave] !== valor))
      setReleido(vigente)
      setEscrito(conservado)
      setActuales(cambios)
      mostrarGeneral(Object.keys(cambios).length > 0 ? 'ATRACTIVO_MODIFICADO' : 'ATRACTIVO_MODIFICADO_SIN_CAMBIOS')
    } catch (fallo) {
      if (fallo instanceof ApiError && fallo.status === 401) return
      if (fallo instanceof ApiError && fallo.status === 404) setPerdido(true)
      else mostrarGeneral('RELECTURA')
    }
  }

  const guardar = async (evento: FormEvent) => {
    evento.preventDefault()
    if (enCurso.current || !atractivo || !borrador) return
    setGeneral(null)
    const locales = validarBorrador(borrador, false)
    setErrores(locales)
    if (Object.keys(locales).length > 0) {
      enfocarPrimero(locales)
      return
    }
    enCurso.current = true
    setEnviando(true)
    try {
      await actualizarAtractivo(id, construirDatos(borrador, false), atractivo.version)
      const estado: EstadoAnuncioGestor = { anuncio: 'guardado' }
      navegar('/gestor/atractivos', { state: estado })
    } catch (fallo) {
      if (!(fallo instanceof ApiError)) mostrarGeneral('GENERICO')
      else if (fallo.status === 401) return
      else if (fallo.codigo === 'ATRACTIVO_MODIFICADO') await releer(atractivo, borrador)
      else if (fallo.status === 404) setPerdido(true)
      else if (fallo.codigo === 'DATOS_INVALIDOS') {
        const todos = erroresAnidados(fallo.problema?.campos)
        const propios = Object.fromEntries(Object.entries(todos).filter(([ruta]) => rutas.includes(ruta)))
        setErrores(propios)
        const sinControl = Object.keys(todos).length - Object.keys(propios).length
        if (sinControl > 0 || Object.keys(todos).length === 0) mostrarGeneral('DATOS_INVALIDOS')
        else enfocarPrimero(propios)
      } else if (fallo.status === 403 && fallo.codigo === 'ACCESO_DENEGADO') mostrarGeneral('ACCESO_DENEGADO')
      else if (servicioNoDisponible(fallo)) mostrarGeneral('SERVICIO_NO_DISPONIBLE')
      else if (fallo.codigo === ERROR_RED) mostrarGeneral('ERROR_RED')
      else mostrarGeneral('GENERICO')
    } finally {
      enCurso.current = false
      setEnviando(false)
    }
  }

  /** Cambia el borrador y limpia los errores de esas rutas. */
  const cambiar = (cambios: Partial<BorradorAtractivo>, rutasCambiadas: string[]) => {
    if (!atractivo) return
    const base = atractivo
    setEscrito((actual) => ({ ...(actual ?? borradorDe(base)), ...cambios }))
    setErrores((actuales) => Object.fromEntries(Object.entries(actuales).filter(([ruta]) => !rutasCambiadas.includes(ruta))))
  }

  /** El diálogo de estado guardó: la versión y el estado nuevos, sin tocar lo escrito. */
  const alCambiarEstado = (actualizado: AtractivoGestion) => setReleido(actualizado)

  const descartar = () => {
    setEscrito(null)
    setErrores({})
    setGeneral(null)
    setActuales({})
  }

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
    atractivo,
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
    alCambiarEstado,
    descartar,
    recargar,
  }
}
