package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.InventarioPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.OfertaMaterializadaPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.service.MaterializacionOferta;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Materializa bajo demanda la oferta del catálogo en {@code inventario_cupo} (DEC-135, DEC-152 a DEC-158). Lo usan la
 * disponibilidad, el resumen y reservar, siempre <b>fuera</b> de una transacción (ADR-007):
 * <ul>
 *   <li>si la marca del servicio ya cubre la fecha pedida (recortada al horizonte), no hace nada ni llama al catálogo;</li>
 *   <li>si no, inserta las filas de toda la ventana con {@code ON CONFLICT DO NOTHING} (las que existen no cambian,
 *   tampoco sus cupos) y después avanza la marca, aunque no haya filas (otra categoría o sin oferta);</li>
 *   <li>un 404 del catálogo no materializa ni avanza la marca: si el servicio se publica después, se materializa;</li>
 *   <li>con el catálogo caído, {@code CatalogoNoDisponibleException} sube y corta: no se borra nada (DEC-155).</li>
 * </ul>
 */
public class MaterializarInventarioService {

    private final CatalogoPort catalogo;
    private final InventarioPort inventario;
    private final OfertaMaterializadaPort marcas;
    private final RelojPort reloj;

    public MaterializarInventarioService(CatalogoPort catalogo, InventarioPort inventario,
                                         OfertaMaterializadaPort marcas, RelojPort reloj) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.inventario = Objects.requireNonNull(inventario);
        this.marcas = Objects.requireNonNull(marcas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** Disponibilidad y resumen: consulta el catálogo solo por los servicios que faltan, de a uno y en orden. */
    public void asegurar(Collection<String> idsServicios, LocalDate hasta) {
        LocalDate hoy = HorarioReservas.hoy(reloj.ahora());
        LocalDate objetivo = objetivo(hoy, hasta);
        if (objetivo == null || idsServicios.isEmpty()) {
            return;
        }
        Map<String, LocalDate> materializadas = marcas.materializadaHasta(idsServicios);
        for (String idServicio : idsServicios) {
            if (!cubierto(materializadas.get(idServicio), objetivo)) {
                catalogo.buscarReservable(idServicio).ifPresent(oferta -> materializar(oferta, hoy));
            }
        }
    }

    /** Reservar: con la oferta que ya se leyó del catálogo, sin otra llamada. */
    public void asegurar(OfertaReservable oferta, LocalDate hasta) {
        LocalDate hoy = HorarioReservas.hoy(reloj.ahora());
        LocalDate objetivo = objetivo(hoy, hasta);
        if (objetivo == null) {
            return;
        }
        if (!cubierto(marcas.materializadaHasta(List.of(oferta.idServicio())).get(oferta.idServicio()), objetivo)) {
            materializar(oferta, hoy);
        }
    }

    /** La fecha hasta la que hace falta materializar, o null si todo lo pedido es pasado. */
    private static LocalDate objetivo(LocalDate hoy, LocalDate hasta) {
        LocalDate ultimo = MaterializacionOferta.ultimoDia(hoy);
        LocalDate objetivo = hasta.isAfter(ultimo) ? ultimo : hasta;
        return objetivo.isBefore(hoy) ? null : objetivo;
    }

    private static boolean cubierto(LocalDate marca, LocalDate objetivo) {
        return marca != null && !marca.isBefore(objetivo);
    }

    /** Primero las filas y después la marca: si algo falla entre los dos, la siguiente vez se repite sin efectos. */
    private void materializar(OfertaReservable oferta, LocalDate hoy) {
        List<FilaInventarioNueva> filas = MaterializacionOferta.filas(oferta, hoy);
        if (!filas.isEmpty()) {
            inventario.asegurar(filas);
        }
        marcas.registrar(oferta.idServicio(), MaterializacionOferta.ultimoDia(hoy), reloj.ahora());
    }
}
