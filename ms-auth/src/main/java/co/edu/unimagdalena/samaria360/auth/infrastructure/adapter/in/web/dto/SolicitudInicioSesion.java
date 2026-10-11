package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SolicitudInicioSesion(
        @NotBlank @Email String correo,
        @NotBlank String contrasena) {

    /** Recorta los espacios del correo antes de validar; el caso de uso normaliza el resto. */
    public SolicitudInicioSesion {
        correo = correo == null ? null : correo.trim();
    }

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "SolicitudInicioSesion[correo=" + correo + ", contrasena=****]";
    }
}
