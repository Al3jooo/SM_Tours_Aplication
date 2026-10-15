package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.util.Optional;

/** Categorías del catálogo que se pueden reservar en esta iteración (D2). Gastronomía y alquiler de equipos, no. */
public enum CategoriaReservable {
    Tour,
    Alojamiento;

    /** @param categoria el nombre de la categoría en ms-catalog */
    public static Optional<CategoriaReservable> desde(String categoria) {
        for (CategoriaReservable valor : values()) {
            if (valor.name().equals(categoria)) {
                return Optional.of(valor);
            }
        }
        return Optional.empty();
    }
}
