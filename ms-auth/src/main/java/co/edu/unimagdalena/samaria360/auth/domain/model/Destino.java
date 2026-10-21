package co.edu.unimagdalena.samaria360.auth.domain.model;

/** Servicio que mantiene una proyección de las cuentas (ADR-008). El texto es el valor de {@code aud} y de la columna. */
public enum Destino {
    MS_CATALOG("ms-catalog"),
    MS_BOOKING("ms-booking");

    private final String texto;

    Destino(String texto) {
        this.texto = texto;
    }

    public String texto() {
        return texto;
    }

    public static Destino desdeTexto(String texto) {
        for (Destino destino : values()) {
            if (destino.texto.equals(texto)) {
                return destino;
            }
        }
        throw new IllegalArgumentException("Destino desconocido: " + texto);
    }
}
