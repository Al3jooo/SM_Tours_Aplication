package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;

import java.util.Optional;
import java.util.UUID;

/** Tablas {@code turista} y {@code turista_categoria_interes} de auth_db (RF53). */
public interface PerfilRepositoryPort {

    /** Vacío si el id no es de un turista. */
    Optional<PerfilActual> buscar(UUID idTurista);

    /**
     * Actualiza los datos y reemplaza el conjunto de categorías dentro de la transacción del caso de uso.
     *
     * @return false si el id no es de un turista (no se cambió nada)
     */
    boolean actualizar(UUID idTurista, PerfilTurista perfil);
}
