package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.PaginaCatalogo;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ms-catalog en memoria: detalle de las ofertas visibles y búsqueda paginada de 10. Puede estar «caído». Los ids en
 * {@code soloEnBusqueda} aparecen en la búsqueda pero su detalle da 404 (dejaron de ser visibles entre una llamada y
 * otra).
 */
public class CatalogoFalso implements CatalogoPort {

    private static final int TAMANO_PAGINA = 10;

    private final Map<String, OfertaReservable> ofertas = new LinkedHashMap<>();
    private final List<String> soloEnBusqueda = new ArrayList<>();
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

    public CatalogoFalso conNoVisibleEnDetalle(String idServicio) {
        soloEnBusqueda.add(idServicio);
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
        disponible();
        consultasDetalle++;
        return Optional.ofNullable(ofertas.get(idServicio));
    }

    @Override
    public PaginaCatalogo listarPublicados(CategoriaReservable categoria, int pagina) {
        eventos.registrar("catalogo.busqueda");
        disponible();
        List<String> ids = new ArrayList<>(ofertas.values().stream()
                .filter(oferta -> oferta.categoria().equals(categoria.name()))
                .map(OfertaReservable::idServicio).toList());
        if (categoria == CategoriaReservable.Tour) {
            ids.addAll(soloEnBusqueda);
        }
        int totalPaginas = (ids.size() + TAMANO_PAGINA - 1) / TAMANO_PAGINA;
        int desde = Math.min(pagina * TAMANO_PAGINA, ids.size());
        return new PaginaCatalogo(ids.subList(desde, Math.min(desde + TAMANO_PAGINA, ids.size())), totalPaginas);
    }

    private void disponible() {
        if (caido) {
            throw new CatalogoNoDisponibleException("ms-catalog caído (prueba)", null);
        }
    }
}
