package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.Eventos;
import co.edu.unimagdalena.samaria360.booking.application.fake.InventarioEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.HOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Disponibilidad pública y resumen de las tarjetas: límites (ajuste 7) y estado con varias unidades (D1). */
class DisponibilidadServiceTest {

    private final InventarioEnMemoria inventario = new InventarioEnMemoria(new Eventos());
    private final DisponibilidadService servicio = new DisponibilidadService(inventario, new RelojFijo(Ofertas.AHORA),
            HorarioReservas.POR_DEFECTO);

    @Test
    void elResumenDiceConCuposMientrasAlgunaUnidadTengaCupos() {
        DiaInventario doble = new DiaInventario(Ofertas.HOTEL, Ofertas.DOBLE, HOY.plusDays(1), null);
        DiaInventario suite = new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY.plusDays(1), null);
        inventario.con(doble, CategoriaReservable.Alojamiento, 12, 0).con(suite, CategoriaReservable.Alojamiento, 3, 1);

        assertThat(servicio.resumir(List.of(Ofertas.HOTEL, Ofertas.TOUR), null, null)).containsExactly(
                Map.entry(Ofertas.HOTEL, EstadoDisponibilidad.CON_CUPOS),
                Map.entry(Ofertas.TOUR, EstadoDisponibilidad.SIN_INVENTARIO));
    }

    @Test
    void elResumenAdmiteHastaDiezServiciosValidosSinRepetidos() {
        List<String> diez = IntStream.range(0, 10).mapToObj(i -> String.format("6d%022x", i)).toList();
        List<String> once = IntStream.range(0, 11).mapToObj(i -> String.format("6d%022x", i)).toList();

        assertThat(servicio.resumir(diez, null, null)).hasSize(10);
        assertThat(servicio.resumir(List.of(Ofertas.HOTEL, Ofertas.HOTEL), null, null)).hasSize(1);
        assertThat(campos(() -> servicio.resumir(once, null, null)))
                .containsEntry("servicios", MotivoInvalido.MAXIMO_ELEMENTOS);
        assertThat(campos(() -> servicio.resumir(List.of("hotel"), null, null)))
                .containsEntry("servicios", MotivoInvalido.FORMATO);
        assertThat(campos(() -> servicio.resumir(Collections.emptyList(), null, null)))
                .containsEntry("servicios", MotivoInvalido.REQUERIDO);
    }

    @Test
    void laConsultaExigeUnServicioValidoYUnRangoDeHasta60Dias() {
        assertThat(campos(() -> servicio.consultar(null, "abc", null, "2031-01-01"))).containsExactly(
                Map.entry("servicio", MotivoInvalido.REQUERIDO),
                Map.entry("unidad", MotivoInvalido.FORMATO),
                Map.entry("hasta", MotivoInvalido.MAXIMO_DIAS));
    }

    @Test
    void laConsultaLlevaElHorarioDeLosAlojamientosYElPlazoDeCancelacion() {
        var disponibilidad = servicio.consultar(Ofertas.HOTEL, null, null, null);

        assertThat(disponibilidad.horario()).isEqualTo(HorarioReservas.POR_DEFECTO);
        assertThat(disponibilidad.plazoCancelacion()).hasHours(24);
    }

    @Test
    void laConsultaFiltraPorUnidad() {
        inventario.con(new DiaInventario(Ofertas.HOTEL, Ofertas.DOBLE, HOY, null), CategoriaReservable.Alojamiento, 12, 12)
                .con(new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY, null), CategoriaReservable.Alojamiento, 3, 3);

        assertThat(servicio.consultar(Ofertas.HOTEL, Ofertas.SUITE, null, null).cupos())
                .singleElement().satisfies(cupo -> assertThat(cupo.idUnidad()).isEqualTo(Ofertas.SUITE));
        assertThat(servicio.consultar(Ofertas.HOTEL, null, null, null).cupos()).hasSize(2);
    }

    private static Map<String, MotivoInvalido> campos(Runnable consulta) {
        DatosReservaInvalidosException error = catchThrowableOfType(DatosReservaInvalidosException.class, consulta::run);
        assertThat(error).isNotNull();
        return error.getCampos();
    }
}
