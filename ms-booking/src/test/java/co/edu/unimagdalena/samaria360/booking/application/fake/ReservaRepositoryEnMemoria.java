package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.application.port.out.ClaveIdempotenciaRepetidaException;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ReservaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.CupoDescontado;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** reservas_db en memoria, con la restricción única (turista, clave) de la base. */
public class ReservaRepositoryEnMemoria implements ReservaRepositoryPort {

    private final Map<UUID, Reserva> reservas = new LinkedHashMap<>();
    private final Map<UUID, List<CupoDescontado>> descuentos = new LinkedHashMap<>();
    private final Eventos eventos;

    public ReservaRepositoryEnMemoria(Eventos eventos) {
        this.eventos = eventos;
    }

    public int cantidad() {
        return reservas.size();
    }

    public void guardar(Reserva reserva) {
        reservas.put(reserva.idReserva(), reserva);
    }

    @Override
    public void insertar(Reserva reserva) {
        eventos.registrar("reservas.insertar");
        boolean repetida = reservas.values().stream().anyMatch(r -> r.idTurista().equals(reserva.idTurista())
                && r.claveIdempotencia().equals(reserva.claveIdempotencia()));
        if (repetida) {
            throw new ClaveIdempotenciaRepetidaException(null);
        }
        reservas.put(reserva.idReserva(), reserva);
    }

    @Override
    public Optional<Reserva> buscarPorClave(UUID idTurista, UUID claveIdempotencia) {
        eventos.registrar("reservas.buscarPorClave");
        return reservas.values().stream().filter(r -> r.idTurista().equals(idTurista)
                && r.claveIdempotencia().equals(claveIdempotencia)).findFirst();
    }

    @Override
    public Optional<Reserva> buscarDeTurista(UUID idReserva, UUID idTurista) {
        return Optional.ofNullable(reservas.get(idReserva)).filter(r -> r.idTurista().equals(idTurista));
    }

    @Override
    public PaginaReservas listarDeTurista(UUID idTurista, int pagina, int tamano) {
        List<Reserva> propias = reservas.values().stream().filter(r -> r.idTurista().equals(idTurista))
                .sorted(Comparator.comparing(Reserva::fechaCreacion).reversed()).toList();
        int desde = Math.min(pagina * tamano, propias.size());
        return new PaginaReservas(propias.subList(desde, Math.min(desde + tamano, propias.size())), pagina, tamano,
                propias.size());
    }

    @Override
    public boolean cancelarSiConfirmada(UUID idReserva, UUID idTurista, Instant ahora) {
        Reserva reserva = reservas.get(idReserva);
        if (reserva == null || !reserva.idTurista().equals(idTurista) || reserva.estado() != EstadoReserva.Confirmada) {
            return false;
        }
        reservas.put(idReserva, reserva.cancelada(ahora));
        return true;
    }

    @Override
    public void registrarDescuentos(UUID idReserva, List<CupoDescontado> nuevos) {
        eventos.registrar("reservas.descuentos");
        descuentos.computeIfAbsent(idReserva, id -> new ArrayList<>()).addAll(nuevos);
    }

    @Override
    public List<CupoDescontado> descuentosDe(UUID idReserva) {
        return List.copyOf(descuentos.getOrDefault(idReserva, List.of()));
    }
}
