package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ActualizarCuentaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoActualizacion;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;

import java.util.Objects;

/**
 * Proyección {@code cuenta} (ADR-008): *upsert* idempotente por versión, que tolera duplicados y desorden. Un solo UPDATE
 * condicional, sin transacción propia. Consultar, cancelar y la disponibilidad no la usan: solo la validación del claim
 * {@code ver} de los endpoints autenticados.
 */
public class ActualizarCuentaService implements ActualizarCuentaUseCase {

    private final CuentaRepositoryPort cuentas;
    private final RelojPort reloj;

    public ActualizarCuentaService(CuentaRepositoryPort cuentas, RelojPort reloj) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public ResultadoActualizacion actualizar(Cuenta nueva) {
        boolean aplicada = cuentas.guardarSiEsMasNueva(nueva, reloj.ahora());
        return aplicada ? ResultadoActualizacion.APLICADA : ResultadoActualizacion.IGNORADA;
    }
}
