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
 * <p>
 * (4) Si la proyección se guardó y es de un Prestador, se vuelve a fijar {@code prestadorActivo} (DEC-142 b): un servicio
 * que el prestador registró entre el paso 2 y el 3 leyó la cuenta todavía Activa y no lo alcanzó el primer
 * {@code updateMulti}. Con la otra mitad, en {@code RegistrarServicioService}, ningún entrelazado deja visible un
 * servicio de un prestador inactivo.
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
        boolean prestador = nueva.rol() == RolCuenta.Prestador || eraPrestador;
        if (prestador) {
            visibilidad.fijarPrestadorActivo(nueva.id().toString(), nueva.prestadorActivo());
        }
        if (!cuentas.guardarSiEsMasNueva(nueva)) {
            return ResultadoActualizacion.IGNORADA;
        }
        if (prestador) {
            visibilidad.fijarPrestadorActivo(nueva.id().toString(), nueva.prestadorActivo());
        }
        return ResultadoActualizacion.APLICADA;
    }
}
