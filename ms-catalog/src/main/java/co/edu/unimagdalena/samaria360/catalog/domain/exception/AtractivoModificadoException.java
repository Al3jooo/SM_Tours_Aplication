package co.edu.unimagdalena.samaria360.catalog.domain.exception;

/**
 * El atractivo cambió después de que el Gestor lo leyó: otra edición o un cambio de estado subieron la versión
 * (DEC-207, como DEC-186). Es el 409 ATRACTIVO_MODIFICADO con la {@code versionActual}.
 */
public class AtractivoModificadoException extends RuntimeException {

    private final int versionActual;

    public AtractivoModificadoException(int versionActual) {
        super("El atractivo cambió: su versión actual es " + versionActual);
        this.versionActual = versionActual;
    }

    public int getVersionActual() {
        return versionActual;
    }
}
