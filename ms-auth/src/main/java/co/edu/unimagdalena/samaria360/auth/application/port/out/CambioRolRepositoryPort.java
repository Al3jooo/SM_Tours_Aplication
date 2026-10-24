package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroPersonal;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoElegibleException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaConRol;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia del cambio de rol (RF45). Todo corre dentro de la transacción que abre {@code CambiarRolTransaccional}. */
public interface CambioRolRepositoryPort {

    /** Cualquier rol; vacío si el id no existe. */
    Optional<CuentaConRol> buscarCuenta(UUID idUsuario);

    /**
     * Bloquea las filas de los Administradores activos, en orden de id, hasta el fin de la transacción (DEC-106) y
     * devuelve sus ids, ya con los cambios que otra transacción confirmó mientras se esperaba el bloqueo.
     */
    List<UUID> bloquearAdministradoresActivos();

    /**
     * UPDATE condicional (rol y versión esperados, cuenta Activa) que sube {@code version_cuenta} en 1, y cambio de las
     * filas de {@code gestor} y {@code administrador}. La fila de {@code turista} no se toca.
     *
     * @throws RolCambiadoException       si otro cambio llegó antes
     * @throws CuentaNoElegibleException si la cuenta dejó de estar Activa
     */
    void aplicar(CambioRol cambio);

    Optional<PersonalResumen> resumen(UUID idUsuario);

    PaginaCuentas<PersonalResumen> listarPersonal(FiltroPersonal filtro);
}
