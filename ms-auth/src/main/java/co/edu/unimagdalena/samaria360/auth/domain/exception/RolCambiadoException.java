package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

/**
 * La cuenta ya no tiene el rol desde el que se pidió el cambio, o cambió mientras tanto (409 ROL_CAMBIADO, DEC-104).
 * Lleva el rol actual para que la pantalla recargue la lista.
 */
public class RolCambiadoException extends RuntimeException {

    private final Rol rolActual;

    public RolCambiadoException(Rol rolActual) {
        super("La cuenta cambió de rol: ahora es " + rolActual);
        this.rolActual = rolActual;
    }

    public Rol getRolActual() {
        return rolActual;
    }
}
