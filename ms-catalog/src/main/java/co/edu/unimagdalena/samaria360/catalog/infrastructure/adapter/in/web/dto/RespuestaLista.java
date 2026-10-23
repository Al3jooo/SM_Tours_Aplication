package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import java.util.List;

/** Un listado completo, sin paginar: {@code {elementos}} (preguntas frecuentes y avisos activos). */
public record RespuestaLista<T>(List<T> elementos) {
}
