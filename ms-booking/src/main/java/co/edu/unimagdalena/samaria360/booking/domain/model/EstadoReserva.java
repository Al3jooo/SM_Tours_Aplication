package co.edu.unimagdalena.samaria360.booking.domain.model;

/**
 * Estados de una reserva. No hay «Pendiente»: RF20 confirma automáticamente. Nada asigna {@code Completada} hasta
 * RF52 (fuera de alcance), pero el modelo la contempla.
 */
public enum EstadoReserva {
    Confirmada,
    Cancelada,
    Completada
}
