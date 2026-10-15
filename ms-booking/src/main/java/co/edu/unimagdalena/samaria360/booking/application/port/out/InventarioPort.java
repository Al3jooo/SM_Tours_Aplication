package co.edu.unimagdalena.samaria360.booking.application.port.out;

import co.edu.unimagdalena.samaria360.booking.domain.model.CupoDia;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.RangoFechas;

import java.util.List;
import java.util.Map;

/**
 * Cupos por día ({@code inventario_cupo}), la fuente de verdad de lo que queda (§2 del plan). Los cambios de cupos son
 * UPDATE condicionales sobre una fila a la vez (D3), en el orden en que los pide el caso de uso.
 */
public interface InventarioPort {

    /** Las filas que existen de esos días, en {@link DiaInventario#ORDEN}. */
    List<FilaInventario> buscarFilas(List<DiaInventario> dias);

    /** Descuenta si alcanza: {@code cupos_disponibles >= cantidad}. @return false si no alcanzó (no cambia nada) */
    boolean descontar(long idInventario, int cantidad);

    /** Devuelve cupos de una reserva cancelada; el CHECK impide superar los totales. */
    void liberar(long idInventario, int cantidad);

    /** {@code INSERT … ON CONFLICT DO NOTHING}: las filas que ya existen no cambian (D5). @return filas nuevas */
    int asegurar(List<FilaInventarioNueva> filas);

    /** @param idUnidad null = todas las unidades (o un tour) */
    List<CupoDia> consultar(String idServicio, String idUnidad, RangoFechas rango);

    /** Un estado por cada servicio pedido, en el mismo orden. */
    Map<String, EstadoDisponibilidad> resumir(List<String> idsServicios, RangoFechas rango);
}
