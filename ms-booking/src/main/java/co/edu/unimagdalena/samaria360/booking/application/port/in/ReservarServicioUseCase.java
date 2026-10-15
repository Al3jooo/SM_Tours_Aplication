package co.edu.unimagdalena.samaria360.booking.application.port.in;

/**
 * CU «Reservar servicio» (RF11, RF20, RF32, RF37): confirmación automática con descuento inmediato de cupos,
 * idempotente por turista y clave.
 */
public interface ReservarServicioUseCase {

    ResultadoReserva reservar(ComandoReserva comando);
}
