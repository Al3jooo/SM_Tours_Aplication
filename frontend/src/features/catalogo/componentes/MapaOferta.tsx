import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import type { ResultadoOferta, Ubicacion } from '../catalogoApi'
import { CENTRO_SANTA_MARTA } from '../geo'

/**
 * Política de uso de las teselas de OpenStreetMap (único recurso externo permitido, ver CLAUDE.md): atribución
 * siempre visible, zoom máximo 19, solo las teselas del área visible (sin prefetching, caché offline ni descarga
 * masiva) y el Referer por defecto del navegador.
 */
const URL_TESELAS = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const ATRIBUCION_OSM = '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
const ZOOM_MAXIMO = 19
const ZOOM_INICIAL = 12
const ZOOM_MAXIMO_ENCUADRE = 15
const ZOOM_UBICACION = 14
const RELLENO_ENCUADRE: L.PointTuple = [32, 32]
const DESPLAZAMIENTO_RESALTADO = 500

/** SVG fijos (sin datos de la API). El color sale de la clase del marcador, con los tokens de estilos.css. */
const SVG_PIN =
  '<svg viewBox="0 0 28 36" width="28" height="36" aria-hidden="true" focusable="false">' +
  '<path class="marcador__pin" d="M14 1.5C7.1 1.5 1.75 6.8 1.75 13.5 1.75 22.6 14 34.5 14 34.5s12.25-11.9 12.25-21C26.25 6.8 20.9 1.5 14 1.5Z"/>' +
  '<circle class="marcador__centro" cx="14" cy="13.5" r="4.5"/></svg>'
const SVG_UBICACION =
  '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true" focusable="false">' +
  '<circle class="marcador__punto" cx="12" cy="12" r="8.5"/></svg>'

/** Clave de un resultado en la página (la misma que usa la lista para el resaltado marcador↔tarjeta). */
export function claveResultado(resultado: Pick<ResultadoOferta, 'tipo' | 'id'>): string {
  return `${resultado.tipo}-${resultado.id}`
}

function iconoResultado(tipo: ResultadoOferta['tipo']): L.DivIcon {
  return L.divIcon({
    className: `marcador marcador--${tipo === 'Servicio' ? 'servicio' : 'atractivo'}`,
    html: SVG_PIN,
    iconSize: [28, 36],
    iconAnchor: [14, 35],
    popupAnchor: [0, -32],
  })
}

function iconoUbicacion(): L.DivIcon {
  return L.divIcon({ className: 'marcador marcador--ubicacion', html: SVG_UBICACION, iconSize: [24, 24], iconAnchor: [12, 12] })
}

/** Contenido de la ventana emergente como texto (nunca HTML con datos de la API). */
function contenidoPopup(texto: string): HTMLElement {
  const elemento = document.createElement('p')
  elemento.className = 'popup-oferta'
  elemento.textContent = texto
  return elemento
}

function agregarMarcador(capa: L.LayerGroup, posicion: Ubicacion, icono: L.DivIcon, nombre: string, zIndexOffset = 0) {
  const marcador = L.marker([posicion.latitud, posicion.longitud], { icon: icono, title: nombre, keyboard: true, zIndexOffset })
  marcador.bindPopup(contenidoPopup(nombre))
  capa.addLayer(marcador)
  // Leaflet pone el title; el nombre accesible del marcador (role="button") se fija explícitamente.
  marcador.getElement()?.setAttribute('aria-label', nombre)
  return marcador
}

function encuadrar(mapa: L.Map | null, puntos: L.LatLngTuple[]) {
  if (mapa && puntos.length > 0) {
    mapa.fitBounds(L.latLngBounds(puntos), { padding: RELLENO_ENCUADRE, maxZoom: ZOOM_MAXIMO_ENCUADRE })
  }
}

interface PropiedadesMapa {
  /** Resultados de la página actual; null mientras carga (se conservan los marcadores anteriores). */
  resultados: ResultadoOferta[] | null
  ubicacion?: Ubicacion
  /** Ubicación a más de RADIO_SANTA_MARTA_KM: se dibuja, pero no entra en el encuadre ni se centra en ella. */
  ubicacionLejana: boolean
  /** Sube cada vez que hay que centrar el mapa en la ubicación. */
  centrado: number
  /** Falso cuando el selector «Lista / Mapa» oculta el mapa. */
  visible: boolean
  resaltada: string | null
  onSeleccionar: (clave: string) => void
}

/**
 * Mapa de WF-05 con Leaflet directo (sin react-leaflet, por su licencia). El mapa se crea una sola vez y se
 * actualiza por capas; la limpieza llama a map.remove(), así que con el doble montaje de StrictMode queda uno vivo.
 */
