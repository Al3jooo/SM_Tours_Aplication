package co.edu.unimagdalena.samaria360.auth.application.port.out;

/** Verificación de contraseñas con un algoritmo unidireccional (RNF05: BCrypt). */
public interface PasswordHasherPort {

    boolean coincide(String plana, String hash);
}
