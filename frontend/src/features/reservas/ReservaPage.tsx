import { useEffect, useId, useMemo, useRef, useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Campo } from '../auth/registro/Campo'
import { useSesion } from '../auth/useSesion'
import { ErrorDetalleServicio } from '../catalogo/detalle/DetalleServicioPage'
import { useDetalleServicio, type DetalleServicio } from '../catalogo/detalle/detalleApi'
import { leerVolverA, rutaDetalle } from '../catalogo/detalle/navegacionDetalle'
import { etiquetaCategoria } from '../catalogo/componentes/etiquetas'
import { ConfirmacionReserva } from './componentes/ConfirmacionReserva'
import { HorariosTour } from './componentes/HorariosTour'
import { ResumenReserva, type DatosResumen } from './componentes/ResumenReserva'
import { UnidadesAlojamiento } from './componentes/UnidadesAlojamiento'
import { buscarHorario, cupoUnidad, horariosDelDia } from './cupos'
import { centavos, formatearCentavos, totalAlojamiento, totalTour } from './dinero'
import { fechasDelRango, formatearFecha, formatearHora, hoyBogota, sumarDias, ventana } from './fechasBogota'
import { cuerpoReserva, type PedidoReserva } from './reservasApi'
import { MAXIMO_CANTIDAD, escribirSeleccion, leerSeleccion, type Seleccion } from './seleccionUrl'
import { useDisponibilidad } from './useDisponibilidad'
import { useIntentoReserva } from './useIntentoReserva'
import {
  ajustarSalida,
  leerEntero,
  limitesSalida,
  validarBusquedaAlojamiento,
  validarBusquedaTour,
  validarHabitaciones,
  validarPersonasTour,
  type CodigoValidacion,
} from './validacionReserva'
import '../catalogo/catalogo.css'
import '../catalogo/detalle/detalle.css'
import './reservas.css'

type ServicioReservable = Extract<DetalleServicio, { categoria: 'Alojamiento' | 'Tour' }>

/** Errores de reservar después de los cuales la disponibilidad mostrada ya no es la real. */
const RECARGAN_DISPONIBILIDAD = new Set(['CUPO_AGOTADO', 'FECHA_NO_DISPONIBLE', 'UNIDAD_NO_DISPONIBLE'])

/**
 * WF-10: disponibilidad y reserva (RF11, RF20, RF37, RF44). Consultar es público; confirmar exige una sesión de
 * Turista (sin sesión, el login vuelve aquí con la misma selección, que vive en la URL).
 */
export function ReservaPage() {
  const { t } = useTranslation()
  const { id = '' } = useParams()
  const ubicacion = useLocation()
  const volverA = leerVolverA(ubicacion.state)
  const detalle = useDetalleServicio(id)

  return (
    <div className="reserva">
      <p className="detalle__volver">
        <Link to={rutaDetalle(id)} state={volverA ? { volverA } : undefined}>
          <span aria-hidden="true">← </span>
          {t('reserva.volverDetalle')}
        </Link>
      </p>
      {detalle.estado === 'cargando' && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('detalle.cargando')}
        </p>
      )}
      {detalle.estado === 'error' && <ErrorDetalleServicio error={detalle.error} onReintentar={detalle.recargar} />}
      {detalle.estado === 'ok' &&
        (detalle.servicio.categoria === 'Alojamiento' || detalle.servicio.categoria === 'Tour' ? (
          <FormularioReserva servicio={detalle.servicio} recargarDetalle={detalle.recargar} />
        ) : (
          <div className="estado-catalogo estado-catalogo--vacio" role="status">
            <h1 className="reserva__titulo">{detalle.servicio.nombreComercial}</h1>
            <p className="estado-catalogo__mensaje">{t('reserva.noReservable')}</p>
          </div>
        ))}
    </div>
  )
}

