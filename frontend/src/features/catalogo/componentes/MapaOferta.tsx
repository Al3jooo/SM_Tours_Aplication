import type { TFunction } from 'i18next'
import * as L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import type { ResultadoOferta, Ubicacion } from '../catalogoApi'
import { rutaDetalle } from '../detalle/navegacionDetalle'
import { CENTRO_SANTA_MARTA } from '../geo'
import { imagenDeCategoria } from './etiquetas'

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
/** Miniatura del popup (RF23), 3:2 como las fotos del catálogo: tamaño fijo para que la ventana no salte al cargar. */
const ANCHO_MINIATURA = 120
const ALTO_MINIATURA = 80

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

/**
 * Ventana emergente de un resultado (RF23): miniatura, nombre y, en los servicios, «Ver detalle». Se arma con nodos y
 * textContent, nunca con HTML que lleve datos de la API. El enlace es un <a href> real (abre en otra pestaña con las
 * teclas de siempre); un clic normal navega dentro de la app con `onVerDetalle`.
 */
function contenidoPopup(resultado: ResultadoOferta, t: TFunction, onVerDetalle: (id: string) => void): HTMLElement {
  const contenedor = document.createElement('div')
  contenedor.className = 'popup-oferta'

  const respaldo = imagenDeCategoria(resultado.categoria, resultado.tipo)
  const miniatura = document.createElement('img')
  miniatura.className = 'popup-oferta__miniatura'
  miniatura.alt = t('mapa.miniatura', { nombre: resultado.nombre })
  miniatura.width = ANCHO_MINIATURA
  miniatura.height = ALTO_MINIATURA
  miniatura.decoding = 'async'
  miniatura.addEventListener('error', () => {
    if (miniatura.getAttribute('src') !== respaldo) miniatura.src = respaldo
  })
  miniatura.src = resultado.imagen ?? respaldo

  const nombre = document.createElement('p')
  nombre.className = 'popup-oferta__nombre'
  nombre.textContent = resultado.nombre
  contenedor.append(miniatura, nombre)

  if (resultado.tipo === 'Servicio') {
    const enlace = document.createElement('a')
    enlace.className = 'popup-oferta__enlace'
    enlace.href = rutaDetalle(resultado.id)
    enlace.textContent = t('catalogo.tarjeta.verDetalle')
    enlace.addEventListener('click', (evento) => {
      if (evento.button !== 0 || evento.metaKey || evento.ctrlKey || evento.shiftKey || evento.altKey) return
      evento.preventDefault()
      onVerDetalle(resultado.id)
    })
    contenedor.append(enlace)
  }
  return contenedor
}

/** Ventana emergente solo con un texto (el marcador «Tu ubicación»). */
function popupTexto(texto: string): HTMLElement {
  const contenedor = document.createElement('div')
  contenedor.className = 'popup-oferta'
  const parrafo = document.createElement('p')
  parrafo.className = 'popup-oferta__nombre'
  parrafo.textContent = texto
  contenedor.append(parrafo)
  return contenedor
}

function agregarMarcador(
  capa: L.LayerGroup,
  posicion: Ubicacion,
  icono: L.DivIcon,
  nombre: string,
  zIndexOffset = 0,
  popup?: () => HTMLElement,
) {
  const marcador = L.marker([posicion.latitud, posicion.longitud], { icon: icono, title: nombre, keyboard: true, zIndexOffset })
  if (popup) marcador.bindPopup(popup)
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
  /** «Ver detalle» del popup de un servicio. */
  onVerDetalle?: (idServicio: string) => void
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
  onVerDetalle,
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
  const alVerDetalle = useRef(onVerDetalle)
  // El popup se arma al abrirse: usa el idioma y el manejador vigentes.
  const traducir = useRef(t)

  useEffect(() => {
    alSeleccionar.current = onSeleccionar
    alVerDetalle.current = onVerDetalle
    traducir.current = t
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
      const marcador = agregarMarcador(capa, resultado.coordenada, iconoResultado(resultado.tipo), resultado.nombre, 0, () =>
        contenidoPopup(resultado, traducir.current, (idServicio) => alVerDetalle.current?.(idServicio)),
      )
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
    if (ubicacion) {
      const texto = t('mapa.tuUbicacion')
      agregarMarcador(capa, ubicacion, iconoUbicacion(), texto, 1000, () => popupTexto(texto))
    }
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
