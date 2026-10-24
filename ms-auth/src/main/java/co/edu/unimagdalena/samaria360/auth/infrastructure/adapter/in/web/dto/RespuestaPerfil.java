package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;

import java.util.List;

/** Perfil del turista (GET y PUT /api/v1/auth/perfil): el correo va en solo lectura (DEC-84). */
public record RespuestaPerfil(String correo, String primerNombre, String segundoNombre, String primerApellido,
                              String segundoApellido, String paisOrigen, List<String> categoriasInteres) {

    public static RespuestaPerfil de(PerfilActual actual) {
        PerfilTurista perfil = actual.perfil();
        return new RespuestaPerfil(actual.correo(), perfil.primerNombre(), perfil.segundoNombre(),
                perfil.primerApellido(), perfil.segundoApellido(), perfil.paisOrigen().valor(),
                perfil.categoriasOrdenadas().stream().map(Enum::name).toList());
    }
}
