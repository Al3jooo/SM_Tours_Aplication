package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;

/**
 * La cuenta no está en el estado desde el que se pidió el cambio: otro Administrador ya la cambió o la acción no es una
 * transición válida (409 ESTADO_CAMBIADO, DEC-39). Lleva el estado actual para que la pantalla recargue la lista.
 */
public class EstadoCambiadoException extends RuntimeException {

    private final EstadoAdministrativo estadoActual;

    public EstadoCambiadoException(EstadoAdministrativo estadoActual) {
        super("La cuenta cambió de estado: ahora está " + estadoActual);
        this.estadoActual = estadoActual;
    }

    public EstadoAdministrativo getEstadoActual() {
        return estadoActual;
    }
}
