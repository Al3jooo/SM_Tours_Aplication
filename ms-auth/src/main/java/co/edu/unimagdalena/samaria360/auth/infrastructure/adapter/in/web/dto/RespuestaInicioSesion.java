package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.SesionIniciada;

import java.time.Instant;
import java.util.UUID;

public record RespuestaInicioSesion(String token, String tipo, Instant expiraEn, UUID idUsuario, String rol) {

    public static RespuestaInicioSesion de(SesionIniciada sesion) {
        return new RespuestaInicioSesion(sesion.token(), "Bearer", sesion.expiraEn(), sesion.idUsuario(),
                sesion.rol().name());
    }
}
