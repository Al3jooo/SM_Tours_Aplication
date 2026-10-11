package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Condiciones de accesibilidad que declaran prestadores y gestores (RF27) y por las que filtra el turista (RF50).
 * El esquema de MongoDB las guarda como texto libre; el dominio solo acepta los tres valores del SRS.
 */
public enum CondicionAccesibilidad {
    MovilidadReducida,
    Visual,
    Auditiva;

    public static CondicionAccesibilidad desdeTexto(String texto) {
        for (CondicionAccesibilidad condicion : values()) {
            if (condicion.name().equals(texto)) {
                return condicion;
            }
        }
        throw new IllegalArgumentException("Condición de accesibilidad desconocida: " + texto);
    }
}
