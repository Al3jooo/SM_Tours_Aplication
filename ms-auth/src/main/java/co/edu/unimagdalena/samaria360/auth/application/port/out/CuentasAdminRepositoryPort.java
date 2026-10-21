package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import co.edu.unimagdalena.samaria360.auth.domain.exception.EstadoCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioEstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaAdministrada;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CuentasAdminRepositoryPort {

    /** Turista o prestador; vacío para los demás roles y para un id inexistente. */
    Optional<CuentaAdministrada> buscarCuenta(UUID idUsuario);

    /**
     * UPDATE condicional (DEC-39): solo si la versión y el estado siguen siendo los esperados; sube
     * {@code version_cuenta} en 1. La validación del prestador guarda además la revisión.
     *
     * @throws EstadoCambiadoException si otro cambio llegó antes
     */
    void aplicar(CambioEstadoCuenta cambio, UUID revisadoPor, Instant ahora);

    PaginaCuentas<PrestadorResumen> listarPrestadores(FiltroCuentas filtro);

    PaginaCuentas<TuristaResumen> listarTuristas(FiltroCuentas filtro);

    Optional<PrestadorDetalle> detallePrestador(UUID idPrestador);

    Optional<TuristaResumen> detalleTurista(UUID idTurista);
}
