package co.edu.unimagdalena.samaria360.booking.application.port.out;

import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** Tabla {@code cuenta} de {@code reservas_db}: la proyección del estado de las cuentas de ms-auth (V4). */
public interface CuentaRepositoryPort {

    Optional<Cuenta> buscar(UUID id);

    OptionalLong versionDe(UUID id);

    /**
     * Upsert condicional: guarda solo si no hay cuenta o la guardada tiene una versión menor.
     *
     * @return false si ya había una versión igual o mayor
     */
    boolean guardarSiEsMasNueva(Cuenta cuenta, Instant ahora);
}
