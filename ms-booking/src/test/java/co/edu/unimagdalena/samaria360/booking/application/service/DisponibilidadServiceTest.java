package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.CatalogoFalso;
import co.edu.unimagdalena.samaria360.booking.application.fake.Eventos;
import co.edu.unimagdalena.samaria360.booking.application.fake.InventarioEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.OfertaMaterializadaEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.HOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Disponibilidad pública y resumen de las tarjetas: límites (ajuste 7), estado con varias unidades (D1) y
 * materialización bajo demanda (DEC-152, DEC-155).
 */
class DisponibilidadServiceTest {

    private final Eventos eventos = new Eventos();
    private final CatalogoFalso catalogo = new CatalogoFalso(eventos);
    private final InventarioEnMemoria inventario = new InventarioEnMemoria(eventos);
    private final OfertaMaterializadaEnMemoria marcas = new OfertaMaterializadaEnMemoria(eventos);
    private final RelojFijo reloj = new RelojFijo(Ofertas.AHORA);
    private final DisponibilidadService servicio = new DisponibilidadService(inventario,
            new MaterializarInventarioService(catalogo, inventario, marcas, reloj), reloj, HorarioReservas.POR_DEFECTO);

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

    // ------------------------------------------------------------------ materialización (DEC-152, DEC-155)

    @Test
    void losIdsQueNoSonObjectIdSeRechazanSinLlamarAlCatalogo() {
        assertThat(campos(() -> servicio.consultar("no-es-un-id", null, null, null)))
                .containsEntry("servicio", MotivoInvalido.FORMATO);
        assertThat(campos(() -> servicio.resumir(List.of(Ofertas.HOTEL, "6A00000000000000000000ZZ"), null, null)))
                .containsEntry("servicios", MotivoInvalido.FORMATO);
        assertThat(catalogo.consultasDetalle()).isZero();
        assertThat(eventos.todos()).doesNotContain("catalogo.detalle", "marcas.consultar");
    }

    @Test
    void laConsultaMaterializaLaOfertaAntesDeLeer() {
        catalogo.con(Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, LocalTime.of(8, 0), LocalTime.of(14, 0)));

        var disponibilidad = servicio.consultar(Ofertas.TOUR, null, null, HOY.plusDays(6).toString());

        assertThat(disponibilidad.cupos()).hasSize(7 * 2).allSatisfy(cupo -> {
            assertThat(cupo.cuposTotales()).isEqualTo(12);
            assertThat(cupo.cuposDisponibles()).isEqualTo(12);
        });
        assertThat(eventos.todos()).containsSubsequence("marcas.consultar", "catalogo.detalle",
                "inventario.asegurar", "marcas.registrar");
    }

    @Test
    void conElRangoCubiertoNoSeLlamaAlCatalogoAunqueEsteCaido() {
        marcas.con(Ofertas.HOTEL, HOY.plusDays(59));
        inventario.con(new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY, null), CategoriaReservable.Alojamiento, 3, 2);
        catalogo.caer();

        assertThat(servicio.consultar(Ofertas.HOTEL, null, null, null).cupos()).hasSize(1);
        assertThat(servicio.resumir(List.of(Ofertas.HOTEL), null, null))
                .containsEntry(Ofertas.HOTEL, EstadoDisponibilidad.CON_CUPOS);
        assertThat(eventos.todos()).doesNotContain("catalogo.detalle");
    }

    @Test
    void conElCatalogoCaidoYElRangoSinCubrirNoSeRespondeSinFechas() {
        catalogo.caer();

        assertThatThrownBy(() -> servicio.consultar(Ofertas.HOTEL, null, null, null))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThatThrownBy(() -> servicio.resumir(List.of(Ofertas.HOTEL, Ofertas.TOUR), null, null))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(eventos.todos().stream().filter("catalogo.detalle"::equals))
                .as("el resumen corta en el primer fallo").hasSize(2);
    }

    @Test
    void elResumenMaterializaLosServiciosNuevosParaLasTarjetas() {
        catalogo.con(Ofertas.hotelQueOpera(Ofertas.TODOS_LOS_DIAS));

        assertThat(servicio.resumir(List.of(Ofertas.HOTEL, Ofertas.TOUR), null, null)).containsExactly(
                Map.entry(Ofertas.HOTEL, EstadoDisponibilidad.CON_CUPOS),
                Map.entry(Ofertas.TOUR, EstadoDisponibilidad.SIN_INVENTARIO));
        assertThat(marcas.marca(Ofertas.HOTEL)).isEqualTo(HOY.plusDays(59));
        assertThat(marcas.marca(Ofertas.TOUR)).as("un 404 no avanza la marca").isNull();
    }

    private static Map<String, MotivoInvalido> campos(Runnable consulta) {
        DatosReservaInvalidosException error = catchThrowableOfType(DatosReservaInvalidosException.class, consulta::run);
        assertThat(error).isNotNull();
        return error.getCampos();
    }
}
