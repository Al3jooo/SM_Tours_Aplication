package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Campos adicionales según la categoría del servicio (RF10). Equivale al {@code oneOf} del esquema de
 * {@code servicios}: cada rama exige su propio detalle y no admite campos de otra categoría.
 */
public sealed interface DetalleCategoria
        permits DetalleAlojamiento, DetalleGastronomia, DetalleTour, DetalleAlquilerEquipo {

    Categoria categoria();
}
