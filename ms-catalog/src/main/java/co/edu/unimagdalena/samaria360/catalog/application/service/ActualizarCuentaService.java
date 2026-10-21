package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ActualizarCuentaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ResultadoActualizacion;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.VisibilidadPrestadorPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RolCuenta;

import java.util.Objects;
import java.util.Optional;

/**
 * Proyección {@code cuentas} (ADR-008). Orden de los efectos: (1) si la versión recibida no es mayor que la guardada, se
 * ignora; (2) si la cuenta es o era de un Prestador, se fija {@code servicios.prestadorActivo}; (3) se guarda la
 * proyección. Si algo falla antes del paso 3, la proyección no avanza y el relevo de ms-auth reintenta: cada paso es
 * idempotente.
 * <p>
 * Supuesto: la secuencia no es atómica, así que solo es correcta si cada cuenta se entrega de a una por vez. Lo
 * garantiza el relevo de ms-auth: un hilo por destino, la última versión pendiente por usuario y el arriendo con
 * {@code FOR UPDATE SKIP LOCKED} (ADR-008).
 */
public class ActualizarCuentaService implements ActualizarCuentaUseCase {

    private final CuentaRepositoryPort cuentas;
    private final VisibilidadPrestadorPort visibilidad;

    public ActualizarCuentaService(CuentaRepositoryPort cuentas, VisibilidadPrestadorPort visibilidad) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.visibilidad = Objects.requireNonNull(visibilidad);
    }

    @Override
    public ResultadoActualizacion actualizar(Cuenta nueva) {
        Optional<Cuenta> guardada = cuentas.buscar(nueva.id());
        if (guardada.isPresent() && guardada.get().version() >= nueva.version()) {
            return ResultadoActualizacion.IGNORADA;
        }
        boolean eraPrestador = guardada.map(c -> c.rol() == RolCuenta.Prestador).orElse(false);
        if (nueva.rol() == RolCuenta.Prestador || eraPrestador) {
            visibilidad.fijarPrestadorActivo(nueva.id().toString(), nueva.prestadorActivo());
        }
        return cuentas.guardarSiEsMasNueva(nueva) ? ResultadoActualizacion.APLICADA : ResultadoActualizacion.IGNORADA;
    }
}
