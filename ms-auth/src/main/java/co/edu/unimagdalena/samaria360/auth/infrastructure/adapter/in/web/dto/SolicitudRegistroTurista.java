package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoRegistroTurista;

import java.util.List;

/**
 * Cuerpo de POST /api/v1/auth/registro/turista (WF-02). Sin anotaciones de validación: el dominio valida y responde con
 * estos mismos nombres de campo en {@code campos}.
 */
public record SolicitudRegistroTurista(String primerNombre, String segundoNombre, String primerApellido,
                                       String segundoApellido, String correo, String contrasena, String paisOrigen,
                                       List<String> categoriasInteres, Boolean aceptoPoliticas) {

    public ComandoRegistroTurista aComando() {
        return new ComandoRegistroTurista(primerNombre, segundoNombre, primerApellido, segundoApellido, correo,
                contrasena, paisOrigen, categoriasInteres, aceptoPoliticas);
    }

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "SolicitudRegistroTurista[correo=" + correo + ", contrasena=****]";
    }
}
