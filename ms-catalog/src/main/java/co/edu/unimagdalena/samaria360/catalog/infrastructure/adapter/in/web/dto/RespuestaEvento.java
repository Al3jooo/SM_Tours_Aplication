package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;

import java.time.LocalDate;

/** Un evento de la agenda pública (RF25): sin {@code gestorId} ni {@code fechaRegistro}, como la ficha (DEC-217). */
public record RespuestaEvento(String id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, String ubicacion,
                              String descripcion) {

    public static RespuestaEvento de(EventoTuristico evento) {
        return new RespuestaEvento(evento.id(), evento.nombre(), evento.fechaInicio(), evento.fechaFin(),
                evento.ubicacion(), evento.descripcion());
    }
}
