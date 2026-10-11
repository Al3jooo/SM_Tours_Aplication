import { useTranslation } from 'react-i18next'
import type { ErrorBusqueda } from '../useBusquedaOferta'

/** `seccionNoCargada`: no se pudo descargar el código de la página (ruta diferida); se reintenta recargando. */
export type TipoEstado = 'cargando' | 'vacio' | 'fueraDeRango' | 'seccionNoCargada' | ErrorBusqueda

/**
 * Estados de la búsqueda. Un fallo del catálogo (p. ej. 503 del gateway) solo afecta a esta región: el encabezado,
 * la sesión y el resto de la página siguen funcionando.
 */
export function EstadoCatalogo({
  tipo,
  onReintentar,
  onQuitarFiltros,
  onPrimeraPagina,
  tarjetasEsqueleto = 3,
}: {
  tipo: TipoEstado
  onReintentar?: () => void
  onQuitarFiltros?: () => void
  onPrimeraPagina?: () => void
  tarjetasEsqueleto?: number
}) {
  const { t } = useTranslation()

  if (tipo === 'cargando') {
    return (
      <div className="estado-catalogo estado-catalogo--cargando" role="status" aria-busy="true">
        <p className="solo-lector">{t('catalogo.busqueda.cargando')}</p>
        <div className="estado-catalogo__esqueletos" aria-hidden="true">
          {Array.from({ length: tarjetasEsqueleto }, (_, i) => (
            <div key={i} className="tarjeta-esqueleto" />
          ))}
        </div>
      </div>
    )
  }

  const contenido = {
    vacio: { mensaje: 'vacio', ayuda: 'vacioAyuda', accion: onQuitarFiltros, boton: 'quitarFiltros' },
    fueraDeRango: { mensaje: 'fueraDeRango', ayuda: null, accion: onPrimeraPagina, boton: 'primeraPagina' },
    noDisponible: { mensaje: 'noDisponible', ayuda: 'noDisponibleAyuda', accion: onReintentar, boton: 'reintentar' },
    criteriosInvalidos: { mensaje: 'criteriosInvalidos', ayuda: null, accion: onQuitarFiltros, boton: 'quitarFiltros' },
    red: { mensaje: 'red', ayuda: null, accion: onReintentar, boton: 'reintentar' },
    otro: { mensaje: 'otro', ayuda: null, accion: onReintentar, boton: 'reintentar' },
    seccionNoCargada: { mensaje: 'seccionNoCargada', ayuda: null, accion: onReintentar, boton: 'reintentar' },
  }[tipo]

  const esError = tipo !== 'vacio' && tipo !== 'fueraDeRango'
  return (
    <div className={`estado-catalogo estado-catalogo--${esError ? 'error' : 'vacio'}`} role={esError ? 'alert' : 'status'}>
      <p className="estado-catalogo__mensaje">{t(`catalogo.busqueda.${contenido.mensaje}`)}</p>
      {contenido.ayuda && <p className="estado-catalogo__ayuda">{t(`catalogo.busqueda.${contenido.ayuda}`)}</p>}
      {contenido.accion && (
        <button type="button" className="boton-secundario" onClick={contenido.accion}>
          {t(`catalogo.busqueda.${contenido.boton}`)}
        </button>
      )}
    </div>
  )
}
