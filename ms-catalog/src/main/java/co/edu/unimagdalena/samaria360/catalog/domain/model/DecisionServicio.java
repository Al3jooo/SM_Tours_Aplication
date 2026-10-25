package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Decisión del Administrador sobre un servicio Pendiente (RF21, DEC-128). Las únicas transiciones son Pendiente →
 * Publicado y Pendiente → Rechazado (DEC-124); el rechazo exige la justificación (CU «Justificar rechazo de servicio»).
 */
public enum DecisionServicio {
    Aprobar(EstadoPublicacion.Publicado),
    Rechazar(EstadoPublicacion.Rechazado);

    private final EstadoPublicacion estadoDestino;

    DecisionServicio(EstadoPublicacion estadoDestino) {
        this.estadoDestino = estadoDestino;
    }

    public EstadoPublicacion estadoDestino() {
        return estadoDestino;
    }
}
