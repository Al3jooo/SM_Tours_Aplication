package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;

import java.util.List;

/**
 * Datos de un atractivo (RF06, RF07, RF24, RF27, RF33, RF58; contrato en {@code docs/sad/api/ms-catalog.md}). En el
 * registro es la parte {@code datos} del multipart (DEC-216) y {@code version} se ignora; en la edición es el cuerpo
 * JSON, {@code version} es obligatoria y {@code estadoOperativo} se ignora (DEC-210). Lo que fija el servidor no está
 * aquí y, si llega, se ignora: el Gestor, las fechas y las imágenes.
 */
public record SolicitudAtractivo(String nombre, String categoria, String descripcion, Coordenadas coordenadas,
                                 Integer limiteCapacidadCarga, String estadoOperativo, String normasAmbientales,
                                 String horarioAtencion, List<String> condicionesAccesibilidad, Integer version) {

    public record Coordenadas(Double longitud, Double latitud) {
    }

    public BorradorAtractivo aBorrador() {
        return new BorradorAtractivo(nombre, categoria, descripcion,
                coordenadas == null ? null
                        : new BorradorAtractivo.DatosCoordenadas(coordenadas.longitud(), coordenadas.latitud()),
                limiteCapacidadCarga, estadoOperativo, normasAmbientales, horarioAtencion, condicionesAccesibilidad);
    }
}
