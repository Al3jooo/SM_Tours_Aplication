package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarUsuarioActualUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.UsuarioActual;
import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.util.Objects;
import java.util.UUID;

public class ConsultarUsuarioActualService implements ConsultarUsuarioActualUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ConsultarUsuarioActualService(UsuarioRepositoryPort usuarios) {
        this.usuarios = Objects.requireNonNull(usuarios);
    }

    /**
     * @throws CuentaInactivaException si la cuenta dejó de estar Activa aunque el token siga vigente (RF22, RF62)
     */
    @Override
    public UsuarioActual consultar(UUID idUsuario) {
        Usuario usuario = usuarios.buscarPorId(idUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(idUsuario));
        if (!usuario.getEstadoCuenta().permiteIniciarSesion()) {
            throw new CuentaInactivaException(usuario.getEstadoCuenta());
        }
        String nombreVisible = usuarios.buscarNombreVisible(idUsuario)
                .filter(nombre -> !nombre.isBlank())
                .orElseGet(() -> parteLocal(usuario.getCorreo()));
        return new UsuarioActual(usuario.getId(), usuario.getCorreo(), usuario.getRol(),
                usuario.getEstadoCuenta(), nombreVisible);
    }

    /** Gestor y Administrador no tienen nombre en el modelo: se usa la parte del correo antes de la @. */
    private static String parteLocal(String correo) {
        int arroba = correo.indexOf('@');
        return arroba > 0 ? correo.substring(0, arroba) : correo;
    }
}
