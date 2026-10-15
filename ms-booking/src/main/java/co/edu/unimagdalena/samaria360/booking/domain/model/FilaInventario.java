package co.edu.unimagdalena.samaria360.booking.domain.model;

/** Una fila existente de {@code inventario_cupo}, con su id, para descontarla por id dentro de la transacción. */
public record FilaInventario(long idInventario, DiaInventario dia) {
}
