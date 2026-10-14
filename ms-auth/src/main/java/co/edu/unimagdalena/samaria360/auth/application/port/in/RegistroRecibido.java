package co.edu.unimagdalena.samaria360.auth.application.port.in;

/**
 * Resultado del registro: siempre el mismo, se haya creado la cuenta o el correo ya existiera (ADR-006). No lleva el id
 * ni ningún dato que distinga los dos casos.
 */
public record RegistroRecibido() {

    public static final RegistroRecibido INSTANCIA = new RegistroRecibido();
}
