package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.Optional;

/**
 * Reconoce una violación de restricción única por su SQLState (23505) y por el <b>nombre</b> de la restricción, que
 * PostgreSQL envía como campo propio del error. Nunca se compara el texto del mensaje (como en ms-auth, ADR-006).
 */
final class RestriccionesUnicas {

    static final String VIOLACION_UNICA = "23505";
    static final String CLAVE_IDEMPOTENCIA = "uq_reserva_turista_clave";

    private RestriccionesUnicas() {
    }

    static Optional<String> violada(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof PSQLException postgres && VIOLACION_UNICA.equals(postgres.getSQLState())) {
                ServerErrorMessage detalle = postgres.getServerErrorMessage();
                if (detalle != null && detalle.getConstraint() != null) {
                    return Optional.of(detalle.getConstraint());
                }
            }
            if (causa.getCause() == causa) {
                break;
            }
        }
        return Optional.empty();
    }
}
