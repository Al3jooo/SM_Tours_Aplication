package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.List;

/**
 * Perfil del turista tal como llega (RF53). Sin correo ni contraseña (DEC-79). Lo valida el dominio
 * ({@code PerfilTurista.validar}), que responde con los mismos nombres de campo.
 */
public record ComandoPerfil(String primerNombre, String segundoNombre, String primerApellido, String segundoApellido,
                            String paisOrigen, List<String> categoriasInteres) {
}
