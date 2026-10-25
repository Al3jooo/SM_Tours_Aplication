package co.edu.unimagdalena.samaria360.booking.application.fake;

import co.edu.unimagdalena.samaria360.booking.application.port.out.OfertaMaterializadaPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** oferta_materializada en memoria, con la misma regla: la marca solo avanza (GREATEST). */
public class OfertaMaterializadaEnMemoria implements OfertaMaterializadaPort {

    private final Map<String, LocalDate> marcas = new HashMap<>();
    private final Eventos eventos;

    public OfertaMaterializadaEnMemoria(Eventos eventos) {
        this.eventos = eventos;
    }

    public OfertaMaterializadaEnMemoria con(String idServicio, LocalDate hasta) {
        marcas.put(idServicio, hasta);
        return this;
    }

    public LocalDate marca(String idServicio) {
        return marcas.get(idServicio);
    }

    @Override
    public Map<String, LocalDate> materializadaHasta(Collection<String> idsServicios) {
        eventos.registrar("marcas.consultar");
        Map<String, LocalDate> encontradas = new HashMap<>();
        idsServicios.stream().filter(marcas::containsKey).forEach(id -> encontradas.put(id, marcas.get(id)));
        return encontradas;
    }

    @Override
    public void registrar(String idServicio, LocalDate hasta, Instant ahora) {
        eventos.registrar("marcas.registrar");
        marcas.merge(idServicio, hasta, (actual, nueva) -> nueva.isAfter(actual) ? nueva : actual);
    }
}
