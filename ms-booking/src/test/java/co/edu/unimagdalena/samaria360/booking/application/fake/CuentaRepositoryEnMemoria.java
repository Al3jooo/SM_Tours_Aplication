package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** Proyección en memoria con el mismo upsert condicional por versión que la tabla {@code cuenta}. */
public class CuentaRepositoryEnMemoria implements CuentaRepositoryPort {

    private final Map<UUID, Cuenta> cuentas = new HashMap<>();
    private final Map<UUID, Instant> actualizadas = new HashMap<>();

    public void agregar(Cuenta cuenta) {
        cuentas.put(cuenta.id(), cuenta);
    }

    public Instant actualizadoEn(UUID id) {
        return actualizadas.get(id);
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
    public boolean guardarSiEsMasNueva(Cuenta cuenta, Instant ahora) {
        Cuenta actual = cuentas.get(cuenta.id());
        if (actual != null && actual.version() >= cuenta.version()) {
            return false;
        }
        cuentas.put(cuenta.id(), cuenta);
        actualizadas.put(cuenta.id(), ahora);
        return true;
    }
}
