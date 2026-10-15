package co.edu.unimagdalena.samaria360.booking.application.port.in;

/**
 * Generador de inventario de demostración (D5), sustituto temporal de RF19: crea las filas que falten para los
 * próximos 60 días de los tours (08:00 y 14:00) y de cada unidad de alojamiento publicada. Nunca cambia filas que ya
 * existen.
 */
public interface AsegurarInventarioDemoUseCase {

    /** @return filas nuevas */
    int asegurar();
}
