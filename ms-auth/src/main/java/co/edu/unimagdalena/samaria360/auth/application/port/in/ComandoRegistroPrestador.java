package co.edu.unimagdalena.samaria360.auth.application.port.in;

/**
 * Datos del registro de un prestador tal como llegan (RF02, WF-03). Los valida el dominio
 * ({@code RegistroPrestador.validar}), que responde con los mismos nombres de campo.
 */
public record ComandoRegistroPrestador(String nombreRepresentanteLegal, String razonSocial, String rnt,
                                       String correoCorporativo, String telefono, String contrasena,
                                       Boolean aceptoPoliticas) {

    /** Evita que la contraseña aparezca en logs. */
    @Override
    public String toString() {
        return "ComandoRegistroPrestador[correoCorporativo=" + correoCorporativo + ", rnt=" + rnt
                + ", contrasena=****]";
    }
}
