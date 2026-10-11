import type { Ubicacion } from './catalogoApi'

/**
 * Cálculos geográficos del frontend. Único lugar con la fórmula de distancia: la ubicación (useUbicacion) y el
 * mapa (MapaOferta) la importan de aquí. Las distancias de los resultados las calcula ms-catalog ($geoNear).
 */

/** Centro de referencia de Santa Marta: Parque de los Novios. */
export const CENTRO_SANTA_MARTA: Ubicacion = { latitud: 11.242, longitud: -74.2117 }

/** Más allá de este radio, la ubicación se considera lejos de Santa Marta (no entra en el encuadre del mapa). */
export const RADIO_SANTA_MARTA_KM = 60

const RADIO_TIERRA_KM = 6371.0088

const aRadianes = (grados: number) => (grados * Math.PI) / 180

/** Distancia del círculo máximo entre dos puntos (fórmula de haversine), en kilómetros. */
export function distanciaKm(a: Ubicacion, b: Ubicacion): number {
  const dLat = aRadianes(b.latitud - a.latitud)
  const dLng = aRadianes(b.longitud - a.longitud)
  const h =
    Math.sin(dLat / 2) ** 2 + Math.cos(aRadianes(a.latitud)) * Math.cos(aRadianes(b.latitud)) * Math.sin(dLng / 2) ** 2
  return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1, Math.sqrt(h)))
}

export function estaLejosDeSantaMarta(ubicacion: Ubicacion): boolean {
  return distanciaKm(ubicacion, CENTRO_SANTA_MARTA) > RADIO_SANTA_MARTA_KM
}
