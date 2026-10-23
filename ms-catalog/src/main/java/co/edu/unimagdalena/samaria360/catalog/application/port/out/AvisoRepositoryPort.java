package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.List;
import java.util.Optional;

/** Colección {@code avisos} de {@code catalog_db} (RF40). Los listados van por fecha de publicación descendente e id. */
public interface AvisoRepositoryPort {

    /** Un ObjectId nuevo, en su forma textual, para un aviso que todavía no se guardó. */
    String nuevoId();

    void insertar(Aviso aviso);

    List<Aviso> activos();

    PaginaResultados<Aviso> pagina(int pagina, int tamano);

    Optional<Aviso> buscar(String id);

    /**
     * UPDATE condicional: pasa a inactivo solo si está activo.
     *
     * @return false si no hay un aviso activo con ese id (no existe o ya estaba inactivo)
     */
    boolean despublicar(String id);
}
