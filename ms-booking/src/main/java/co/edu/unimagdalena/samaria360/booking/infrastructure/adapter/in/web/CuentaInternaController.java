package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ActualizarCuentaUseCase;
import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.RolCuenta;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto.SolicitudCuenta;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoint interno (ADR-008): ms-auth entrega aquí el estado de cada cuenta. Solo lo alcanza un JWT de servicio con
 * {@code aud = ms-booking}; el API Gateway no enruta {@code /internal/**}. Responde 204 se aplique o se ignore la
 * versión (idempotente), para que el relevo no reintente un duplicado.
 */
@RestController
public class CuentaInternaController {

    private final ActualizarCuentaUseCase actualizar;

    public CuentaInternaController(ActualizarCuentaUseCase actualizar) {
        this.actualizar = actualizar;
    }

    @PutMapping("/internal/v1/cuentas/{id}")
    public ResponseEntity<Void> actualizar(@PathVariable UUID id, @RequestBody SolicitudCuenta solicitud) {
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        RolCuenta rol = leer(RolCuenta.class, solicitud.rol(), "rol", campos);
        EstadoCuenta estado = leer(EstadoCuenta.class, solicitud.estadoCuenta(), "estadoCuenta", campos);
        if (solicitud.version() == null) {
            campos.put("version", MotivoInvalido.REQUERIDO);
        } else if (solicitud.version() < 1) {
            campos.put("version", MotivoInvalido.VALOR_MINIMO);
        }
        if (!campos.isEmpty()) {
            throw new DatosReservaInvalidosException(campos);
        }
        actualizar.actualizar(new Cuenta(id, rol, estado, solicitud.version()));
        return ResponseEntity.noContent().build();
    }

    private static <E extends Enum<E>> E leer(Class<E> tipo, String valor, String campo,
                                              Map<String, MotivoInvalido> campos) {
        if (valor == null || valor.isBlank()) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            campos.put(campo, MotivoInvalido.NO_PERMITIDO);
            return null;
        }
    }
}
