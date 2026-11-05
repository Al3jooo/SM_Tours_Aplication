package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Un evento en las respuestas del Gestor (RF25): con quién lo registró y cuándo. */
public record RespuestaEventoGestion(String id, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                     String ubicacion, String descripcion, UUID gestorId, Instant fechaRegistro) {

    public static RespuestaEventoGestion de(EventoTuristico evento) {
        return new RespuestaEventoGestion(evento.id(), evento.nombre(), evento.fechaInicio(), evento.fechaFin(),
                evento.ubicacion(), evento.descripcion(), evento.gestorId(), evento.fechaRegistro());
    }
}
