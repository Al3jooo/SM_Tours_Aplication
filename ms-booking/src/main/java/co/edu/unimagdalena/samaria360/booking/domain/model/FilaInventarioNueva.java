package co.edu.unimagdalena.samaria360.booking.domain.model;

/**
 * Fila que crea la materialización de la oferta (DEC-135): los cupos totales salen de la capacidad configurada en el
 * catálogo (habitaciones de la unidad o aforo del tour). Si la fila ya existe, no se toca.
 */
public record FilaInventarioNueva(DiaInventario dia, CategoriaReservable categoria, int cupos) {

    public FilaInventarioNueva {
        if (cupos <= 0) {
            throw new IllegalArgumentException("cupos debe ser mayor que 0");
        }
    }
}
