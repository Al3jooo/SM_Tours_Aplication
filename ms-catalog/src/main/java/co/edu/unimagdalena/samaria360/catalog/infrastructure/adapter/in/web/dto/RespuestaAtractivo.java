package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;

import java.util.List;

/**
 * Ficha pública de un atractivo (WF-06 para atractivos; DEC-217): todo lo que ve el Turista, sin el Gestor ni la
 * versión. Las condiciones de accesibilidad van en el orden de RF27.
 *
 * @param limiteCapacidadCarga RF07
 * @param estadoOperativo      RF33; null solo en un atractivo anterior a la iteración E sin estado
 * @param normasAmbientales    RF24; null si no tiene
 * @param horarioAtencion      RF58; null si no tiene
 */
public record RespuestaAtractivo(String id, String nombre, String categoria, String descripcion,
                                 Coordenadas coordenadas, List<String> imagenes, int limiteCapacidadCarga,
                                 String estadoOperativo, String normasAmbientales, String horarioAtencion,
                                 List<String> condicionesAccesibilidad) {

    public record Coordenadas(double longitud, double latitud) {
    }

    public static RespuestaAtractivo de(AtractivoPublico atractivo) {
        return new RespuestaAtractivo(atractivo.getId().orElseThrow(), atractivo.getNombre(),
                atractivo.getCategoria().orElse(null), atractivo.getDescripcion().orElse(null),
                coordenadas(atractivo), atractivo.getImagenes(), atractivo.getLimiteCapacidadCarga(),
                atractivo.getEstadoOperativo().map(Enum::name).orElse(null),
                atractivo.getNormasAmbientales().orElse(null), atractivo.getHorarioAtencion().orElse(null),
                accesibilidad(atractivo));
    }

    static Coordenadas coordenadas(AtractivoPublico atractivo) {
        return new Coordenadas(atractivo.getCoordenada().longitud(), atractivo.getCoordenada().latitud());
    }

    static List<String> accesibilidad(AtractivoPublico atractivo) {
        return RespuestaServicio.ORDEN_ACCESIBILIDAD.stream()
                .filter(atractivo.getCondicionesAccesibilidad()::contains)
                .map(Enum::name)
                .toList();
    }
}
