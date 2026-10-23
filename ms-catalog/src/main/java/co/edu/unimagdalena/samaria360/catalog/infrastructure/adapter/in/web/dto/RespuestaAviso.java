package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;

import java.time.Instant;

/** Un aviso general (RF40) en el banner público y en las respuestas del Administrador. */
public record RespuestaAviso(String id, String mensaje, String tipo, boolean activo, Instant fechaPublicacion) {

    public static RespuestaAviso de(Aviso aviso) {
        return new RespuestaAviso(aviso.id(), aviso.mensaje(), aviso.tipo().name(), aviso.activo(),
                aviso.fechaPublicacion());
    }
}
