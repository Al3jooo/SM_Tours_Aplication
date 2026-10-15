package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ComandoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ClaveIdempotenciaRepetidaException;
import co.edu.unimagdalena.samaria360.booking.application.service.ReservarServicioService;
import co.edu.unimagdalena.samaria360.booking.application.service.ReservarServicioService.Preparacion;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Coordina las fases de {@link ReservarServicioService} con las transacciones (D3, D4, D6):
 * <ol>
 *   <li>preparar <b>sin transacción</b>: la llamada HTTP al catálogo nunca ocurre con filas bloqueadas;</li>
 *   <li>confirmar en una transacción: cualquier fallo (fecha sin inventario, cupo agotado) la revierte entera;</li>
 *   <li>si otro pedido con la misma clave confirmó primero, esta transacción se revirtió en el insert y se lee la
 *   reserva ganadora en una transacción nueva: el cliente recibe la misma respuesta, nunca un 500.</li>
 * </ol>
 */
public class ReservarServicioTransaccional implements ReservarServicioUseCase {

    private final ReservarServicioService servicio;
    private final TransactionTemplate transaccion;

    public ReservarServicioTransaccional(ReservarServicioService servicio, TransactionTemplate transaccion) {
        this.servicio = servicio;
        this.transaccion = transaccion;
    }

    @Override
    public ResultadoReserva reservar(ComandoReserva comando) {
        Preparacion preparacion = servicio.preparar(comando);
        if (preparacion.repetida() != null) {
            return new ResultadoReserva(preparacion.repetida(), true);
        }
        try {
            return new ResultadoReserva(transaccion.execute(estado -> servicio.confirmar(preparacion)), false);
        } catch (ClaveIdempotenciaRepetidaException carrera) {
            return transaccion.execute(estado -> servicio.resolverRepetida(preparacion));
        }
    }
}
