package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoRegistroPrestador;

/**
 * Cuerpo de POST /api/v1/auth/registro/prestador (WF-03). Sin anotaciones de validación: el dominio valida y responde con
 * estos mismos nombres de campo en {@code campos}.
 */
public record SolicitudRegistroPrestador(String nombreRepresentanteLegal, String razonSocial, String rnt,
                                         String correoCorporativo, String telefono, String contrasena,
                                         Boolean aceptoPoliticas) {

    public ComandoRegistroPrestador aComando() {
        return new ComandoRegistroPrestador(nombreRepresentanteLegal, razonSocial, rnt, correoCorporativo, telefono,
                contrasena, aceptoPoliticas);
    }

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "SolicitudRegistroPrestador[correoCorporativo=" + correoCorporativo + ", rnt=" + rnt
                + ", contrasena=****]";
    }
}
