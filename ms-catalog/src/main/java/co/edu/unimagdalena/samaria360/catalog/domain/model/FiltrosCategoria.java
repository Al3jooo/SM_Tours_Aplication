package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * «Más filtros» de WF-05: filtros sobre el detalle de una categoría (RF10), combinables con los de RF16. Solo
 * aplican a servicios de esa categoría, así que la búsqueda debe llevar {@code categoria} igual a la suya.
 * <p>
 * Los textos se comparan completos, sin distinguir mayúsculas pero sí tildes ({@code cabaña} coincide con
 * {@code Cabaña}; {@code Cabana} no), igual que la expresión regular anclada con la opción {@code i} del adaptador
 * MongoDB. Las opciones que ofrece el frontend salen de los datos ({@link OpcionesFiltro}), así que llegan exactas.
 */
public sealed interface FiltrosCategoria
        permits FiltrosAlojamiento, FiltrosGastronomia, FiltrosTour, FiltrosAlquilerEquipo {

    /** Longitud máxima de un filtro de texto, como el texto de búsqueda. */
    int LONGITUD_MAXIMA_TEXTO = CriteriosBusqueda.LONGITUD_MAXIMA_TEXTO;

    Categoria categoria();

    /** Nombres de los parámetros con valor, en el orden de la API. Vacía = sin filtros. */
    List<String> camposActivos();

    /**
     * Si el detalle de un servicio de la categoría cumple todos los filtros.
     *
     * @param precioMaximo precio máximo de la búsqueda (RF16), o null. Solo lo usa Alojamiento, que lo exige a la
     *                     misma unidad que cumple los filtros de unidad; para el precio del servicio sigue valiendo
     *                     {@code precioBase} (ver {@link FiltrosAlojamiento})
     */
    boolean cumple(DetalleCategoria detalle, BigDecimal precioMaximo);
}
