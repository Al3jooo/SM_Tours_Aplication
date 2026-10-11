package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda copias, como una base de datos real: si el caso de uso modifica el usuario
 * y no llama a {@link #guardar}, el cambio no queda persistido.
 */
public class UsuarioRepositoryEnMemoria implements UsuarioRepositoryPort {

    private final Map<UUID, Usuario> usuarios = new HashMap<>();
    private int llamadasGuardar;

    /** Precarga un usuario sin contar como llamada a {@link #guardar}. */
    public void agregar(Usuario usuario) {
        usuarios.put(usuario.getId(), copiar(usuario));
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarios.values().stream()
                .filter(u -> u.getCorreo().equals(correo))
                .findFirst()
                .map(UsuarioRepositoryEnMemoria::copiar);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return Optional.ofNullable(usuarios.get(id)).map(UsuarioRepositoryEnMemoria::copiar);
    }

    @Override
    public void guardar(Usuario usuario) {
        llamadasGuardar++;
        usuarios.put(usuario.getId(), copiar(usuario));
    }

    /** Estado persistido del usuario. */
    public Usuario persistido(UUID id) {
        return buscarPorId(id).orElseThrow();
    }

    public int llamadasGuardar() {
        return llamadasGuardar;
    }

    private static Usuario copiar(Usuario u) {
        return new Usuario(u.getId(), u.getCorreo(), u.getContrasenaHash(), u.getRol(), u.getEstadoCuenta(),
                u.getIntentosFallidos(), u.getBloqueadoHasta().orElse(null));
    }
}
