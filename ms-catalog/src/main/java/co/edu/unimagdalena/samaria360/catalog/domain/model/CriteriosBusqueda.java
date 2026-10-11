package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Filtros combinables, orden y paginación de la búsqueda de servicios y atractivos (RF16, RF38, RF50, RF61).
 * Todos los filtros son opcionales (null o vacío = sin filtro).
 *
 * @param texto              nombre, vereda o actividad; se normaliza con trim y vacío equivale a null
 * @param categoria          categoría de servicio
 * @param precioMaximo       precio máximo, mayor o igual que 0
 * @param ubicacion          ubicación actual del turista, si la comparte (RF38)
 * @param distanciaMaximaKm  distancia máxima en línea recta; exige ubicación
 * @param accesibilidad      condiciones que deben cumplir los resultados (RF50)
 * @param orden              orden de los resultados (RF61); null = orden por defecto
 * @param pagina             número de página, desde 0
 * @param tamanoPagina       elementos por página, de 1 a 10 (RF16)
 */
public record CriteriosBusqueda(String texto, Categoria categoria, BigDecimal precioMaximo, Coordenada ubicacion,
                                Double distanciaMaximaKm, Set<CondicionAccesibilidad> accesibilidad,
                                OrdenBusqueda orden, int pagina, int tamanoPagina) {

    /** RF16: lista paginada de 10 elementos o menos por vista. */
    public static final int TAMANO_PAGINA_MAXIMO = 10;

    /** Acota el costo de cada búsqueda por texto (el texto, además, se escapa antes de usarse como regex). */
    public static final int LONGITUD_MAXIMA_TEXTO = 100;

    public CriteriosBusqueda {
        texto = texto == null || texto.isBlank() ? null : texto.trim();
        accesibilidad = accesibilidad == null ? Set.of() : Set.copyOf(accesibilidad);

        if (texto != null && texto.length() > LONGITUD_MAXIMA_TEXTO) {
            throw new CriteriosBusquedaInvalidosException(
                    "El texto de búsqueda admite como máximo " + LONGITUD_MAXIMA_TEXTO + " caracteres");
        }

        if (tamanoPagina < 1 || tamanoPagina > TAMANO_PAGINA_MAXIMO) {
            throw new CriteriosBusquedaInvalidosException(
                    "El tamaño de página debe estar entre 1 y " + TAMANO_PAGINA_MAXIMO + " (RF16)");
        }
        if (pagina < 0) {
            throw new CriteriosBusquedaInvalidosException("La página no puede ser negativa");
        }
        if (precioMaximo != null && precioMaximo.signum() < 0) {
            throw new CriteriosBusquedaInvalidosException("El precio máximo no puede ser negativo");
        }
        if (distanciaMaximaKm != null) {
            if (!Double.isFinite(distanciaMaximaKm) || distanciaMaximaKm <= 0) {
                throw new CriteriosBusquedaInvalidosException("La distancia máxima debe ser mayor que 0");
            }
            if (ubicacion == null) {
                throw new CriteriosBusquedaInvalidosException(
                        "La distancia máxima exige la ubicación del turista (RF38)");
            }
        }
        if (orden == OrdenBusqueda.Distancia && ubicacion == null) {
            throw new CriteriosBusquedaInvalidosException(
                    "Ordenar por distancia exige la ubicación del turista (RF61)");
        }
    }

    /** Primera página, de tamaño máximo, sin filtros ni orden. */
    public static CriteriosBusqueda sinFiltros() {
        return new CriteriosBusqueda(null, null, null, null, null, Set.of(), null, 0, TAMANO_PAGINA_MAXIMO);
    }

    /** Copia con otro orden (el caso de uso resuelve el orden por defecto). */
    public CriteriosBusqueda conOrden(OrdenBusqueda nuevoOrden) {
        return new CriteriosBusqueda(texto, categoria, precioMaximo, ubicacion, distanciaMaximaKm, accesibilidad,
                nuevoOrden, pagina, tamanoPagina);
    }
}
