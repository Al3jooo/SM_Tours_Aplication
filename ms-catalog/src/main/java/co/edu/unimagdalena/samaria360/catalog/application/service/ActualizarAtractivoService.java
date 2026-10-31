package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ActualizarAtractivoUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionAtractivosPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoModificadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ActualizacionAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * CU «Actualizar atractivo» (RF07, RF24, RF27, RF58) y «Cambiar estado operativo» (RF33). Orden: el atractivo (404),
 * los datos (400) y la escritura condicional sobre la versión leída (DEC-207). Si la escritura no coincide, se vuelve a
 * leer para distinguir el 404 del 409.
 */
public class ActualizarAtractivoService implements ActualizarAtractivoUseCase {

    private final GestionAtractivosPort atractivos;
    private final RelojPort reloj;

    public ActualizarAtractivoService(GestionAtractivosPort atractivos, RelojPort reloj) {
        this.atractivos = Objects.requireNonNull(atractivos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public AtractivoPublico actualizar(String id, BorradorAtractivo borrador, Integer version) {
        return editar(id, (actual, fecha) -> ActualizacionAtractivo.aplicar(actual, borrador, version, fecha),
                atractivos::actualizar);
    }

    @Override
    public AtractivoPublico cambiarEstado(String id, String estadoNuevo, Integer version) {
        return editar(id, (actual, fecha) -> ActualizacionAtractivo.cambiarEstado(actual, estadoNuevo, version, fecha),
                atractivos::cambiarEstado);
    }

    /** La fecha va en milisegundos, como la guarda MongoDB. */
    private AtractivoPublico editar(String id, BiFunction<AtractivoPublico, Instant, AtractivoPublico> edicion,
                                    Predicate<AtractivoPublico> escritura) {
        AtractivoPublico actual = existente(id);
        if (!escritura.test(edicion.apply(actual, reloj.ahora().truncatedTo(ChronoUnit.MILLIS)))) {
            throw new AtractivoModificadoException(existente(id).getVersion());
        }
        return existente(id);
    }

    private AtractivoPublico existente(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new AtractivoNoEncontradoException(id);
        }
        return atractivos.buscar(id).orElseThrow(() -> new AtractivoNoEncontradoException(id));
    }
}
