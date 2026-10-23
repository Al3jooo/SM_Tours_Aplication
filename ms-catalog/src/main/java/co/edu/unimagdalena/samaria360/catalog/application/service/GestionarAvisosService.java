package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarAvisosUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.AvisoRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AvisoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AvisoYaInactivoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CU «Crear y publicar aviso general» (RF40). Despublicar es un UPDATE condicional (activo → inactivo, como DEC-39): si
 * no cambia nada, se busca el aviso para distinguir el 404 (no existe) del 409 (ya estaba inactivo, DEC-64).
 */
public class GestionarAvisosService implements GestionarAvisosUseCase {

    private final AvisoRepositoryPort avisos;
    private final RelojPort reloj;

    public GestionarAvisosService(AvisoRepositoryPort avisos, RelojPort reloj) {
        this.avisos = Objects.requireNonNull(avisos);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public List<Aviso> activos() {
        return avisos.activos();
    }

    @Override
    public PaginaResultados<Aviso> pagina(int pagina) {
        if (pagina < 0) {
            throw new ContenidoInvalidoException(Map.of("pagina", MotivoInvalido.FORMATO));
        }
        return avisos.pagina(pagina, TAMANO_PAGINA);
    }

    /** La fecha va en milisegundos, como la guarda MongoDB. */
    @Override
    public Aviso publicar(String mensaje, String tipo) {
        Aviso aviso = Aviso.publicar(avisos.nuevoId(), mensaje, tipo, reloj.ahora().truncatedTo(ChronoUnit.MILLIS));
        avisos.insertar(aviso);
        return aviso;
    }

    @Override
    public Aviso despublicar(String id) {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new AvisoNoEncontradoException(id);
        }
        if (!avisos.despublicar(id)) {
            avisos.buscar(id).orElseThrow(() -> new AvisoNoEncontradoException(id));
            throw new AvisoYaInactivoException(id);
        }
        return avisos.buscar(id).orElseThrow(() -> new AvisoNoEncontradoException(id));
    }
}
