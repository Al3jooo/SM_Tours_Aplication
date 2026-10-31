package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoModificadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;

/**
 * CU «Actualizar atractivo» (RF07, RF24, RF27, RF58) y CU «Cambiar estado operativo» (RF33). Cualquier Gestor, sobre
 * cualquier atractivo (DEC-206). Cada operación compara la versión leída y la sube (DEC-207). Las dos lanzan:
 * {@link AtractivoNoEncontradoException} si no existe o el id no es válido (404);
 * {@link ContenidoInvalidoException} con un motivo por cada campo inválido (400), y
 * {@link AtractivoModificadoException} si la versión ya no es la leída (409 ATRACTIVO_MODIFICADO).
 */
public interface ActualizarAtractivoUseCase {

    /** @param borrador su {@code estadoOperativo} se ignora: el estado cambia con {@link #cambiarEstado} */
    AtractivoPublico actualizar(String id, BorradorAtractivo borrador, Integer version);

    AtractivoPublico cambiarEstado(String id, String estadoNuevo, Integer version);
}
