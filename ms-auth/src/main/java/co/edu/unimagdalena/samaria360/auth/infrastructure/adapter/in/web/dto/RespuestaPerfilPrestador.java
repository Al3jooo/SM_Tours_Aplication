package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilPrestador;

/**
 * Perfil comercial del Prestador (GET y PUT /api/v1/auth/prestador/perfil; RF43, DEC-188). {@code correo},
 * {@code representanteLegal}, {@code razonSocial} y {@code rnt} van en solo lectura; el RNT cierra O16.
 */
public record RespuestaPerfilPrestador(String correo, String representanteLegal, String razonSocial, String rnt,
                                       String correoCorporativo, String telefono) {

    public static RespuestaPerfilPrestador de(PerfilPrestador perfil) {
        return new RespuestaPerfilPrestador(perfil.correo(), perfil.representanteLegal(), perfil.razonSocial(),
                perfil.rnt(), perfil.correoCorporativo(), perfil.telefono());
    }
}
