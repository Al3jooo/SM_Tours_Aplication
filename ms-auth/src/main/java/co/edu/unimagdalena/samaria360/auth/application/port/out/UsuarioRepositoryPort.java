package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    /** @param correo ya normalizado (sin espacios y en minúsculas) */
    Optional<Usuario> buscarPorCorreo(String correo);

    Optional<Usuario> buscarPorId(UUID id);

    /** Persiste {@code intentos_fallidos} y {@code bloqueado_hasta}. */
    void guardar(Usuario usuario);
}
