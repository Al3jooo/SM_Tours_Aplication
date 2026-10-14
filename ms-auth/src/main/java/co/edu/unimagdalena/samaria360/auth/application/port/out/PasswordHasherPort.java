package co.edu.unimagdalena.samaria360.auth.application.port.out;

/** Contraseñas con un algoritmo unidireccional (RNF05: BCrypt). */
public interface PasswordHasherPort {

    boolean coincide(String plana, String hash);

    /** Hash de una contraseña nueva (registro, RF01 y RF02). */
    String codificar(String plana);
}
