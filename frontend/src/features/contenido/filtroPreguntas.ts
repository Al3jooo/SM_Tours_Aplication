import type { PreguntaFrecuente } from './contenidoApi'

/**
 * Texto comparable para RF47 (DEC-62, DEC-71): sin tildes ni otras marcas diacríticas (NFD y `\p{M}`), en minúsculas
 * del español y sin los espacios de los extremos. «  Café » y «cafe» quedan iguales.
 */
export function normalizarTexto(texto: string): string {
  return texto.normalize('NFD').replace(/\p{M}/gu, '').toLocaleLowerCase('es').trim()
}

/** Pregunta con su título y su cuerpo ya normalizados: se calcula una vez por lista, no en cada pulsación. */
export interface PreguntaIndexada {
  pregunta: PreguntaFrecuente
  texto: string
}

export function indexarPreguntas(preguntas: PreguntaFrecuente[]): PreguntaIndexada[] {
  return preguntas.map((pregunta) => ({
    pregunta,
    // Un separador que el término normalizado no puede contener: una coincidencia no cruza del título al cuerpo.
    texto: `${normalizarTexto(pregunta.titulo)}\n${normalizarTexto(pregunta.cuerpo)}`,
  }))
}

/**
 * RF47: las preguntas cuyo título o cuerpo contienen el término (coincidencia exacta o parcial), sin distinguir
 * mayúsculas ni tildes. Un término vacío (o solo con espacios) devuelve todas. Conserva el orden del servidor (DEC-63).
 */
export function filtrarPreguntas(indice: PreguntaIndexada[], termino: string): PreguntaFrecuente[] {
  const buscado = normalizarTexto(termino)
  if (buscado === '') return indice.map((entrada) => entrada.pregunta)
  return indice.filter((entrada) => entrada.texto.includes(buscado)).map((entrada) => entrada.pregunta)
}
