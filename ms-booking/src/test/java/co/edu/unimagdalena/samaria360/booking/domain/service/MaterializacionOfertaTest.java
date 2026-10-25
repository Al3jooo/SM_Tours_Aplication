package co.edu.unimagdalena.samaria360.booking.domain.service;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.FilaInventarioNueva;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaOperativa;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadReservable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.HOY;
import static org.assertj.core.api.Assertions.assertThat;

/** Filas de la oferta (DEC-152, DEC-153, DEC-157): días de operación, salidas o unidades, ventana de 60 días y orden. */
class MaterializacionOfertaTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DOS = LocalTime.of(14, 0);

    @Test
    void unTourConDiasParcialesSoloTieneFilasEnSusDiasYUnaPorSalidaConElAforo() {
        OfertaReservable tour = Ofertas.tourQueSale(EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), DOS, OCHO);

        List<FilaInventarioNueva> filas = MaterializacionOferta.filas(tour, HOY);

        // HOY es domingo: en 60 días hay 8 sábados y 9 domingos.
        assertThat(filas).hasSize(17 * 2).allSatisfy(fila -> {
            assertThat(fila.dia().fecha().getDayOfWeek()).isIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            assertThat(fila.categoria()).isEqualTo(CategoriaReservable.Tour);
            assertThat(fila.cupos()).isEqualTo(12);
            assertThat(fila.dia().idUnidad()).isNull();
        });
        assertThat(filas).extracting(fila -> fila.dia().hora()).containsOnly(OCHO, DOS);
        assertThat(filas.get(0).dia()).isEqualTo(new DiaInventario(Ofertas.TOUR, null, HOY, OCHO));
    }

    @Test
    void unAlojamientoTieneUnaFilaPorUnidadYNocheDeOperacionConSusHabitaciones() {
        OfertaReservable hotel = Ofertas.hotelQueOpera(EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY));

        List<FilaInventarioNueva> filas = MaterializacionOferta.filas(hotel, HOY);

        // 8 viernes y 8 sábados, por 2 unidades.
        assertThat(filas).hasSize(16 * 2).allSatisfy(fila -> {
            assertThat(fila.dia().fecha().getDayOfWeek()).isIn(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
            assertThat(fila.dia().hora()).isNull();
            assertThat(fila.cupos()).isEqualTo(fila.dia().idUnidad().equals(Ofertas.DOBLE) ? 12 : 3);
        });
    }

    @Test
    void nuncaHayFechasPasadasNiPosterioresAlHorizonte() {
        List<FilaInventarioNueva> filas = MaterializacionOferta.filas(
                Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, OCHO), HOY);

        assertThat(filas).extracting(fila -> fila.dia().fecha())
                .allMatch(fecha -> !fecha.isBefore(HOY) && !fecha.isAfter(HOY.plusDays(59)))
                .contains(HOY, HOY.plusDays(59));
        assertThat(MaterializacionOferta.ultimoDia(HOY)).isEqualTo(HOY.plusDays(59));
    }

    /** DEC-156: la oferta que M008 rellenó en la semilla produce las mismas filas que el generador D5. */
    @Test
    void conLaOfertaDeLaSemillaLasFilasSonLasDelGeneradorD5() {
        OfertaReservable tour = Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, OCHO, DOS);
        OfertaReservable hotel = Ofertas.hotelQueOpera(Ofertas.TODOS_LOS_DIAS);

        assertThat(MaterializacionOferta.filas(tour, HOY)).containsExactlyInAnyOrderElementsOf(generadorD5(tour));
        assertThat(MaterializacionOferta.filas(hotel, HOY)).containsExactlyInAnyOrderElementsOf(generadorD5(hotel));
        assertThat(MaterializacionOferta.filas(tour, HOY)).hasSize(60 * 2);
        assertThat(MaterializacionOferta.filas(hotel, HOY)).hasSize(60 * 2);
    }

    @Test
    void sinOfertaEnOtraCategoriaOConCapacidadCeroNoHayFilas() {
        OfertaOperativa todos = new OfertaOperativa(Ofertas.TODOS_LOS_DIAS, List.of(OCHO));

        assertThat(MaterializacionOferta.filas(Ofertas.tour(), HOY)).isEmpty();
        assertThat(MaterializacionOferta.filas(Ofertas.restaurante(), HOY)).isEmpty();
        assertThat(MaterializacionOferta.filas(new OfertaReservable(Ofertas.RESTAURANTE, Ofertas.PRESTADOR, "Bar",
                "Gastronomia", BigDecimal.TEN, null, null, List.of(), todos), HOY)).isEmpty();
        assertThat(MaterializacionOferta.filas(Ofertas.tour(BigDecimal.TEN, 0, todos), HOY)).isEmpty();
        assertThat(MaterializacionOferta.filas(Ofertas.tour(BigDecimal.TEN, 12,
                new OfertaOperativa(Ofertas.TODOS_LOS_DIAS, List.of())), HOY)).as("un tour sin salidas").isEmpty();
        OfertaReservable sinHabitaciones = new OfertaReservable(Ofertas.HOTEL, Ofertas.PRESTADOR, "Hotel",
                "Alojamiento", BigDecimal.TEN, null, null,
                List.of(new UnidadReservable(Ofertas.DOBLE, "Doble", 0, 2, BigDecimal.TEN)), todos);
        assertThat(MaterializacionOferta.filas(sinHabitaciones, HOY)).isEmpty();
    }

    @Test
    void lasFilasVanEnUnOrdenTotalPorFechaUnidadYHora() {
        List<DiaInventario> dias = MaterializacionOferta.filas(Ofertas.hotelQueOpera(Ofertas.TODOS_LOS_DIAS), HOY)
                .stream().map(FilaInventarioNueva::dia).toList();

        assertThat(dias).isSortedAccordingTo(MaterializacionOferta.ORDEN_INSERCION);
        assertThat(dias.subList(0, 2)).extracting(DiaInventario::idUnidad).containsExactly(Ofertas.DOBLE, Ofertas.SUITE);
    }

    /** Lo que hacía {@code AsegurarInventarioDemoService.filas} (D5): 60 días, 08:00 y 14:00, cada unidad y noche. */
    private static List<FilaInventarioNueva> generadorD5(OfertaReservable oferta) {
        List<FilaInventarioNueva> filas = new ArrayList<>();
        for (int dia = 0; dia < 60; dia++) {
            LocalDate fecha = HOY.plusDays(dia);
            if (oferta.categoria().equals("Tour")) {
                for (LocalTime hora : List.of(OCHO, DOS)) {
                    filas.add(new FilaInventarioNueva(new DiaInventario(oferta.idServicio(), null, fecha, hora),
                            CategoriaReservable.Tour, oferta.aforoMaxGrupo()));
                }
            } else {
                for (UnidadReservable unidad : oferta.unidades()) {
                    filas.add(new FilaInventarioNueva(new DiaInventario(oferta.idServicio(), unidad.idUnidad(), fecha,
                            null), CategoriaReservable.Alojamiento, unidad.numHabitaciones()));
                }
            }
        }
        return filas;
    }
}
