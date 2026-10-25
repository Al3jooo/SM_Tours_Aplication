package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.application.port.out.InventarioPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.CupoDia;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.RangoFechas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** inventario_cupo en memoria, con las mismas reglas: descuento condicional y nunca por encima del total. */
public class InventarioEnMemoria implements InventarioPort {

    public static final class Fila {
        final long id;
        final DiaInventario dia;
        final CategoriaReservable categoria;
        final int totales;
        int disponibles;

        Fila(long id, DiaInventario dia, CategoriaReservable categoria, int totales, int disponibles) {
            this.id = id;
            this.dia = dia;
            this.categoria = categoria;
            this.totales = totales;
            this.disponibles = disponibles;
        }
    }

    private final Map<DiaInventario, Fila> filas = new LinkedHashMap<>();
    private final AtomicLong secuencia = new AtomicLong(1);
    private final Eventos eventos;

    public InventarioEnMemoria(Eventos eventos) {
        this.eventos = eventos;
    }

    public InventarioEnMemoria con(DiaInventario dia, CategoriaReservable categoria, int totales, int disponibles) {
        filas.put(dia, new Fila(secuencia.getAndIncrement(), dia, categoria, totales, disponibles));
        return this;
    }

    public int disponibles(DiaInventario dia) {
        return filas.get(dia).disponibles;
    }

    public int totales(DiaInventario dia) {
        return filas.get(dia).totales;
    }

    public int cantidadFilas() {
        return filas.size();
    }

    public List<DiaInventario> dias() {
        return List.copyOf(filas.keySet());
    }

    @Override
    public List<FilaInventario> buscarFilas(List<DiaInventario> dias) {
        eventos.registrar("inventario.buscar");
        return dias.stream().sorted(DiaInventario.ORDEN).filter(filas::containsKey)
                .map(dia -> new FilaInventario(filas.get(dia).id, dia)).toList();
    }

    @Override
    public boolean descontar(long idInventario, int cantidad) {
        eventos.registrar("inventario.descontar");
        Fila fila = porId(idInventario);
        if (fila.disponibles < cantidad) {
            return false;
        }
        fila.disponibles -= cantidad;
        return true;
    }

    @Override
    public void liberar(long idInventario, int cantidad) {
        eventos.registrar("inventario.liberar");
        Fila fila = porId(idInventario);
        if (fila.disponibles + cantidad > fila.totales) {
            throw new IllegalStateException("ck_inventario_cupo_disponibles");
        }
        fila.disponibles += cantidad;
    }

    @Override
    public int asegurar(List<FilaInventarioNueva> nuevas) {
        eventos.registrar("inventario.asegurar");
        int insertadas = 0;
        for (FilaInventarioNueva nueva : nuevas) {
            if (!filas.containsKey(nueva.dia())) {
                con(nueva.dia(), nueva.categoria(), nueva.cupos(), nueva.cupos());
                insertadas++;
            }
        }
        return insertadas;
    }

    @Override
    public List<CupoDia> consultar(String idServicio, String idUnidad, RangoFechas rango) {
        List<CupoDia> cupos = new ArrayList<>();
        for (Fila fila : filas.values()) {
            DiaInventario dia = fila.dia;
            if (dia.idServicio().equals(idServicio) && (idUnidad == null || idUnidad.equals(dia.idUnidad()))
                    && !dia.fecha().isBefore(rango.desde()) && !dia.fecha().isAfter(rango.hasta())) {
                cupos.add(new CupoDia(fila.categoria, dia.idUnidad(), dia.fecha(), dia.hora(), fila.totales,
                        fila.disponibles));
            }
        }
        return cupos;
    }

    @Override
    public Map<String, EstadoDisponibilidad> resumir(List<String> idsServicios, RangoFechas rango) {
        Map<String, EstadoDisponibilidad> resumen = new LinkedHashMap<>();
        for (String id : idsServicios) {
            List<CupoDia> cupos = consultar(id, null, rango);
            resumen.put(id, cupos.isEmpty() ? EstadoDisponibilidad.SIN_INVENTARIO
                    : cupos.stream().anyMatch(c -> c.cuposDisponibles() > 0) ? EstadoDisponibilidad.CON_CUPOS
                    : EstadoDisponibilidad.AGOTADO);
        }
        return resumen;
    }

    private Fila porId(long id) {
        return filas.values().stream().filter(f -> f.id == id).findFirst().orElseThrow();
    }
}
