/**
 * Leaflet simulado para las pruebas: jsdom no dibuja mapas. Registra lo que el componente le pide (mapas creados y
 * eliminados, capas, marcadores, control de zoom, encuadres, rueda del mouse) para verificarlo. setup.ts lo aplica a
 * todas las pruebas con vi.mock('leaflet').
 */

type Punto = [number, number]
type Manejador = (evento: { type: string }) => void

class Emisor {
  private readonly manejadores = new Map<string, Manejador[]>()

  on(tipos: string, manejador: Manejador) {
    for (const tipo of tipos.split(/\s+/)) {
      this.manejadores.set(tipo, [...(this.manejadores.get(tipo) ?? []), manejador])
    }
    return this
  }

  /** En las pruebas: simula un evento de Leaflet (click, focus, blur…). */
  fire(tipo: string) {
    for (const manejador of this.manejadores.get(tipo) ?? []) manejador({ type: tipo })
    return this
  }
}

class RuedaSimulada {
  private habilitada: boolean
  constructor(habilitada: boolean) {
    this.habilitada = habilitada
  }
  enable() {
    this.habilitada = true
  }
  disable() {
    this.habilitada = false
  }
  enabled() {
    return this.habilitada
  }
}

export interface IconoSimulado {
  options: { className?: string; html?: string }
}

export class MarcadorSimulado extends Emisor {
  readonly posicion: Punto
  readonly opciones: { icon: IconoSimulado; title?: string; keyboard?: boolean; zIndexOffset?: number }
  popup: unknown = null
  zIndexOffset: number
  readonly elemento: HTMLElement

  constructor(posicion: Punto, opciones: MarcadorSimulado['opciones']) {
    super()
    this.posicion = posicion
    this.opciones = opciones
    this.zIndexOffset = opciones.zIndexOffset ?? 0
    // Como Leaflet: el elemento del icono lleva la clase del divIcon, el title y, con keyboard, es enfocable.
    this.elemento = document.createElement('div')
    this.elemento.className = `leaflet-marker-icon ${opciones.icon.options.className ?? ''}`.trim()
    if (opciones.title) this.elemento.title = opciones.title
    if (opciones.keyboard) {
      this.elemento.tabIndex = 0
      this.elemento.setAttribute('role', 'button')
    }
  }

  bindPopup(contenido: unknown) {
    this.popup = contenido
    return this
  }

  getElement() {
    return this.elemento
  }

  setZIndexOffset(desplazamiento: number) {
    this.zIndexOffset = desplazamiento
    return this
  }
}

export class CapaSimulada {
  marcadores: MarcadorSimulado[] = []
  limpiezas = 0

  addTo(mapa: MapaSimulado) {
    mapa.capas.push(this)
    return this
  }

  addLayer(marcador: MarcadorSimulado) {
    this.marcadores.push(marcador)
    return this
  }

  clearLayers() {
    this.marcadores = []
    this.limpiezas += 1
    return this
  }
}

export class CapaTeselasSimulada {
  readonly url: string
  readonly opciones: Record<string, unknown>

  constructor(url: string, opciones: Record<string, unknown>) {
    this.url = url
    this.opciones = opciones
  }

  addTo(mapa: MapaSimulado) {
    mapa.capas.push(this)
    return this
  }
}

export class ControlZoomSimulado {
  readonly opciones: { zoomInTitle?: string; zoomOutTitle?: string; position?: string }
  mapa: MapaSimulado | null = null

  constructor(opciones: ControlZoomSimulado['opciones']) {
    this.opciones = opciones
  }

  addTo(mapa: MapaSimulado) {
    this.mapa = mapa
    mapa.controles.push(this)
    return this
  }

  remove() {
    if (this.mapa) this.mapa.controles = this.mapa.controles.filter((control) => control !== this)
    this.mapa = null
    return this
  }
}

export class MapaSimulado extends Emisor {
  eliminado = false
  capas: (CapaSimulada | CapaTeselasSimulada)[] = []
  controles: ControlZoomSimulado[] = []
  encuadres: { puntos: Punto[]; opciones: unknown }[] = []
  vistas: { centro: Punto; zoom: number }[] = []
  invalidaciones = 0
  readonly scrollWheelZoom: RuedaSimulada
  readonly contenedor: HTMLElement
  readonly opciones: Record<string, unknown> & { zoom?: number }
  private zoom: number

  constructor(contenedor: HTMLElement, opciones: MapaSimulado['opciones']) {
    super()
    this.contenedor = contenedor
    this.opciones = opciones
    this.zoom = opciones.zoom ?? 0
    this.scrollWheelZoom = new RuedaSimulada(opciones.scrollWheelZoom !== false)
  }

  remove() {
    this.eliminado = true
    return this
  }

  fitBounds(limites: { puntos: Punto[] }, opciones?: unknown) {
    this.encuadres.push({ puntos: limites.puntos, opciones })
    return this
  }

  setView(centro: Punto, zoom: number) {
    this.vistas.push({ centro, zoom })
    this.zoom = zoom
    return this
  }

  getZoom() {
    return this.zoom
  }

  invalidateSize() {
    this.invalidaciones += 1
    return this
  }

  /** Marcadores de las capas de resultados y de ubicación. */
  marcadores(): MarcadorSimulado[] {
    return this.capas.flatMap((capa) => (capa instanceof CapaSimulada ? capa.marcadores : []))
  }

  teselas(): CapaTeselasSimulada[] {
    return this.capas.filter((capa) => capa instanceof CapaTeselasSimulada)
  }
}

export const leafletSimulado = { mapas: [] as MapaSimulado[] }

export function reiniciarLeafletSimulado() {
  leafletSimulado.mapas = []
}

/** El único mapa sin eliminar; falla si hay cero o más de uno. */
export function mapaVivo(): MapaSimulado {
  const vivos = leafletSimulado.mapas.filter((mapa) => !mapa.eliminado)
  if (vivos.length !== 1) throw new Error(`Se esperaba un mapa vivo y hay ${vivos.length}`)
  return vivos[0]
}

export const moduloLeaflet = {
  map(contenedor: HTMLElement, opciones: Record<string, unknown>) {
    const mapa = new MapaSimulado(contenedor, opciones)
    leafletSimulado.mapas.push(mapa)
    return mapa
  },
  tileLayer: (url: string, opciones: Record<string, unknown>) => new CapaTeselasSimulada(url, opciones),
  layerGroup: () => new CapaSimulada(),
  marker: (posicion: Punto, opciones: MarcadorSimulado['opciones']) => new MarcadorSimulado(posicion, opciones),
  divIcon: (options: IconoSimulado['options']): IconoSimulado => ({ options }),
  latLngBounds: (puntos: Punto[]) => ({ puntos }),
  control: { zoom: (opciones: ControlZoomSimulado['opciones']) => new ControlZoomSimulado(opciones) },
}
