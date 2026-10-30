package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilPrestador;
import co.edu.unimagdalena.samaria360.auth.domain.model.DatosComercialesPrestador;

import java.util.Optional;
import java.util.UUID;

/** Tablas {@code prestador} y {@code usuario} de auth_db: el perfil comercial (RF43). */
public interface PerfilPrestadorRepositoryPort {

    /** Vacío si el id no es de un prestador. */
    Optional<PerfilPrestador> buscar(UUID idPrestador);

    /** @return false si el id no es de un prestador (no se cambió nada) */
    boolean actualizar(UUID idPrestador, DatosComercialesPrestador datos);
}
