package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CorreoYaRegistradoException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RntYaRegistradoException;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del registro. Cada {@code registrar…} inserta el usuario y su subtipo; la transacción la abre la
 * infraestructura, así que si algo falla no queda un usuario a medias.
 */
public interface RegistroRepositoryPort {

    /** @param correo ya normalizado */
    Optional<UUID> buscarIdPorCorreo(String correo);

    boolean existeRnt(String rnt);

    /** @throws CorreoYaRegistradoException si otro registro con el mismo correo terminó antes (carrera) */
    void registrarTurista(TuristaNuevo turista);

    /**
     * @throws CorreoYaRegistradoException si otro registro con el mismo correo terminó antes (carrera)
     * @throws RntYaRegistradoException    si otro registro con el mismo RNT terminó antes (carrera)
     */
    void registrarPrestador(PrestadorNuevo prestador);
}
