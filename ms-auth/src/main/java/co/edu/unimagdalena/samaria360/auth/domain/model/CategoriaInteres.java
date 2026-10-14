package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.util.Optional;

/**
 * Categoría de interés turístico del turista (RF01, RF53). Decisión D1: vocabulario cerrado de las tres que muestra
 * WF-02; el turista elige al menos una. La base aplica el mismo vocabulario con un CHECK (V4).
 */
public enum CategoriaInteres {
    Naturaleza,
    Cultura,
    Aventura;

    /** Nombre exacto, como lo envía el formulario; cualquier otro valor no está permitido. */
    public static Optional<CategoriaInteres> desdeTexto(String texto) {
        for (CategoriaInteres categoria : values()) {
            if (categoria.name().equals(texto)) {
                return Optional.of(categoria);
            }
        }
        return Optional.empty();
    }
}
