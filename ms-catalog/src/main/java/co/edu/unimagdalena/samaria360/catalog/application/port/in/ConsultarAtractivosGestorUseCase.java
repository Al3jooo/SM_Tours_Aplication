package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

/** Flujo «Consultar atractivos» del CU «Actualizar atractivo»: el panel «Atractivos» del Gestor (WF-24, DEC-206). */
public interface ConsultarAtractivosGestorUseCase {

    int TAMANO_PAGINA = 10;

    PaginaResultados<AtractivoPublico> listar(int pagina);

    /** Con la versión, para editar. @throws AtractivoNoEncontradoException si no existe o el id no es válido */
    AtractivoPublico detalle(String id);
}
