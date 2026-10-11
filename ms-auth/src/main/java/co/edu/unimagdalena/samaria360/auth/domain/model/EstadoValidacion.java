package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Estado de validación de un Prestador por parte del Administrador (RF02, RF28).
 * Es independiente de {@link EstadoCuenta}: no interviene en el inicio de sesión;
 * solo habilita el registro de servicios (RF09).
 */
public enum EstadoValidacion {
    /** "Pendiente de Validación" (RF02). */
    Pendiente("Pendiente"),
    Activo("Activo"),
    Rechazado("Rechazado");

    private final String texto;

    EstadoValidacion(String texto) {
        this.texto = texto;
    }

    /** Valor almacenado en {@code prestador.estado_validacion}. */
    public String texto() {
        return texto;
    }

    public static EstadoValidacion desdeTexto(String texto) {
        for (EstadoValidacion estado : values()) {
            if (estado.texto.equals(texto)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de validación desconocido: " + texto);
    }
}
