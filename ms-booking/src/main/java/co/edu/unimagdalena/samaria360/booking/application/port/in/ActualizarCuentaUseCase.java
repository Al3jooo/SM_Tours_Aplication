package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;

/**
 * Recibe el estado de una cuenta desde ms-auth (ADR-008). Idempotente por versión: tolera duplicados y desorden. Es lo
 * único que ms-booking recibe de otro servicio por mensaje; no publica nada (D7 precisada en ADR-008).
 */
public interface ActualizarCuentaUseCase {

    ResultadoActualizacion actualizar(Cuenta cuenta);
}
