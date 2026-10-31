package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;

/** CU «Consultar detalle de atractivo» (RF06, RF07, RF24, RF27, RF33, RF58; DEC-87, DEC-217): público, sin token. */
public interface ConsultarAtractivoUseCase {

    /** @throws AtractivoNoEncontradoException si no existe o el id no es un ObjectId */
    AtractivoPublico detalle(String id);
}
