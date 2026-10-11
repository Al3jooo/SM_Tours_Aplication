package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.util.UUID;

/** Datos básicos del usuario autenticado (GET /me). Nunca incluye el hash de la contraseña. */
public record UsuarioActual(UUID idUsuario, String correo, Rol rol, EstadoCuenta estadoCuenta) {
}