export function MapaOferta({
  resultados,
  ubicacion,
  ubicacionLejana,
  centrado,
  visible,
  resaltada,
  onSeleccionar,
}: PropiedadesMapa) {
  const { t } = useTranslation()
  const id = useId()
  const contenedor = useRef<HTMLDivElement>(null)
  const mapa = useRef<L.Map | null>(null)
  const capaResultados = useRef<L.LayerGroup | null>(null)
  const capaUbicacion = useRef<L.LayerGroup | null>(null)
  const marcadores = useRef(new Map<string, L.Marker>())
  const puntosEncuadre = useRef<L.LatLngTuple[]>([])
  const alSeleccionar = useRef(onSeleccionar)

  useEffect(() => {
    alSeleccionar.current = onSeleccionar
  })

  useEffect(() => {
    const nuevo = L.map(contenedor.current!, {
      center: [CENTRO_SANTA_MARTA.latitud, CENTRO_SANTA_MARTA.longitud],
      zoom: ZOOM_INICIAL,
      maxZoom: ZOOM_MAXIMO,
      zoomControl: false,
      keyboard: true,
      // La rueda no hace zoom hasta que el mapa tiene el foco o un clic: no atrapa el desplazamiento de la página.
      scrollWheelZoom: false,
    })
    L.tileLayer(URL_TESELAS, { maxZoom: ZOOM_MAXIMO, attribution: ATRIBUCION_OSM, updateWhenIdle: true }).addTo(nuevo)
    nuevo.on('focus click', () => nuevo.scrollWheelZoom.enable())
    nuevo.on('blur mouseout', () => nuevo.scrollWheelZoom.disable())
    capaResultados.current = L.layerGroup().addTo(nuevo)
    capaUbicacion.current = L.layerGroup().addTo(nuevo)
    mapa.current = nuevo
    return () => {
      nuevo.remove()
      mapa.current = null
      capaResultados.current = null
      capaUbicacion.current = null
      marcadores.current.clear()
    }
  }, [])

  // Control de zoom con títulos traducidos (Leaflet los usa como title y aria-label): se reemplaza al cambiar el idioma.
  useEffect(() => {
    if (!mapa.current) return
    const control = L.control
      .zoom({ position: 'topright', zoomInTitle: t('mapa.acercar'), zoomOutTitle: t('mapa.alejar') })
      .addTo(mapa.current)
    return () => {
      control.remove()
    }
  }, [t])

  useEffect(() => {
    const capa = capaResultados.current
    if (!capa || resultados === null) return
    capa.clearLayers()
    marcadores.current.clear()
    for (const resultado of resultados) {
      const clave = claveResultado(resultado)
      const marcador = agregarMarcador(capa, resultado.coordenada, iconoResultado(resultado.tipo), resultado.nombre)
      marcador.on('click', () => alSeleccionar.current(clave))
      marcadores.current.set(clave, marcador)
    }
    const puntos: L.LatLngTuple[] = resultados.map(({ coordenada }) => [coordenada.latitud, coordenada.longitud])
    if (ubicacion && !ubicacionLejana) puntos.push([ubicacion.latitud, ubicacion.longitud])
    puntosEncuadre.current = puntos
    encuadrar(mapa.current, puntos)
    // Solo cuando cambian los resultados: la ubicación cambia antes que ellos (la consulta la incluye).
  }, [resultados])

  useEffect(() => {
    const capa = capaUbicacion.current
    if (!capa) return
    capa.clearLayers()
    if (ubicacion) agregarMarcador(capa, ubicacion, iconoUbicacion(), t('mapa.tuUbicacion'), 1000)
  }, [ubicacion, t])

  useEffect(() => {
    if (centrado === 0 || !ubicacion || ubicacionLejana || !mapa.current) return
    mapa.current.setView([ubicacion.latitud, ubicacion.longitud], Math.max(mapa.current.getZoom(), ZOOM_UBICACION))
    // Solo ante una nueva petición de centrar.
  }, [centrado])

  // Al mostrarse (selector «Lista / Mapa»), el contenedor cambió de tamaño estando oculto.
  useEffect(() => {
    if (!visible || !mapa.current) return
    mapa.current.invalidateSize()
    encuadrar(mapa.current, puntosEncuadre.current)
  }, [visible])

  useEffect(() => {
    marcadores.current.forEach((marcador, clave) => {
      const activo = clave === resaltada
      marcador.getElement()?.classList.toggle('marcador--resaltado', activo)
      marcador.setZIndexOffset(activo ? DESPLAZAMIENTO_RESALTADO : 0)
    })
  }, [resaltada, resultados])

  return (
    <div className="mapa-oferta">
      <div
        ref={contenedor}
        className="mapa-oferta__lienzo"
        role="region"
        aria-label={t('explorar.mapa')}
        aria-describedby={`${id}-rueda`}
        tabIndex={0}
      />
      <p id={`${id}-rueda`} className="mapa-oferta__indicacion">
        {t('mapa.indicacionRueda')}
      </p>
    </div>
  )
}
