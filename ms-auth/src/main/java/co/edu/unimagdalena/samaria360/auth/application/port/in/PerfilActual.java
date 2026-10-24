package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;

/** Perfil del turista con su correo, que se muestra en solo lectura (DEC-84). */
public record PerfilActual(String correo, PerfilTurista perfil) {
}
