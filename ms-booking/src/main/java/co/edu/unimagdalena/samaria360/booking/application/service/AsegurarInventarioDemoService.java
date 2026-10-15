package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.AsegurarInventarioDemoUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.InventarioPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.PaginaCatalogo;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadReservable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generador de inventario demo (D5). Recorre todas las páginas de servicios publicados de cada categoría reservable y
 * consulta el detalle de cada uno (la búsqueda no trae capacidades). Por cada servicio crea, para los próximos
 * {@value #DIAS_VENTANA} días desde hoy en Santa Marta:
 * <ul>
 *   <li>tour: una fila por horario ({@link #HORARIOS_TOUR}) con el aforo del grupo;</li>
 *   <li>alojamiento: una fila por unidad y noche con sus habitaciones (D1).</li>
 * </ul>
 * Solo inserta lo que falta: no cambia los cupos de filas existentes, aunque el catálogo haya cambiado (eso es RF19).
 * Si el catálogo no responde, la excepción sube y el programador vuelve a intentar.
 */
public class AsegurarInventarioDemoService implements AsegurarInventarioDemoUseCase {

    static final int DIAS_VENTANA = 60;
    static final List<LocalTime> HORARIOS_TOUR = List.of(LocalTime.of(8, 0), LocalTime.of(14, 0));

    private final CatalogoPort catalogo;
    private final InventarioPort inventario;
    private final RelojPort reloj;

    public AsegurarInventarioDemoService(CatalogoPort catalogo, InventarioPort inventario, RelojPort reloj) {
        this.catalogo = Objects.requireNonNull(catalogo);
        this.inventario = Objects.requireNonNull(inventario);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public int asegurar() {
        LocalDate hoy = HorarioReservas.hoy(reloj.ahora());
        int nuevas = 0;
        for (CategoriaReservable categoria : CategoriaReservable.values()) {
            int totalPaginas = 1;
            for (int pagina = 0; pagina < totalPaginas; pagina++) {
                PaginaCatalogo resultado = catalogo.listarPublicados(categoria, pagina);
                totalPaginas = resultado.totalPaginas();
                for (String idServicio : resultado.idsServicios()) {
                    nuevas += catalogo.buscarReservable(idServicio)
                            .map(oferta -> inventario.asegurar(filas(oferta, hoy)))
                            .orElse(0);
                }
            }
        }
        return nuevas;
    }

    static List<FilaInventarioNueva> filas(OfertaReservable oferta, LocalDate hoy) {
        List<FilaInventarioNueva> filas = new ArrayList<>();
        CategoriaReservable categoria = CategoriaReservable.desde(oferta.categoria()).orElse(null);
        for (int dia = 0; dia < DIAS_VENTANA; dia++) {
            LocalDate fecha = hoy.plusDays(dia);
            if (categoria == CategoriaReservable.Tour && oferta.aforoMaxGrupo() != null && oferta.aforoMaxGrupo() > 0) {
                for (LocalTime hora : HORARIOS_TOUR) {
                    filas.add(new FilaInventarioNueva(new DiaInventario(oferta.idServicio(), null, fecha, hora),
                            categoria, oferta.aforoMaxGrupo()));
                }
            } else if (categoria == CategoriaReservable.Alojamiento) {
                for (UnidadReservable unidad : oferta.unidades()) {
                    filas.add(new FilaInventarioNueva(
                            new DiaInventario(oferta.idServicio(), unidad.idUnidad(), fecha, null),
                            categoria, unidad.numHabitaciones()));
                }
            }
        }
        return filas;
    }
}
