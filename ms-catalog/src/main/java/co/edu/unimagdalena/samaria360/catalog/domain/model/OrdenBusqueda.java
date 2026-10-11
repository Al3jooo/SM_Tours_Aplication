package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Orden de los resultados de búsqueda (RF61). Por defecto: Distancia si hay ubicación; si no, Nombre.
 * «Calificación» queda pendiente: el modelo de datos todavía no tiene la calificación promedio (ver
 * docs/wireframes/README.md, «Discrepancias detectadas», punto 4).
 */
public enum OrdenBusqueda {
    Distancia,
    PrecioAscendente,
    PrecioDescendente,
    Nombre
}
