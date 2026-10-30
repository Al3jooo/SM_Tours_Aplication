package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * El servicio cambió después de que el Prestador lo leyó: otra edición suya o la decisión del Administrador subieron la
 * versión (DEC-186). Es el 409 SERVICIO_MODIFICADO con la {@code versionActual}.
 */
public class ServicioModificadoException extends RuntimeException {

    private final int versionActual;

    public ServicioModificadoException(int versionActual) {
        super("El servicio cambió: su versión actual es " + versionActual);
        this.versionActual = versionActual;
    }

    public int getVersionActual() {
        return versionActual;
    }
}
