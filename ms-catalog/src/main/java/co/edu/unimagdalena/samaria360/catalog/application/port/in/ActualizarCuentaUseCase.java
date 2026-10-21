package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;

/**
 * Recibe el estado de una cuenta desde ms-auth (ADR-008). Idempotente por versión: tolera duplicados y desorden. Si es
 * un Prestador, deriva {@code servicios.prestadorActivo} (RF22).
 */
public interface ActualizarCuentaUseCase {

    ResultadoActualizacion actualizar(Cuenta cuenta);
}
