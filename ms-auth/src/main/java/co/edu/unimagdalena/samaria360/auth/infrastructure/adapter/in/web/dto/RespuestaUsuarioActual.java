package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.UsuarioActual;

import java.util.UUID;

public record RespuestaUsuarioActual(UUID idUsuario, String correo, String rol, String estadoCuenta,
                                     String nombreVisible) {

    public static RespuestaUsuarioActual de(UsuarioActual usuario) {
        return new RespuestaUsuarioActual(usuario.idUsuario(), usuario.correo(), usuario.rol().name(),
                usuario.estadoCuenta().name(), usuario.nombreVisible());
    }
}
