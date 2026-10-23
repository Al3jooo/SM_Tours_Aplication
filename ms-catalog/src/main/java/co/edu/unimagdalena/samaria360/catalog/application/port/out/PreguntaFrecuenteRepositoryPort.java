package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

import java.util.List;

/** Colección {@code faq} de {@code catalog_db} (RF26). */
public interface PreguntaFrecuenteRepositoryPort {

    /** Un ObjectId nuevo, en su forma textual, para una pregunta que todavía no se guardó. */
    String nuevoId();

    /** Por título, sin distinguir mayúsculas ni tildes (colación {@code es}, nivel 1), y por id para desempatar. */
    List<PreguntaFrecuente> listar();

    long contar();

    void insertar(PreguntaFrecuente pregunta);

    /** @return false si no existe una pregunta con ese id */
    boolean reemplazar(PreguntaFrecuente pregunta);

    /** @return false si no existe una pregunta con ese id */
    boolean eliminar(String id);
}
