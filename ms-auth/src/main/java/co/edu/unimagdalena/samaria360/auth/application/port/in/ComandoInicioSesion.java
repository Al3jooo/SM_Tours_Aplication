package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.Objects;

/** Datos de entrada del caso de uso Iniciar sesión (RF15). */
public record ComandoInicioSesion(String correo, String contrasena) {

    public ComandoInicioSesion {
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(contrasena, "contrasena");
    }

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "ComandoInicioSesion[correo=" + correo + ", contrasena=****]";
    }
}
