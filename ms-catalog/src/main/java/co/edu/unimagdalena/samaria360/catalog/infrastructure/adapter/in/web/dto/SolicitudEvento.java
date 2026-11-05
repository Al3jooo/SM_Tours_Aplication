package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorEvento;

/**
 * Cuerpo de {@code POST /gestor/eventos} (RF25). Las fechas van como texto {@code yyyy-MM-dd} (DEC-236) para que el
 * dominio responda {@code FORMATO} por campo. Lo que fija el servidor ({@code gestorId}, {@code fechaRegistro}) se ignora.
 */
public record SolicitudEvento(String nombre, String fechaInicio, String fechaFin, String ubicacion,
                              String descripcion) {

    public BorradorEvento aBorrador() {
        return new BorradorEvento(nombre, fechaInicio, fechaFin, ubicacion, descripcion);
    }
}
