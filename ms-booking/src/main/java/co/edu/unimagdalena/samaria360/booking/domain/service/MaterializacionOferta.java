package co.edu.unimagdalena.samaria360.booking.domain.service;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaOperativa;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadReservable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Filas de inventario que corresponden a la oferta del catálogo (DEC-135, DEC-152, DEC-153), para materializarlas bajo
 * demanda. Cubre siempre la ventana completa, de hoy a {@link #ultimoDia}: nunca fechas pasadas ni posteriores.
 * <ul>
 *   <li>tour: una fila por salida de cada día de operación, con {@code aforoMaxGrupo} cupos;</li>
 *   <li>alojamiento: una fila por unidad y noche de operación, con sus {@code numHabitaciones} (D1, D2).</li>
 * </ul>
 * Sin oferta, en otra categoría o con capacidad 0 no hay filas (DEC-157). Con la oferta de la semilla (todos los días;
 * 08:00 y 14:00) produce las mismas filas que el generador de inventario demo que reemplaza (D5, DEC-156).
 */
public final class MaterializacionOferta {

    /** Días de la ventana, contando hoy (DEC-153; el mismo horizonte que tenía D5 y que usa WF-10). */
    public static final int DIAS_HORIZONTE = 60;

    /**
     * Orden total con el que se insertan las filas (DEC-158): dos materializaciones simultáneas toman los índices en el
     * mismo orden y no se esperan entre sí en círculo.
     */
    static final Comparator<DiaInventario> ORDEN_INSERCION = Comparator.comparing(DiaInventario::fecha)
            .thenComparing(DiaInventario::idUnidad, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(DiaInventario::hora, Comparator.nullsFirst(Comparator.naturalOrder()));

    private MaterializacionOferta() {
    }

    /** Último día de la ventana que empieza hoy (incluido). */
    public static LocalDate ultimoDia(LocalDate hoy) {
        return hoy.plusDays(DIAS_HORIZONTE - 1L);
    }

    public static List<FilaInventarioNueva> filas(OfertaReservable oferta, LocalDate hoy) {
        CategoriaReservable categoria = CategoriaReservable.desde(oferta.categoria()).orElse(null);
        OfertaOperativa operativa = oferta.oferta();
        List<FilaInventarioNueva> filas = new ArrayList<>();
        if (categoria == null || operativa == null) {
            return filas;
        }
        for (LocalDate fecha = hoy; !fecha.isAfter(ultimoDia(hoy)); fecha = fecha.plusDays(1)) {
            if (!operativa.opera(fecha)) {
                continue;
            }
            if (categoria == CategoriaReservable.Tour) {
                if (oferta.aforoMaxGrupo() != null && oferta.aforoMaxGrupo() > 0) {
                    for (LocalTime salida : operativa.salidas()) {
                        filas.add(new FilaInventarioNueva(new DiaInventario(oferta.idServicio(), null, fecha, salida),
                                categoria, oferta.aforoMaxGrupo()));
                    }
                }
            } else {
                for (UnidadReservable unidad : oferta.unidades()) {
                    if (unidad.numHabitaciones() > 0) {
                        filas.add(new FilaInventarioNueva(
                                new DiaInventario(oferta.idServicio(), unidad.idUnidad(), fecha, null),
                                categoria, unidad.numHabitaciones()));
                    }
                }
            }
        }
        filas.sort(Comparator.comparing(FilaInventarioNueva::dia, ORDEN_INSERCION));
        return filas;
    }
}