function FormularioReserva({ servicio, recargarDetalle }: { servicio: ServicioReservable; recargarDetalle: () => void }) {
  const { t, i18n } = useTranslation()
  const id = useId()
  const { sesion } = useSesion()
  const irA = useNavigate()
  const ubicacion = useLocation()
  const [parametros, setParametros] = useSearchParams()
  const seleccion = useMemo(() => leerSeleccion(parametros), [parametros])
  const [hoy] = useState(() => hoyBogota())
  const { ultimaNoche, ultimaSalida } = ventana(hoy)
  const esAlojamiento = servicio.categoria === 'Alojamiento'
  const fecha = (valor: string) => formatearFecha(valor, i18n.language)

  // Borrador del formulario «Buscar disponibilidad»: las fechas se aplican (URL) al verificar.
  const [entrada, setEntrada] = useState(seleccion.entrada ?? '')
  const [salida, setSalida] = useState(seleccion.salida ?? '')
  const [dia, setDia] = useState(seleccion.fecha ?? '')
  const [personasTexto, setPersonasTexto] = useState(String(seleccion.personas ?? 1))
  const [habitacionesTexto, setHabitacionesTexto] = useState(String(seleccion.habitaciones ?? 1))
  const [errores, setErrores] = useState<Partial<Record<string, CodigoValidacion>>>({})
  const [anuncio, setAnuncio] = useState('')
  const [avisoRol, setAvisoRol] = useState(false)
  const [precioAceptado, setPrecioAceptado] = useState<{ firma: string; unitario: number | null; total: number } | null>(
    null,
  )
  const { envio, confirmar, descartar } = useIntentoReserva()
  const alerta = useRef<HTMLDivElement>(null)

  // Cada cambio de la selección lo hace el turista: deja atrás el error y el aviso del intento anterior.
  const actualizar = (cambios: Partial<Seleccion>) => {
    descartar()
    setAvisoRol(false)
    setParametros(escribirSeleccion({ ...seleccion, ...cambios }), { replace: true })
  }

  // Rango aplicado → disponibilidad. Solo con fechas válidas (las de la URL también pueden venir a mano).
  const rango = useMemo(() => {
    if (esAlojamiento) {
      const { entrada: e, salida: s } = seleccion
      if (!e || !s) return null
      const fallos = validarBusquedaAlojamiento({ entrada: e, salida: s, personas: 1 }, hoy)
      return fallos.entrada || fallos.salida ? null : { desde: e, hasta: sumarDias(s, -1) }
    }
    const f = seleccion.fecha
    if (!f || validarBusquedaTour({ fecha: f, personas: 1 }, hoy).fecha) return null
    return { desde: f, hasta: f }
  }, [esAlojamiento, seleccion, hoy])
  const disponibilidad = useDisponibilidad(servicio.id, rango)
  const recargarDisponibilidad = disponibilidad.recargar
  const datos = disponibilidad.estado === 'ok' ? disponibilidad.datos : null

  const pendienteVerificar = esAlojamiento
    ? entrada !== (seleccion.entrada ?? '') || salida !== (seleccion.salida ?? '')
    : dia !== (seleccion.fecha ?? '')

  // ---------------------------------------------------------------- selección y precio
  const personas = seleccion.personas
  const noches = esAlojamiento && rango && seleccion.entrada && seleccion.salida ? fechasDelRango(seleccion.entrada, seleccion.salida) : []
  const unidades = servicio.categoria === 'Alojamiento' ? servicio.detalleCategoria.unidades : []
  const unidadUrl = unidades.find((unidad) => unidad.idUnidad === seleccion.unidad)
  const cupo = unidadUrl && datos ? cupoUnidad(datos, unidadUrl.idUnidad, noches).cupo : 0
  const unidad = cupo > 0 ? unidadUrl : undefined
  const horarios = !esAlojamiento && datos && seleccion.fecha ? horariosDelDia(datos, seleccion.fecha) : []
  const horarioUrl = !esAlojamiento && datos && seleccion.fecha && seleccion.hora ? buscarHorario(datos, seleccion.fecha, seleccion.hora) : undefined
  const horario = horarioUrl && horarioUrl.cuposDisponibles > 0 ? horarioUrl : undefined
  const habitaciones = leerEntero(habitacionesTexto)

  const erroresSeleccion: Partial<Record<string, CodigoValidacion>> =
    servicio.categoria === 'Alojamiento'
      ? unidad && personas
        ? validarHabitaciones({ habitaciones, personas }, unidad, cupo)
        : {}
      : horario && personas
        ? validarPersonasTour(personas, servicio.detalleCategoria.aforoMaxGrupo, horario.cuposDisponibles)
        : {}

  const unitarioCatalogo = esAlojamiento ? centavos(unidad?.precioNoche) : centavos(servicio.precioBase)
  const pedidoSinPrecio: PedidoReserva | null =
    Object.keys(erroresSeleccion).length > 0 || !personas
      ? null
      : unidad && seleccion.entrada && seleccion.salida && habitaciones && !Number.isNaN(habitaciones)
        ? {
            categoria: 'Alojamiento',
            idServicio: servicio.id,
            idUnidad: unidad.idUnidad,
            fechaEntrada: seleccion.entrada,
            fechaSalida: seleccion.salida,
            cantidad: habitaciones,
            personas,
            totalCentavos: 0,
          }
        : horario && seleccion.fecha && seleccion.hora
          ? {
              categoria: 'Tour',
              idServicio: servicio.id,
              fechaEntrada: seleccion.fecha,
              horaInicio: seleccion.hora,
              personas,
              totalCentavos: 0,
            }
          : null
  const firma = pedidoSinPrecio ? cuerpoReserva(pedidoSinPrecio) : null
  const aceptado = precioAceptado && precioAceptado.firma === firma ? precioAceptado : null
  const totalCalculado =
    pedidoSinPrecio?.categoria === 'Alojamiento'
      ? totalAlojamiento(unitarioCatalogo, noches.length, pedidoSinPrecio.cantidad)
      : pedidoSinPrecio?.categoria === 'Tour'
        ? totalTour(unitarioCatalogo, pedidoSinPrecio.personas)
        : null
  const total = aceptado ? aceptado.total : totalCalculado
  const pedido: PedidoReserva | null = pedidoSinPrecio && total !== null ? { ...pedidoSinPrecio, totalCentavos: total } : null

  const motivo = (() => {
    if (disponibilidad.estado !== 'ok') return t('reserva.motivos.verificar')
    if (pendienteVerificar) return t('reserva.motivos.fechasCambiaron')
    if (esAlojamiento && !unidad) return t('reserva.motivos.elegirUnidad')
    if (!esAlojamiento && !horario) return t('reserva.motivos.elegirHorario')
    if (!pedidoSinPrecio) return t('reserva.motivos.revisarDatos')
    if (total === null) return t('reserva.motivos.sinTotal')
    return null
  })()

  useEffect(() => {
    if (envio.estado !== 'error') return
    if (RECARGAN_DISPONIBILIDAD.has(envio.codigo)) recargarDisponibilidad()
    if (envio.codigo === 'UNIDAD_NO_DISPONIBLE') recargarDetalle()
    alerta.current?.focus()
  }, [envio, recargarDisponibilidad, recargarDetalle])

  useEffect(() => {
    if (avisoRol) alerta.current?.focus()
  }, [avisoRol])

  // ---------------------------------------------------------------- acciones
  const verificar = (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    const personasLeidas = leerEntero(personasTexto)
    const fallos: Partial<Record<string, CodigoValidacion>> = esAlojamiento
      ? validarBusquedaAlojamiento({ entrada, salida, personas: personasLeidas }, hoy)
      : validarBusquedaTour({ fecha: dia, personas: personasLeidas }, hoy)
    if (personasLeidas !== undefined && personasLeidas > MAXIMO_CANTIDAD) fallos.personas = 'FORMATO'
    setErrores(fallos)
    const primero = Object.keys(fallos)[0]
    if (primero) {
      document.getElementById(`${id}-${primero}`)?.focus()
      return
    }
    const mismoRango = esAlojamiento
      ? entrada === seleccion.entrada && salida === seleccion.salida
      : dia === seleccion.fecha
    actualizar(
      esAlojamiento
        ? { entrada, salida, personas: personasLeidas }
        : { fecha: dia, personas: personasLeidas, hora: mismoRango ? seleccion.hora : undefined },
    )
    if (mismoRango) recargarDisponibilidad()
  }

  const cambiarEntrada = (valor: string) => {
    setEntrada(valor)
    const { salida: nuevaSalida, ajuste } = ajustarSalida(valor, salida, hoy)
    if (ajuste) {
      setSalida(nuevaSalida)
      setAnuncio(ajuste === 'ajustada' ? t('reserva.salidaAjustada', { fecha: fecha(nuevaSalida) }) : t('reserva.salidaBorrada'))
    }
  }

  const cambiarPersonas = (valor: string) => {
    setPersonasTexto(valor)
    const leidas = leerEntero(valor)
    if (leidas !== undefined && !Number.isNaN(leidas) && leidas >= 1 && leidas <= MAXIMO_CANTIDAD) {
      actualizar({ personas: leidas })
      setErrores((actuales) => {
        const resto = { ...actuales }
        delete resto.personas
        return resto
      })
    }
  }

  const cambiarHabitaciones = (valor: string) => {
    setHabitacionesTexto(valor)
    const leidas = leerEntero(valor)
    if (leidas !== undefined && !Number.isNaN(leidas) && leidas >= 1 && leidas <= MAXIMO_CANTIDAD) {
      actualizar({ habitaciones: leidas })
    }
  }

  const enviar = (pedidoAEnviar: PedidoReserva) => {
    if (sesion.estado === 'cargando') return
    if (sesion.estado === 'anonimo') {
      irA('/login', { state: { desde: ubicacion.pathname + ubicacion.search } })
      return
    }
    if (sesion.usuario.rol !== 'Turista') {
      setAvisoRol(true)
      return
    }
    setAvisoRol(false)
    void confirmar(sesion.usuario.idUsuario, pedidoAEnviar)
  }

  const aceptarPrecioNuevo = () => {
    if (envio.estado !== 'error' || !pedidoSinPrecio || !firma) return
    const nuevoTotal = centavos(envio.problema?.precioTotal as number | string | undefined)
    if (nuevoTotal === null) return
    setPrecioAceptado({
      firma,
      unitario: centavos(envio.problema?.precioUnitario as number | string | undefined),
      total: nuevoTotal,
    })
    enviar({ ...pedidoSinPrecio, totalCentavos: nuevoTotal })
  }

  if (envio.estado === 'confirmada') {
    return (
      <>
        <h1 className="reserva__titulo">{t('reserva.titulo', { servicio: servicio.nombreComercial })}</h1>
        <ConfirmacionReserva reserva={envio.reserva} />
      </>
    )
  }

  // ---------------------------------------------------------------- vista
  const mensaje = (campo: string, codigo: CodigoValidacion | undefined) => {
    if (!codigo) return undefined
    const parametrosMensaje: Record<string, string | number> = {
      fecha: fecha(campo === 'salida' ? ultimaSalida : ultimaNoche),
      personas: personas ?? 0,
      maximo:
        codigo === 'EXCEDE_AFORO' && servicio.categoria === 'Tour'
          ? servicio.detalleCategoria.aforoMaxGrupo
          : esAlojamiento
            ? cupo
            : (horario?.cuposDisponibles ?? 0),
    }
    return t(`reserva.validacion.${codigo}`, parametrosMensaje)
  }
  const errorDe = (campo: string) => mensaje(campo, errores[campo] ?? erroresSeleccion[campo])
  const limites = limitesSalida(entrada, hoy)

  const datosResumen: DatosResumen = {
    servicio: servicio.nombreComercial,
    categoria: servicio.categoria,
    unidad: unidad?.nombre,
    entrada: esAlojamiento ? seleccion.entrada : undefined,
    salida: esAlojamiento ? seleccion.salida : undefined,
    noches: esAlojamiento && noches.length > 0 ? noches.length : undefined,
    habitaciones: esAlojamiento && unidad && habitaciones && !Number.isNaN(habitaciones) ? habitaciones : undefined,
    fecha: esAlojamiento ? undefined : seleccion.fecha,
    hora: horario ? seleccion.hora : undefined,
    personas,
    unitario: aceptado?.unitario ?? ((esAlojamiento ? unidad : horario) ? unitarioCatalogo : null),
    total: pedido ? total : null,
    precioActualizado: aceptado !== null,
  }

  return (
    <>
      <header className="reserva__encabezado">
        <p className="detalle__categoria">{etiquetaCategoria(t, servicio.categoria)}</p>
        <h1 className="reserva__titulo">{t('reserva.titulo', { servicio: servicio.nombreComercial })}</h1>
      </header>

      <div className="reserva__cuerpo">
        <div className="reserva__principal">
          <form className="reserva__busqueda" noValidate onSubmit={verificar}>
            <fieldset className="reserva__grupo">
              <legend>{t('reserva.buscar')}</legend>
              <div className="reserva__campos">
                {esAlojamiento ? (
                  <>
                    <Campo
                      id={`${id}-entrada`}
                      etiqueta={t('reserva.entrada')}
                      type="date"
                      min={hoy}
                      max={ultimaNoche}
                      value={entrada}
                      ayuda={t('reserva.ayudaVentana', { fecha: fecha(ultimaNoche) })}
                      error={errorDe('entrada')}
                      onChange={(evento) => cambiarEntrada(evento.target.value)}
                    />
                    <Campo
                      id={`${id}-salida`}
                      etiqueta={t('reserva.salida')}
                      type="date"
                      min={limites?.min ?? sumarDias(hoy, 1)}
                      max={limites?.max ?? ultimaSalida}
                      value={salida}
                      ayuda={t('reserva.ayudaSalida')}
                      error={errorDe('salida')}
                      onChange={(evento) => setSalida(evento.target.value)}
                    />
                  </>
                ) : (
                  <Campo
                    id={`${id}-fecha`}
                    etiqueta={t('reserva.fecha')}
                    type="date"
                    min={hoy}
                    max={ultimaNoche}
                    value={dia}
                    ayuda={t('reserva.ayudaVentana', { fecha: fecha(ultimaNoche) })}
                    error={errorDe('fecha')}
                    onChange={(evento) => setDia(evento.target.value)}
                  />
                )}
                <Campo
                  id={`${id}-personas`}
                  etiqueta={t(esAlojamiento ? 'reserva.huespedes' : 'reserva.personas')}
                  type="number"
                  inputMode="numeric"
                  min={1}
                  max={MAXIMO_CANTIDAD}
                  value={personasTexto}
                  error={errorDe('personas')}
                  onChange={(evento) => cambiarPersonas(evento.target.value)}
                />
              </div>
              <button type="submit" className="boton-secundario reserva__verificar">
                {t('reserva.verificar')}
              </button>
            </fieldset>
          </form>
          <p className="reserva__anuncio" role="status">
            {anuncio}
          </p>

          <section className="reserva__disponibilidad" aria-labelledby={`${id}-disponibilidad`}>
            <h2 id={`${id}-disponibilidad`}>{t('reserva.disponibilidad')}</h2>
            <p className="reserva__consulta" role="status">
              {disponibilidad.estado === 'ok' &&
                t('reserva.consultadaEn', { hora: formatearHora(disponibilidad.consultadaEn, i18n.language) })}
            </p>
            {disponibilidad.estado === 'inactivo' && <p className="filtro__ayuda">{t('reserva.elegirFechas')}</p>}
            {disponibilidad.estado === 'cargando' && (
              <p className="estado-carga" role="status" aria-busy="true">
                {t('reserva.verificando')}
              </p>
            )}
            {disponibilidad.estado === 'error' && (
              <div className="estado-catalogo estado-catalogo--error" role="alert">
                <p className="estado-catalogo__mensaje">{t(`reserva.disponibilidadErrores.${disponibilidad.error}`)}</p>
                <button type="button" className="boton-secundario" onClick={recargarDisponibilidad}>
                  {t('catalogo.busqueda.reintentar')}
                </button>
              </div>
            )}
            {datos &&
              (servicio.categoria === 'Alojamiento' ? (
                datos.categoria === null ? (
                  <p className="estado-catalogo__mensaje">{t('reserva.sinInventario')}</p>
                ) : (
                  <>
                    <UnidadesAlojamiento
                      unidades={unidades}
                      serviciosIncluidos={servicio.detalleCategoria.serviciosIncluidos}
                      imagen={servicio.imagenes[0] ?? null}
                      disponibilidad={datos}
                      noches={noches}
                      seleccionada={unidad?.idUnidad}
                      onElegir={(idUnidad) => actualizar({ unidad: idUnidad })}
                    />
                    {unidad && (
                      <div className="reserva__campos">
                        <Campo
                          id={`${id}-habitaciones`}
                          etiqueta={t('reserva.habitaciones')}
                          type="number"
                          inputMode="numeric"
                          min={1}
                          max={cupo}
                          value={habitacionesTexto}
                          ayuda={t('reserva.ayudaHabitaciones', { count: unidad.capacidadMaxHuespedes })}
                          error={errorDe('habitaciones')}
                          onChange={(evento) => cambiarHabitaciones(evento.target.value)}
                        />
                      </div>
                    )}
                  </>
                )
              ) : horarios.length === 0 ? (
                <p className="estado-catalogo__mensaje">{t('reserva.sinHorarios')}</p>
              ) : (
                <HorariosTour
                  horarios={horarios}
                  seleccionado={horario ? seleccion.hora : undefined}
                  onElegir={(hora) => actualizar({ hora })}
                />
              ))}
          </section>

          {(envio.estado === 'error' || avisoRol) && (
            <div ref={alerta} tabIndex={-1} className="aviso-reserva" role="alert">
              {avisoRol ? (
                <p>{t('reserva.soloTuristas')}</p>
              ) : (
                envio.estado === 'error' && (
                  <ErrorEnvio
                    status={envio.status}
                    codigo={envio.codigo}
                    problema={envio.problema}
                    totalAnterior={totalCalculado}
                    onReintentar={() => pedido && enviar(pedido)}
                    onAceptarPrecio={aceptarPrecioNuevo}
                  />
                )
              )}
            </div>
          )}
        </div>

        <ResumenReserva
          datos={datosResumen}
          horario={datos}
          motivoNoConfirmable={motivo}
          enviando={envio.estado === 'enviando'}
          onConfirmar={() => pedido && enviar(pedido)}
        />
      </div>
    </>
  )
}

