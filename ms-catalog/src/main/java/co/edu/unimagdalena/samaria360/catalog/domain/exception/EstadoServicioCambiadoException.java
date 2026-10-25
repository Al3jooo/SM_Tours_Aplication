package co.edu.unimagdalena.samaria360.catalog.domain.exception;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;

/**
 * El servicio ya no estaba Pendiente: otro Administrador lo aprobó o lo rechazó antes (DEC-128, como DEC-39). Es el 409
 * ESTADO_CAMBIADO con el {@code estadoActual}.
 */
public class EstadoServicioCambiadoException extends RuntimeException {

    private final EstadoPublicacion estadoActual;

    public EstadoServicioCambiadoException(EstadoPublicacion estadoActual) {
        super("El servicio ya no está Pendiente: está " + estadoActual);
        this.estadoActual = estadoActual;
    }

    public EstadoPublicacion getEstadoActual() {
        return estadoActual;
    }
}
