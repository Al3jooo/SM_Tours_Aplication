package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

import java.util.List;

/**
 * CU «Gestionar FAQs» (RF26, RF47). Consultar es público; crear, editar y eliminar son del Administrador (la
 * autorización la aplica la infraestructura). Los datos se validan primero (400) y después se busca la pregunta (404).
 */
public interface GestionarPreguntasFrecuentesUseCase {

    /** Todas, por título en orden alfabético (DEC-63): son {@value PreguntaFrecuente#MAXIMO_PREGUNTAS} como mucho. */
    List<PreguntaFrecuente> listar();

    PreguntaFrecuente crear(String titulo, String cuerpo);

    /** Reemplaza título y cuerpo y renueva {@code fechaActualizacion}. Gana la última escritura. */
    PreguntaFrecuente editar(String id, String titulo, String cuerpo);

    /** Borrado físico (RF26). */
    void eliminar(String id);
}
