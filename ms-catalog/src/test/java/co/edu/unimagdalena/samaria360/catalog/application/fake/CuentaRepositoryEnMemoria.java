package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** Proyección en memoria con el mismo upsert condicional por versión que la colección {@code cuentas}. */
public class CuentaRepositoryEnMemoria implements CuentaRepositoryPort {

    private final Map<UUID, Cuenta> cuentas = new HashMap<>();
    private RuntimeException fallarAlGuardar;

    public void agregar(Cuenta cuenta) {
        cuentas.put(cuenta.id(), cuenta);
    }

    /** Simula un fallo al guardar la proyección: el siguiente guardado lanza el error. */
    public void fallarAlGuardar(RuntimeException error) {
        this.fallarAlGuardar = error;
    }

    @Override
    public Optional<Cuenta> buscar(UUID id) {
        return Optional.ofNullable(cuentas.get(id));
    }

    @Override
    public OptionalLong versionDe(UUID id) {
        Cuenta cuenta = cuentas.get(id);
        return cuenta == null ? OptionalLong.empty() : OptionalLong.of(cuenta.version());
    }

    @Override
    public boolean guardarSiEsMasNueva(Cuenta cuenta) {
        if (fallarAlGuardar != null) {
            throw fallarAlGuardar;
        }
        Cuenta actual = cuentas.get(cuenta.id());
        if (actual != null && actual.version() >= cuenta.version()) {
            return false;
        }
        cuentas.put(cuenta.id(), cuenta);
        return true;
    }
}
