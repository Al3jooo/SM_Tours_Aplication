package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;

import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** Colección {@code cuentas} de {@code catalog_db}: la proyección del estado de las cuentas de ms-auth. */
public interface CuentaRepositoryPort {

    Optional<Cuenta> buscar(UUID id);

    OptionalLong versionDe(UUID id);

    /**
     * Upsert condicional: guarda solo si no hay cuenta o la guardada tiene una versión menor.
     *
     * @return false si ya había una versión igual o mayor (otro envío llegó antes)
     */
    boolean guardarSiEsMasNueva(Cuenta cuenta);
}
