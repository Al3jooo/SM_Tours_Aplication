/** Términos y condiciones (WF-15, RF46). Lo usan el pie, el pie reducido y el texto legal del login (WF-01). */
export const RUTA_TERMINOS = '/terminos'

/** Ancla de la sección 4 de WF-15, «Datos personales (Ley 1581 de 2012)» (DEC-68). */
export const ANCLA_PRIVACIDAD = 'privacidad'

/**
 * Destino de la Política de Privacidad (RF46; requisito derivado D6, Ley 1581): la sección 4 de los términos (DEC-68).
 * Lo comparten el pie de página, el pie reducido, el login y la casilla del registro, para que siempre lleven al mismo
 * lugar.
 */
export const RUTA_POLITICA_PRIVACIDAD = `${RUTA_TERMINOS}#${ANCLA_PRIVACIDAD}`

/** Sección pública de preguntas frecuentes (WF-13, RF26), desde la columna «Legal y Soporte» del pie. */
export const RUTA_PREGUNTAS_FRECUENTES = '/preguntas-frecuentes'
