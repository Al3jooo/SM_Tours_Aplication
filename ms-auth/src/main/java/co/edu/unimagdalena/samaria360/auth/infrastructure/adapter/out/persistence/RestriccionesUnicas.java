package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import java.util.Optional;

/**
 * Reconoce una violación de restricción única por su SQLState (23505, unique_violation) y por el <b>nombre</b> de la
 * restricción, que PostgreSQL envía como campo propio del error (ServerErrorMessage). Nunca se compara el texto del
 * mensaje, que cambia con la versión y el idioma del servidor; por eso no se usa
 * {@code ConstraintViolationException.getConstraintName()} de Hibernate, que en PostgreSQL lo extrae del texto.
 */
final class RestriccionesUnicas {

    static final String VIOLACION_UNICA = "23505";
    static final String CORREO = "uq_usuario_correo";
    static final String RNT = "uq_prestador_rnt";

    private RestriccionesUnicas() {
    }

    /** Nombre de la restricción única violada, si la causa es una. */
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
