package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

/** Normalización y comparación de los filtros de texto de {@link FiltrosCategoria}. */
final class TextoFiltro {

    private TextoFiltro() {
    }

    /** trim; vacío equivale a null. */
    static String normalizar(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.trim();
        if (limpio.length() > FiltrosCategoria.LONGITUD_MAXIMA_TEXTO) {
            throw new CriteriosBusquedaInvalidosException(
                    campo + " admite como máximo " + FiltrosCategoria.LONGITUD_MAXIMA_TEXTO + " caracteres");
        }
        return limpio;
    }

    /** Sin filtro, coincide todo; con filtro, el valor completo, sin distinguir mayúsculas (sí tildes). */
    static boolean coincide(String filtro, String valor) {
        return filtro == null || (valor != null && valor.equalsIgnoreCase(filtro));
    }
}
