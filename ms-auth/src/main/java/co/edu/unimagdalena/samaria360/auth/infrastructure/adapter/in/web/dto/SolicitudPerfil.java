package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoPerfil;

import java.util.List;

/**
 * Cuerpo de PUT /api/v1/auth/perfil (RF53). Sin {@code correo}: si llega, Jackson lo ignora como cualquier campo
 * desconocido (DEC-84). Sin anotaciones de validación: el dominio valida y responde con estos nombres en {@code campos}.
 */
public record SolicitudPerfil(String primerNombre, String segundoNombre, String primerApellido, String segundoApellido,
                              String paisOrigen, List<String> categoriasInteres) {

    public ComandoPerfil aComando() {
        return new ComandoPerfil(primerNombre, segundoNombre, primerApellido, segundoApellido, paisOrigen,
                categoriasInteres);
    }
}
