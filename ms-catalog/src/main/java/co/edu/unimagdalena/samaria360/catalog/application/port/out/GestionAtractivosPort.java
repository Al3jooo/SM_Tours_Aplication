package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.Optional;

/**
 * Colección {@code atractivos} para el Gestor (DEC-206): el registro (RF06), el panel «Atractivos» (WF-24), la edición
 * (RF07, RF24, RF27, RF58) y el cambio de estado (RF33). Los ids son ObjectId en texto, ya validados.
 */
public interface GestionAtractivosPort {

    /** Un ObjectId nuevo, en su forma textual, para un atractivo que todavía no se guardó. */
    String nuevoId();

    void insertar(AtractivoPublico atractivo);

    Optional<AtractivoPublico> buscar(String id);

    /** Todos los atractivos, del más reciente al más antiguo (desempate por id). */
    PaginaResultados<AtractivoPublico> listar(int pagina, int tamano);

    /**
     * Escribe los datos editables y {@code fechaActualizacion} y sube la versión: UPDATE condicional sobre el id y la
     * versión que el Gestor leyó ({@code atractivo.getVersion()}, DEC-207).
     *
     * @return false si no hay un atractivo con ese id y esa versión
     */
    boolean actualizar(AtractivoPublico atractivo);

    /** Como {@link #actualizar}, con el estado operativo (RF33). */
    boolean cambiarEstado(AtractivoPublico atractivo);
}
