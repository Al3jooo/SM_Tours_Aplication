package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.HistorialBusquedaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Historial en memoria, deduplicado por {@code (turista, clave)} como el índice único de M007. {@link #simularCarrera}
 * hace que el próximo upsert choque con el índice; {@code actualizacionesFallidas} hace que también fallen las
 * actualizaciones.
 */
public class HistorialBusquedaRepositoryEnMemoria implements HistorialBusquedaRepositoryPort {

    private record Registro(UUID turista, String id, CriteriosGuardados criterios, Instant fecha) {
    }

    private final List<Registro> registros = new ArrayList<>();
    private int secuencia;
    private boolean carrera;
    private boolean actualizacionesFallidas;
    private int actualizaciones;

    public void simularCarrera(boolean tambienFallaLaActualizacion) {
        this.carrera = true;
        this.actualizacionesFallidas = tambienFallaLaActualizacion;
    }

    public int actualizaciones() {
        return actualizaciones;
    }

    public int total() {
        return registros.size();
    }

    @Override
    public boolean guardar(UUID turistaId, CriteriosGuardados criterios, Instant fecha) {
        if (carrera) {
            carrera = false;
            return false;
        }
        if (!actualizar(turistaId, criterios, fecha)) {
            registros.add(new Registro(turistaId, String.format("%024x", ++secuencia), criterios, fecha));
        }
        return true;
    }

    @Override
    public boolean actualizar(UUID turistaId, CriteriosGuardados criterios, Instant fecha) {
        actualizaciones++;
        if (actualizacionesFallidas) {
            return false;
        }
        for (int i = 0; i < registros.size(); i++) {
            Registro r = registros.get(i);
            if (r.turista().equals(turistaId) && r.criterios().clave().equals(criterios.clave())) {
                registros.set(i, new Registro(turistaId, r.id(), criterios, fecha));
                return true;
            }
        }
        return false;
    }

    @Override
    public void podar(UUID turistaId, int conservar) {
        List<Registro> sobrantes = delTurista(turistaId).stream().skip(conservar).toList();
        registros.removeAll(sobrantes);
    }

    @Override
    public List<BusquedaReciente> listar(UUID turistaId, int maximo) {
        return delTurista(turistaId).stream().limit(maximo)
                .map(r -> new BusquedaReciente(r.id(), r.criterios(), r.fecha())).toList();
    }

    private List<Registro> delTurista(UUID turistaId) {
        return registros.stream().filter(r -> r.turista().equals(turistaId))
                .sorted(Comparator.comparing(Registro::fecha).thenComparing(Registro::id).reversed()).toList();
    }
}
