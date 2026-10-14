package co.edu.unimagdalena.samaria360.auth.domain.exception;

/**
 * El Administrador rechazó la validación del prestador (RF28; decisión D3). El modelo no guarda el motivo del rechazo,
 * así que el mensaje es genérico; guardar la justificación es parte del CU de RF28.
 */
public class CuentaRechazadaException extends InicioSesionException {

    public CuentaRechazadaException() {
        super("La solicitud de la cuenta fue rechazada por el Administrador");
    }
}
