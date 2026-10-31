import { useId, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { CONDICIONES_ACCESIBILIDAD } from '../../catalogo/catalogoApi'
import { etiquetaCategoria } from '../../catalogo/componentes/etiquetas'
import { CampoServicio, ErrorGrupo } from '../../prestador/componentes/CampoServicio'
import { SelectorUbicacion } from '../../prestador/componentes/SelectorUbicacion'
import { idDeCampo, type ErroresServicio } from '../../prestador/rutasCampos'
import { CATEGORIAS_ATRACTIVO, ESTADOS_OPERATIVOS } from '../atractivosGestorApi'
import { MAXIMO_ATRACTIVO, textoErrorAtractivo, type BorradorAtractivo } from '../borradorAtractivo'

/**
 * Campos de WF-09 comunes al alta y a la edición, en el orden del wireframe ampliado (DEC-234): datos del atractivo
 * (con la descripción de RF06 y el pin del mapa sincronizado con latitud y longitud, DEC-223), accesibilidad (las tres
 * condiciones de RF27, DEC-221), aforo, estado y horario (RF07, RF33, RF58), las fotos (las pone quien lo usa, DEC-226)
 * y las normas ambientales (RF24). Los contadores cuentan puntos de código (DEC-227). El estado solo se elige al
 * registrar; en la edición cambia con su diálogo (DEC-210).
 */
export function FormularioAtractivo({
  borrador,
  errores,
  conEstado,
  fotos,
  onCambiar,
}: {
  borrador: BorradorAtractivo
  errores: ErroresServicio
  conEstado: boolean
  fotos: ReactNode
  onCambiar: (cambios: Partial<BorradorAtractivo>, rutas: string[]) => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const error = (ruta: string) => textoErrorAtractivo(t, ruta, errores[ruta])
  const idCategoria = idDeCampo('categoria')
  const idEstado = idDeCampo('estadoOperativo')
  const alternar = (valor: string) =>
    borrador.condicionesAccesibilidad.includes(valor)
      ? borrador.condicionesAccesibilidad.filter((actual) => actual !== valor)
      : [...borrador.condicionesAccesibilidad, valor]

  return (
    <>
      <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-datos`}>
        <h2 id={`${id}-datos`}>{t('gestor.formulario.secciones.datos')}</h2>
        <p className="campo__ayuda">{t('gestor.formulario.datosAyuda')}</p>
        <CampoServicio ruta="nombre" etiqueta={t('gestor.campos.nombre')} maximo={MAXIMO_ATRACTIVO.nombre}
          valor={borrador.nombre} onCambiar={(v) => onCambiar({ nombre: v }, ['nombre'])} error={error('nombre')} />
        <div className="campo">
          <label htmlFor={idCategoria}>{t('gestor.campos.categoria')}</label>
          <div className="campo__control">
            <select
              id={idCategoria}
              value={borrador.categoria}
              aria-invalid={error('categoria') ? true : undefined}
              aria-describedby={error('categoria') ? `${idCategoria}-error` : undefined}
              onChange={(evento) => onCambiar({ categoria: evento.target.value }, ['categoria'])}
            >
              <option value="">{t('gestor.formulario.elegirCategoria')}</option>
              {CATEGORIAS_ATRACTIVO.map((valor) => (
                <option key={valor} value={valor}>
                  {etiquetaCategoria(t, valor)}
                </option>
              ))}
            </select>
          </div>
          <ErrorGrupo ruta="categoria" error={error('categoria')} />
        </div>
        <CampoServicio ruta="descripcion" etiqueta={t('gestor.campos.descripcion')} multilinea
          maximo={MAXIMO_ATRACTIVO.descripcion} valor={borrador.descripcion}
          onCambiar={(v) => onCambiar({ descripcion: v }, ['descripcion'])} error={error('descripcion')} />
        <SelectorUbicacion
          tipo="atractivo"
          valor={{ latitud: borrador.latitud, longitud: borrador.longitud }}
          errores={errores}
          onCambiar={({ latitud, longitud }) =>
            onCambiar({ latitud, longitud }, ['coordenadas', 'coordenadas.latitud', 'coordenadas.longitud'])
          }
        />
      </section>

      <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-accesibilidad`}>
        <h2 id={`${id}-accesibilidad`}>{t('gestor.formulario.secciones.accesibilidad')}</h2>
        <fieldset
          id={idDeCampo('condicionesAccesibilidad')}
          tabIndex={-1}
          className="prestador__grupo"
          aria-describedby={error('condicionesAccesibilidad') ? `${idDeCampo('condicionesAccesibilidad')}-error` : undefined}
        >
          <legend className="solo-lector">{t('gestor.formulario.secciones.accesibilidad')}</legend>
          <ul className="prestador__casillas">
            {CONDICIONES_ACCESIBILIDAD.map((condicion) => (
              <li key={condicion}>
                <label>
                  <input
                    type="checkbox"
                    checked={borrador.condicionesAccesibilidad.includes(condicion)}
                    onChange={() =>
                      onCambiar({ condicionesAccesibilidad: alternar(condicion) }, ['condicionesAccesibilidad'])
                    }
                  />
                  {t(`catalogo.accesibilidad.${condicion}`)}
                </label>
              </li>
            ))}
          </ul>
          <ErrorGrupo ruta="condicionesAccesibilidad" error={error('condicionesAccesibilidad')} />
        </fieldset>
      </section>

      <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-aforo`}>
        <h2 id={`${id}-aforo`}>{t('gestor.formulario.secciones.aforo')}</h2>
        <p className="campo__ayuda">{t('gestor.formulario.aforoAyuda')}</p>
        <div className="prestador__columnas">
          <CampoServicio ruta="limiteCapacidadCarga" etiqueta={t('gestor.campos.capacidad')} inputMode="numeric"
            sufijo={t('gestor.formulario.personas')} valor={borrador.limiteCapacidadCarga}
            onCambiar={(v) => onCambiar({ limiteCapacidadCarga: v }, ['limiteCapacidadCarga'])}
            error={error('limiteCapacidadCarga')} />
          {conEstado && (
            <div className="campo">
              <label htmlFor={idEstado}>{t('gestor.campos.estado')}</label>
              <div className="campo__control">
                <select
                  id={idEstado}
                  value={borrador.estadoOperativo}
                  aria-invalid={error('estadoOperativo') ? true : undefined}
                  aria-describedby={[`${idEstado}-ayuda`, error('estadoOperativo') ? `${idEstado}-error` : null]
                    .filter(Boolean)
                    .join(' ')}
                  onChange={(evento) => onCambiar({ estadoOperativo: evento.target.value }, ['estadoOperativo'])}
                >
                  {ESTADOS_OPERATIVOS.map((estado) => (
                    <option key={estado} value={estado}>
                      {t(`catalogo.estados.${estado}`)}
                    </option>
                  ))}
                </select>
              </div>
              <p id={`${idEstado}-ayuda`} className="campo__ayuda">
                {t('gestor.formulario.estadoAyuda')}
              </p>
              <ErrorGrupo ruta="estadoOperativo" error={error('estadoOperativo')} />
            </div>
          )}
        </div>
        <CampoServicio ruta="horarioAtencion" etiqueta={t('gestor.campos.horario')} opcional multilinea
          maximo={MAXIMO_ATRACTIVO.horarioAtencion} ayuda={t('gestor.formulario.horarioAyuda')}
          valor={borrador.horarioAtencion} onCambiar={(v) => onCambiar({ horarioAtencion: v }, ['horarioAtencion'])}
          error={error('horarioAtencion')} />
      </section>

      {fotos}

      <section className="tarjeta prestador__seccion" aria-labelledby={`${id}-normas`}>
        <h2 id={`${id}-normas`}>{t('gestor.formulario.secciones.normas')}</h2>
        <CampoServicio ruta="normasAmbientales" etiqueta={t('gestor.campos.normas')} opcional multilinea
          maximo={MAXIMO_ATRACTIVO.normasAmbientales} ayuda={t('gestor.formulario.normasAyuda')}
          valor={borrador.normasAmbientales}
          onCambiar={(v) => onCambiar({ normasAmbientales: v }, ['normasAmbientales'])}
          error={error('normasAmbientales')} />
      </section>
    </>
  )
}

/** Elemento de WF-09 sin RF (DEC-219, DEC-228): visible, deshabilitado y sin texto que lo presente como obligatorio. */
export function DatoNoDisponible({ etiqueta }: { etiqueta: string }) {
  const { t } = useTranslation()
  return (
    <div role="group" aria-disabled="true" aria-label={etiqueta} className="prestador__no-disponible">
      <span>{etiqueta}</span>
      <span className="deshabilitado">
        <span aria-hidden="true">—</span>
        <span className="solo-lector">{t('comun.noDisponible')}</span>
      </span>
    </div>
  )
}
