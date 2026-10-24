package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.HistorialBusquedasUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.HistorialBusquedaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CU «Consultar historial de búsquedas» (RF60). La poda va después de cada escritura y no es atómica: con dos
 * búsquedas simultáneas pueden quedar 11 un instante, pero la consulta siempre devuelve 10 como máximo.
 */
public class HistorialBusquedasService implements HistorialBusquedasUseCase {

    private final HistorialBusquedaRepositoryPort historial;
    private final RelojPort reloj;

    public HistorialBusquedasService(HistorialBusquedaRepositoryPort historial, RelojPort reloj) {
        this.historial = Objects.requireNonNull(historial);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /**
     * Si el upsert choca con el índice único (la misma combinación llegó a la vez), se reintenta una vez como
     * actualización; si tampoco cambia nada, la otra solicitud ya dejó la búsqueda registrada y no es un error.
     */
    @Override
    public void registrar(UUID turistaId, CriteriosGuardados criterios) {
        if (criterios.esVacio()) {
            return;
        }
        Instant ahora = reloj.ahora().truncatedTo(ChronoUnit.MILLIS);
        if (!historial.guardar(turistaId, criterios, ahora)) {
            historial.actualizar(turistaId, criterios, ahora);
        }
        historial.podar(turistaId, BusquedaReciente.MAXIMO_POR_TURISTA);
    }

    @Override
    public List<BusquedaReciente> listar(UUID turistaId) {
        return historial.listar(turistaId, BusquedaReciente.MAXIMO_POR_TURISTA);
    }
}