/** Mensaje de un intento fallido, con su acción (reintentar con la misma clave o aceptar el precio nuevo). */
function ErrorEnvio({
  status,
  codigo,
  problema,
  totalAnterior,
  onReintentar,
  onAceptarPrecio,
}: {
  status: number
  codigo: string
  problema: Record<string, unknown> | null
  totalAnterior: number | null
  onReintentar: () => void
  onAceptarPrecio: () => void
}) {
  const { t, i18n } = useTranslation()

  if (codigo === 'PRECIO_CAMBIADO') {
    const nuevo = centavos(problema?.precioTotal as number | string | undefined)
    return (
      <>
        <p>
          {t('reserva.errores.PRECIO_CAMBIADO', {
            anterior: totalAnterior === null ? '—' : formatearCentavos(totalAnterior, i18n.language),
            nuevo: nuevo === null ? '—' : formatearCentavos(nuevo, i18n.language),
          })}
        </p>
        {nuevo !== null && (
          <button type="button" className="boton-secundario" onClick={onAceptarPrecio}>
            {t('reserva.aceptarPrecio')}
          </button>
        )}
      </>
    )
  }

  // Red, 5xx o SERVICIO_NO_DISPONIBLE: la reserva pudo no llegar; se reintenta con la misma clave (D6).
  const reintentable = codigo === 'ERROR_RED' || status >= 500 || codigo === 'SERVICIO_NO_DISPONIBLE'
  if (reintentable) {
    return (
      <>
        <p>
          {t(
            codigo === 'ERROR_RED'
              ? 'reserva.errores.red'
              : problema?.servicio === 'ms-catalog'
                ? 'reserva.errores.catalogoNoDisponible'
                : 'reserva.errores.noDisponible',
          )}
        </p>
        <button type="button" className="boton-secundario" onClick={onReintentar}>
          {t('catalogo.busqueda.reintentar')}
        </button>
      </>
    )
  }

  if (codigo === 'DATOS_INVALIDOS') {
    const campos = (problema?.campos ?? {}) as Record<string, string>
    return (
      <>
        <p>{t('reserva.errores.DATOS_INVALIDOS')}</p>
        <ul>
          {Object.entries(campos).map(([campo, motivo]) => (
            <li key={campo}>
              {t(`reserva.campos.${campo}`, { defaultValue: campo })}:{' '}
              {t(`reserva.validacion.${motivo}`, { defaultValue: motivo, fecha: '', personas: '', maximo: '' })}
            </li>
          ))}
        </ul>
      </>
    )
  }

  if (codigo === 'ACCESO_DENEGADO') return <p>{t('reserva.soloTuristas')}</p>
  return <p>{t(`reserva.errores.${codigo}`, { defaultValue: t('reserva.errores.GENERICO') })}</p>
}
