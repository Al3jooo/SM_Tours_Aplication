package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Datos de un evento tal como los envía el Gestor, sin validar (RF25). Las fechas llegan como texto
 * {@code yyyy-MM-dd} para responder {@code FORMATO} por campo (DEC-236). Lo valida
 * {@link EventoTuristico#registrar}; no trae lo que fija el servidor: el Gestor ni la fecha de registro.
 */
public record BorradorEvento(String nombre, String fechaInicio, String fechaFin, String ubicacion,
                             String descripcion) {
}
