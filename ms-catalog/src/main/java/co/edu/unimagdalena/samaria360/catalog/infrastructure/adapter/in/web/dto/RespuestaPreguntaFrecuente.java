package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

import java.time.Instant;

/** Una pregunta frecuente (RF26) en el listado público y en las respuestas del Administrador. */
public record RespuestaPreguntaFrecuente(String id, String titulo, String cuerpo, Instant fechaActualizacion) {

    public static RespuestaPreguntaFrecuente de(PreguntaFrecuente pregunta) {
        return new RespuestaPreguntaFrecuente(pregunta.id(), pregunta.titulo(), pregunta.cuerpo(),
                pregunta.fechaActualizacion());
    }
}
