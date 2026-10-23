import { useEffect, useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { formatearDia } from '../../shared/formato'
import { alCambiarAvisos, listarAvisosActivos, type Aviso } from './contenidoApi'
import './contenido.css'

/** Ids de los avisos que el visitante cerró en esta sesión del navegador (DEC-72). */
const CLAVE_CERRADOS = 'samaria360.avisosCerrados'

function leerCerrados(): Set<string> {
  try {
    const guardado = JSON.parse(sessionStorage.getItem(CLAVE_CERRADOS) ?? '[]') as unknown
    return new Set(Array.isArray(guardado) ? guardado.filter((id): id is string => typeof id === 'string') : [])
  } catch {
    return new Set()
  }
}

function guardarCerrados(cerrados: Set<string>): void {
  try {
    sessionStorage.setItem(CLAVE_CERRADOS, JSON.stringify([...cerrados]))
  } catch {
    // Sin almacenamiento: el cierre vale mientras la app siga montada.
  }
}

/**
 * Banner de avisos del municipio (RF40, WF-14; CU «Consultar avisos generales»), elemento común de las pantallas con el
 * encabezado de la plataforma (DEC-67). Muestra apilados todos los activos, del más reciente al más antiguo, con su
 * tipo y su fecha; sin rotación (WCAG 2.2.2). «Cerrar avisos» oculta los que se ven en la sesión del navegador: un
 * aviso nuevo (otro id) vuelve a mostrar el banner, solo con los no cerrados (DEC-72). Sin avisos, mientras carga o si
 * el catálogo no responde, no se muestra nada (FA1, FA2). Pide la lista sin token (DEC-47) y la vuelve a pedir cuando
 * la gestión de avisos avisa de un cambio.
 */
export function BannerAvisos() {
  const { t, i18n } = useTranslation()
  const id = useId()
  const [avisos, setAvisos] = useState<Aviso[]>([])
  const [cerrados, setCerrados] = useState<Set<string>>(leerCerrados)
  const [intento, setIntento] = useState(0)

  useEffect(() => alCambiarAvisos(() => setIntento((n) => n + 1)), [])

  useEffect(() => {
    let vigente = true
    listarAvisosActivos()
      .then((lista) => {
        if (vigente) setAvisos(lista)
      })
      .catch(() => {
        // El aviso no es contenido principal: si falla, la pantalla sigue igual y el banner no aparece.
        if (vigente) setAvisos([])
      })
    return () => {
      vigente = false
    }
  }, [intento])

  const visibles = avisos.filter((aviso) => !cerrados.has(aviso.id))
  if (visibles.length === 0) return null

  const cerrar = () => {
    const nuevos = new Set([...cerrados, ...visibles.map((aviso) => aviso.id)])
    guardarCerrados(nuevos)
    setCerrados(nuevos)
    // La región desaparece: el foco pasa al contenido principal en lugar de perderse.
    document.getElementById('contenido')?.focus()
  }

  return (
    <section className="banner-avisos" aria-labelledby={`${id}-titulo`}>
      <div className="banner-avisos__contenido">
        <h2 id={`${id}-titulo`} className="banner-avisos__titulo">
          {t('avisos.banner.titulo')}
        </h2>
        <ul className="banner-avisos__lista">
          {visibles.map((aviso) => (
            <li key={aviso.id} className="banner-avisos__aviso">
              <span className={`insignia-aviso insignia-aviso--${aviso.tipo}`}>{t(`avisos.tipos.${aviso.tipo}`)}</span>
              {/* El mensaje va tal como lo escribió el Administrador (DEC-69). */}
              <span className="banner-avisos__mensaje">{aviso.mensaje}</span>
              <span className="banner-avisos__fecha">
                <span aria-hidden="true">· </span>
                <time dateTime={aviso.fechaPublicacion}>{formatearDia(aviso.fechaPublicacion, i18n.language)}</time>
              </span>
            </li>
          ))}
        </ul>
        <button type="button" className="boton-secundario banner-avisos__cerrar" onClick={cerrar}>
          {t('avisos.banner.cerrar')}
        </button>
      </div>
    </section>
  )
}
