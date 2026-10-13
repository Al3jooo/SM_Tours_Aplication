import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import {
  CATEGORIAS,
  SERVICIOS_INCLUIDOS,
  contarMasFiltros,
  soloDeCategoria,
  type Categoria,
  type MasFiltros as Filtros,
  type ServicioIncluido,
} from '../catalogoApi'
import { formatearHoras } from '../formato'
import type { OpcionesFiltro } from '../opcionesFiltroApi'
import { useOpcionesFiltro } from '../useOpcionesFiltro'
import { etiquetaCategoria } from './etiquetas'

/**
 * «Más filtros» de WF-05 (RF10): botón que despliega, dentro del formulario de filtros, la categoría y los filtros
 * propios de esa categoría. Los cambios se aplican con «Aplicar filtros», como los demás. Las opciones salen de los
 * servicios publicados (GET /api/v1/catalogo/filtros); no hay listas fijas en el código, salvo las etiquetas de los
 * servicios incluidos, que son un vocabulario cerrado.
 *
 * «Todas» quita la categoría y sus filtros: vuelven a aparecer los atractivos. Al cambiar de categoría se limpian los
 * filtros que ya no aplican. Al abrir, el foco va a la categoría; con Escape o «Cerrar», vuelve al botón.
 */
export function MasFiltros({
  categoria,
  filtros,
  onCambiar,
}: {
  categoria: Categoria | undefined
  filtros: Filtros
  onCambiar: (categoria: Categoria | undefined, filtros: Filtros) => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const [abierto, setAbierto] = useState(false)
  const boton = useRef<HTMLButtonElement>(null)
  const panel = useRef<HTMLDivElement>(null)
  const opciones = useOpcionesFiltro(categoria, abierto)
  const activos = contarMasFiltros(filtros)

  useEffect(() => {
    if (abierto) panel.current?.querySelector<HTMLInputElement>('input[type="radio"]:checked')?.focus()
  }, [abierto])

  const cerrar = () => {
    setAbierto(false)
    boton.current?.focus()
  }

  const alPulsarTecla = (evento: KeyboardEvent<HTMLDivElement>) => {
    if (evento.key === 'Escape') {
      evento.preventDefault()
      cerrar()
    }
  }

  const elegirCategoria = (nueva: Categoria | undefined) => onCambiar(nueva, soloDeCategoria(filtros, nueva))
  const cambiar = (cambios: Filtros) => onCambiar(categoria, soloDeCategoria({ ...filtros, ...cambios }, categoria))

  return (
    <>
      <div className="filtro mas-filtros__control">
        <button
          ref={boton}
          type="button"
          className="boton-secundario mas-filtros__boton"
          aria-expanded={abierto}
          aria-controls={`${id}-panel`}
          onClick={() => setAbierto((actual) => !actual)}
        >
          {t('explorar.masFiltros.boton')}
          {activos > 0 && (
            <span className="mas-filtros__contador">
              <span aria-hidden="true">{activos}</span>
              <span className="solo-lector">, {t('explorar.masFiltros.activos', { count: activos })}</span>
            </span>
          )}
        </button>
      </div>

      <div
        ref={panel}
        id={`${id}-panel`}
        className="mas-filtros__panel"
        role="group"
        aria-labelledby={`${id}-titulo`}
        hidden={!abierto}
        onKeyDown={alPulsarTecla}
      >
        <div className="mas-filtros__cabecera">
          <h3 id={`${id}-titulo`}>{t('explorar.masFiltros.titulo')}</h3>
          <button type="button" className="enlace mas-filtros__cerrar" onClick={cerrar}>
            {t('explorar.masFiltros.cerrar')}
          </button>
        </div>

        <fieldset className="filtro mas-filtros__categorias">
          <legend>{t('explorar.masFiltros.categoria')}</legend>
          <label className="casilla">
            <input
              type="radio"
              name={`${id}-categoria`}
              checked={categoria === undefined}
              onChange={() => elegirCategoria(undefined)}
            />
            {t('explorar.masFiltros.todas')}
          </label>
          {CATEGORIAS.map((opcion) => (
            <label key={opcion} className="casilla">
              <input
                type="radio"
                name={`${id}-categoria`}
                checked={categoria === opcion}
                onChange={() => elegirCategoria(opcion)}
              />
              {etiquetaCategoria(t, opcion)}
            </label>
          ))}
        </fieldset>

        <div className="mas-filtros__campos" aria-live="polite">
          {categoria === undefined && <p className="filtro__ayuda">{t('explorar.masFiltros.elegirCategoria')}</p>}
          {opciones.estado === 'cargando' && (
            <p className="filtro__ayuda" role="status">
              {t('explorar.masFiltros.cargando')}
            </p>
          )}
          {opciones.estado === 'error' && (
            <div className="mas-filtros__error">
              <p className="filtro__ayuda">{t('explorar.masFiltros.error')}</p>
              <button type="button" className="boton-secundario" onClick={opciones.reintentar}>
                {t('catalogo.busqueda.reintentar')}
              </button>
            </div>
          )}
          {opciones.estado === 'ok' && categoria && (
            <CamposCategoria categoria={categoria} opciones={opciones.opciones} filtros={filtros} onCambiar={cambiar} />
          )}
        </div>
      </div>
    </>
  )
}

/** Valores para una lista: los de las opciones más el elegido (si llegó por URL y ya no se ofrece). */
function conElegido(valores: string[] | undefined, elegido: string | undefined): string[] {
  const lista = valores ?? []
  return elegido && !lista.includes(elegido) ? [...lista, elegido] : lista
}

function CamposCategoria({
  categoria,
  opciones,
  filtros,
  onCambiar,
}: {
  categoria: Categoria
  opciones: OpcionesFiltro
  filtros: Filtros
  onCambiar: (cambios: Filtros) => void
}) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const horas = (valor: number) => t('explorar.masFiltros.horas', { horas: formatearHoras(valor, i18n.language) })

  const lista = (campo: 'tipoHabitacion' | 'tipoCocina' | 'nivelDificultad' | 'tipoVehiculoEquipo', valores?: string[]) => {
    const opcionesLista = conElegido(valores, filtros[campo])
    if (opcionesLista.length === 0) return null
    return (
      <div className="filtro">
        <label htmlFor={`${id}-${campo}`}>{t(`explorar.masFiltros.${campo}`)}</label>
        <select
          id={`${id}-${campo}`}
          className="campo-lista"
          value={filtros[campo] ?? ''}
          onChange={(evento) => onCambiar({ [campo]: evento.target.value || undefined } as Filtros)}
        >
          <option value="">{t('explorar.masFiltros.cualquiera')}</option>
          {opcionesLista.map((valor) => (
            <option key={valor} value={valor}>
              {valor}
            </option>
          ))}
        </select>
      </div>
    )
  }

  let campos: ReactNode
  switch (categoria) {
    case 'Alojamiento': {
      const elegidos = new Set(filtros.serviciosIncluidos ?? [])
      const servicios = SERVICIOS_INCLUIDOS.filter((s) => opciones.serviciosIncluidos?.includes(s) || elegidos.has(s))
      const alternar = (servicio: ServicioIncluido) => {
        const nuevos = new Set(elegidos)
        if (nuevos.has(servicio)) nuevos.delete(servicio)
        else nuevos.add(servicio)
        onCambiar({ serviciosIncluidos: SERVICIOS_INCLUIDOS.filter((s) => nuevos.has(s)) })
      }
      campos = (
        <>
          {servicios.length > 0 && (
            <fieldset className="filtro">
              <legend>{t('explorar.masFiltros.serviciosIncluidos')}</legend>
              {servicios.map((servicio) => (
                <label key={servicio} className="casilla">
                  <input type="checkbox" checked={elegidos.has(servicio)} onChange={() => alternar(servicio)} />
                  {t(`catalogo.serviciosIncluidos.${servicio}`)}
                </label>
              ))}
            </fieldset>
          )}
          {opciones.huespedes && (
            <div className="filtro">
              <label htmlFor={`${id}-huespedes`}>{t('explorar.masFiltros.huespedes')}</label>
              <input
                id={`${id}-huespedes`}
                className="campo-numero"
                type="number"
                inputMode="numeric"
                min={1}
                max={opciones.huespedes.maximo}
                step={1}
                value={filtros.huespedes ?? ''}
                aria-describedby={`${id}-huespedes-ayuda`}
                onChange={(evento) => {
                  const valor = Number.parseInt(evento.target.value, 10)
                  onCambiar({ huespedes: Number.isInteger(valor) && valor >= 1 ? valor : undefined })
                }}
              />
              <p id={`${id}-huespedes-ayuda`} className="filtro__ayuda">
                {t('explorar.masFiltros.huespedesAyuda', { maximo: opciones.huespedes.maximo })}
              </p>
            </div>
          )}
          {lista('tipoHabitacion', opciones.tiposHabitacion)}
        </>
      )
      break
    }
    case 'Gastronomia': {
      const sinOpcion = !opciones.opcionVegetariana && !filtros.menuVegetariano
      campos = (
        <>
          <div className="filtro">
            <label className="casilla">
              <input
                type="checkbox"
                checked={filtros.menuVegetariano ?? false}
                disabled={sinOpcion}
                aria-describedby={sinOpcion ? `${id}-sin-vegetariana` : undefined}
                onChange={(evento) => onCambiar({ menuVegetariano: evento.target.checked || undefined })}
              />
              {t('explorar.masFiltros.opcionVegetariana')}
            </label>
            {sinOpcion && (
              <p id={`${id}-sin-vegetariana`} className="filtro__ayuda">
                {t('explorar.masFiltros.sinOpcionVegetariana')}
              </p>
            )}
          </div>
          {lista('tipoCocina', opciones.tiposCocina)}
        </>
      )
      break
    }
    case 'Tour': {
      const rango = opciones.duracionHoras
      const valor = filtros.duracionMaxHoras
      campos = (
        <>
          {lista('nivelDificultad', opciones.nivelesDificultad)}
          {rango && rango.maximo > rango.minimo && (
            <div className="filtro">
              <label htmlFor={`${id}-duracion`}>{t('explorar.masFiltros.duracionMax')}</label>
              <input
                id={`${id}-duracion`}
                type="range"
                min={rango.minimo}
                max={rango.maximo}
                step={0.5}
                value={valor === undefined ? rango.maximo : Math.min(Math.max(valor, rango.minimo), rango.maximo)}
                aria-valuetext={valor === undefined ? t('explorar.sinLimite') : horas(valor)}
                onChange={(evento) => {
                  const elegidas = Number(evento.target.value)
                  onCambiar({ duracionMaxHoras: elegidas >= rango.maximo ? undefined : elegidas })
                }}
              />
              <output htmlFor={`${id}-duracion`} className="filtro__valor">
                {valor === undefined ? t('explorar.sinLimite') : horas(valor)}
              </output>
            </div>
          )}
        </>
      )
      break
    }
    case 'AlquilerEquipo':
      campos = lista('tipoVehiculoEquipo', opciones.tiposVehiculoEquipo)
      break
  }

  const vacia =
    !opciones.serviciosIncluidos?.length &&
    !opciones.tiposHabitacion?.length &&
    !opciones.huespedes &&
    !opciones.opcionVegetariana &&
    !opciones.tiposCocina?.length &&
    !opciones.nivelesDificultad?.length &&
    !opciones.duracionHoras &&
    !opciones.tiposVehiculoEquipo?.length
  return (
    <>
      {vacia && <p className="filtro__ayuda">{t('explorar.masFiltros.sinOpciones')}</p>}
      <div className="mas-filtros__rejilla">{campos}</div>
    </>
  )
}
