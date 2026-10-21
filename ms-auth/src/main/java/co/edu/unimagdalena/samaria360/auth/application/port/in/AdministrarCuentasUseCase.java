package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.UUID;

/** Cambios de estado de las cuentas (RF28, RF22, RF62). Cada uno es una transacción con su evento. */
public interface AdministrarCuentasUseCase {

    PrestadorDetalle validar(ComandoValidacion comando);

    PrestadorDetalle inactivar(UUID idPrestador);

    TuristaResumen desactivar(UUID idTurista);

    TuristaResumen reactivar(UUID idTurista);
}
