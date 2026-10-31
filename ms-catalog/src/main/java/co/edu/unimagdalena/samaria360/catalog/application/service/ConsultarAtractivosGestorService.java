package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarAtractivosGestorUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionAtractivosPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.Map;
import java.util.Objects;

/** Panel «Atractivos» del Gestor (WF-24): todos los atractivos, no solo los que registró (DEC-206). */
public class ConsultarAtractivosGestorService implements ConsultarAtractivosGestorUseCase {

    private final GestionAtractivosPort atractivos;

    public ConsultarAtractivosGestorService(GestionAtractivosPort atractivos) {
        this.atractivos = Objects.requireNonNull(atractivos);
    }

    @Override
    public PaginaResultados<AtractivoPublico> listar(int pagina) {
        if (pagina < 0) {
            throw new ContenidoInvalidoException(Map.of("pagina", MotivoInvalido.FORMATO));
        }
        return atractivos.listar(pagina, TAMANO_PAGINA);
    }

    @Override
    public AtractivoPublico detalle(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new AtractivoNoEncontradoException(id);
        }
        return atractivos.buscar(id).orElseThrow(() -> new AtractivoNoEncontradoException(id));
    }
}
