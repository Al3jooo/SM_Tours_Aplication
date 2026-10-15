package co.edu.unimagdalena.samaria360.booking.application.port.in;

/**
 * RF52: pasa de Confirmada a Completada las reservas cuya actividad ya terminó ({@code fecha_fin} ≤ ahora). Es
 * idempotente: una segunda pasada no cambia nada. Requisito previo para calificar (RF17).
 */
public interface CompletarReservasUseCase {

    /** @return cuántas reservas cambiaron */
    int completar();
}
