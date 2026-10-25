package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** ms-catalog en memoria: detalle de las ofertas visibles (404 para las demás). Puede estar «caído». */
public class CatalogoFalso implements CatalogoPort {

    private final Map<String, OfertaReservable> ofertas = new LinkedHashMap<>();
    private final Eventos eventos;
    private boolean caido;
    private int consultasDetalle;

    public CatalogoFalso(Eventos eventos) {
        this.eventos = eventos;
    }

    public CatalogoFalso con(OfertaReservable... nuevas) {
        for (OfertaReservable oferta : nuevas) {
            ofertas.put(oferta.idServicio(), oferta);
        }
        return this;
    }

    public void caer() {
        caido = true;
    }

    public int consultasDetalle() {
        return consultasDetalle;
    }

    @Override
    public Optional<OfertaReservable> buscarReservable(String idServicio) {
        eventos.registrar("catalogo.detalle");
        if (caido) {
            throw new CatalogoNoDisponibleException("ms-catalog caído (prueba)", null);
        }
        consultasDetalle++;
        return Optional.ofNullable(ofertas.get(idServicio));
    }
}
