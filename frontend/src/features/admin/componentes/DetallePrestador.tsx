import { useEffect, useId, useRef, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { PrestadorDetalle } from '../adminApi'
import { formatearFecha } from '../formato'
import { InsigniaCuenta } from './InsigniaCuenta'

export type EstadoDetalle =
  | { estado: 'cargando'; idUsuario: string }
  | { estado: 'ok'; idUsuario: string; prestador: PrestadorDetalle }
  | { estado: 'error'; idUsuario: string; codigo: string }

/** «—» a la vista y «sin datos» para lectores de pantalla (razón social opcional, D2 de «Registrarse»). */
function SinDato() {
  const { t } = useTranslation()
  return (
    <>
      <span aria-hidden="true">—</span>
      <span className="solo-lector">{t('comun.sinDatos')}</span>
    </>
  )
}

/**
 * «Detalle del prestador» de WF-12 (RF28): datos organizacionales y RNT, «Políticas aceptadas: Sí · fecha» (sin hora) y
 * sin «País» (DEC-45). Con estado Pendiente, «Aprobar → Activo» y «Rechazar». Al abrirse, el foco va al título.
 */
export function DetallePrestador({
  detalle,
  enviando,
  error,
  onAprobar,
  onRechazar,
  onReintentar,
  onCerrar,
}: {
  detalle: EstadoDetalle
  enviando: boolean
  /** Código del último intento fallido de «Aprobar». */
  error: string | null
  onAprobar: (prestador: PrestadorDetalle) => void
  onRechazar: (prestador: PrestadorDetalle) => void
  onReintentar: () => void
  onCerrar: () => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const titulo = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    titulo.current?.focus()
  }, [detalle.idUsuario])

  const fila = (etiqueta: string, valor: ReactNode) => (
    <div key={etiqueta}>
      <dt>{etiqueta}</dt>
      <dd>{valor}</dd>
    </div>
  )

  return (
    <section className="detalle-prestador" aria-labelledby={`${id}-titulo`}>
      <div className="detalle-prestador__encabezado">
        <h2 id={`${id}-titulo`} ref={titulo} tabIndex={-1}>
          {t('admin.detalle.titulo')}
        </h2>
        <button type="button" className="boton-secundario" onClick={onCerrar}>
          {t('admin.detalle.cerrar')}
        </button>
      </div>

      {detalle.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('admin.detalle.cargando')}
        </p>
      )}
      {detalle.estado === 'error' && (
        <div className="estado-catalogo estado-catalogo--error" role="alert">
          <p className="estado-catalogo__mensaje">
            {t(`admin.errores.${detalle.codigo}`, { defaultValue: t('admin.errores.GENERICO') })}
          </p>
          <button type="button" className="boton-secundario" onClick={onReintentar}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}
      {detalle.estado === 'ok' && (
        <>
          <dl className="detalle-prestador__datos">
            {fila(t('admin.detalle.representante'), detalle.prestador.representanteLegal)}
            {fila(t('admin.detalle.razonSocial'), detalle.prestador.razonSocial ?? <SinDato />)}
            {fila(t('admin.detalle.rnt'), detalle.prestador.rnt)}
            {fila(t('admin.detalle.correo'), detalle.prestador.correoCorporativo)}
            {fila(t('admin.detalle.telefono'), detalle.prestador.telefono)}
            {fila(
              t('admin.detalle.politicas'),
              detalle.prestador.aceptoPoliticas
                ? t('admin.detalle.politicasSi', { fecha: formatearFecha(detalle.prestador.fechaRegistro, i18n.language) })
                : t('admin.detalle.politicasNo'),
            )}
            {fila(t('admin.detalle.estado'), <InsigniaCuenta estado={detalle.prestador.estado} />)}
          </dl>
          {error && (
            <p className="aviso-cuentas aviso-cuentas--error" role="alert">
              {t(`admin.errores.${error}`, { defaultValue: t('admin.errores.GENERICO') })}
            </p>
          )}
          {detalle.prestador.estado === 'Pendiente' && (
            <div className="detalle-prestador__acciones">
              <button
                type="button"
                className={`boton-principal${enviando ? ' deshabilitado' : ''}`}
                aria-disabled={enviando ? true : undefined}
                aria-busy={enviando ? true : undefined}
                onClick={() => {
                  if (!enviando) onAprobar(detalle.prestador)
                }}
              >
                {enviando ? t('admin.dialogo.enviando') : t('admin.detalle.aprobar')}
              </button>
              <button
                type="button"
                className="boton-secundario"
                onClick={() => {
                  if (!enviando) onRechazar(detalle.prestador)
                }}
              >
                {t('admin.detalle.rechazar')}
              </button>
            </div>
          )}
        </>
      )}
    </section>
  )
}
