package co.edu.unimagdalena.samaria360.booking.domain.model;

/**
 * Resumen de disponibilidad de un servicio para las tarjetas (§3 del plan) en una ventana de días.
 * <ul>
 *   <li>{@link #CON_CUPOS}: alguna unidad u horario tiene cupos en algún día;</li>
 *   <li>{@link #AGOTADO}: hay inventario, pero todo en 0;</li>
 *   <li>{@link #SIN_INVENTARIO}: no hay filas (el servicio no se ofrece en esas fechas).</li>
 * </ul>
 */
public enum EstadoDisponibilidad {
    CON_CUPOS,
    AGOTADO,
    SIN_INVENTARIO
}
