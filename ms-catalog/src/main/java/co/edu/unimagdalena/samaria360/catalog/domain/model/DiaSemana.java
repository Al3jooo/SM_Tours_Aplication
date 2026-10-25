package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Día de operación de la oferta de un servicio (DEC-136). Sin tildes, como {@code Gastronomia}: son los valores que
 * guarda MongoDB y que viajan en la API.
 */
public enum DiaSemana {
    Lunes,
    Martes,
    Miercoles,
    Jueves,
    Viernes,
    Sabado,
    Domingo
}
