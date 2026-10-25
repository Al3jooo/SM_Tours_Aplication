package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Quién aprobó o rechazó un servicio y cuándo (RF21, DEC-145), como {@code prestador.revisado_por} y
 * {@code revisado_en} en RF28. {@code motivoRechazo} es la justificación obligatoria del rechazo: de 1 a 500 caracteres
 * sin los espacios de los extremos (como DEC-42); nula al aprobar.
 */
public record Revision(UUID revisadoPor, Instant fechaRevision, String motivoRechazo) {

    public static final int JUSTIFICACION_MAXIMA = 500;

    public Revision {
        Objects.requireNonNull(revisadoPor, "revisadoPor");
        Objects.requireNonNull(fechaRevision, "fechaRevision");
        if (motivoRechazo != null) {
            TextoAcotado.exigir(motivoRechazo, JUSTIFICACION_MAXIMA, "motivoRechazo");
        }
    }
}
