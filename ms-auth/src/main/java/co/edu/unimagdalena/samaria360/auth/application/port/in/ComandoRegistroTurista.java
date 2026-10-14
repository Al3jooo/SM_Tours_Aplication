package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.List;

/**
 * Datos del registro de un turista tal como llegan (RF01, WF-02). Los valida el dominio
 * ({@code RegistroTurista.validar}), que responde con los mismos nombres de campo.
 */
public record ComandoRegistroTurista(String primerNombre, String segundoNombre, String primerApellido,
                                     String segundoApellido, String correo, String contrasena, String paisOrigen,
                                     List<String> categoriasInteres, Boolean aceptoPoliticas) {

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "ComandoRegistroTurista[correo=" + correo + ", contrasena=****]";
    }
}
