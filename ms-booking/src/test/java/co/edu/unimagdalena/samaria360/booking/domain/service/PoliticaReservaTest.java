package co.edu.unimagdalena.samaria360.booking.domain.service;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.PrecioCambiadoException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.UnidadNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import co.edu.unimagdalena.samaria360.booking.domain.model.ReservaPlanificada;
import co.edu.unimagdalena.samaria360.booking.domain.model.SolicitudReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.AHORA;
import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.DOBLE;
import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.HOY;
import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.SUITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Reglas que dependen del catálogo (D1, D2, §5): fechas, unidad, capacidad, aforo y precio por unidad. */
class PoliticaReservaTest {

    private final PoliticaReserva politica = new PoliticaReserva(HorarioReservas.POR_DEFECTO);

    // ------------------------------------------------------------------ alojamiento

    @Test
    void alojamientoCobraElPrecioDeLaUnidadElegidaPorNocheYHabitacion() {
        ReservaPlanificada doble = planificar(Ofertas.alojamiento(DOBLE, HOY.plusDays(5), 3, 2, 4, "900000"));
        ReservaPlanificada suite = planificar(Ofertas.alojamiento(SUITE, HOY.plusDays(5), 3, 2, 8, "1560000"));

        assertThat(doble.precioUnitario()).isEqualByComparingTo("150000");
        assertThat(doble.montoTotal()).isEqualByComparingTo("900000");
        assertThat(doble.unidadCobro()).isEqualTo(UnidadCobro.Noche);
        assertThat(doble.nombreUnidad()).isEqualTo("Doble");
        assertThat(suite.precioUnitario()).isEqualByComparingTo("260000");
        assertThat(suite.nombreUnidad()).isEqualTo("Suite familiar");
        assertThat(suite.categoria()).isEqualTo(CategoriaReservable.Alojamiento);
    }

    @Test
    void lasNochesSonElRangoSemiabiertoDeLaUnidadEnOrden() {
        ReservaPlanificada plan = planificar(Ofertas.alojamiento(SUITE, HOY.plusDays(5), 3, 1, 2, "780000"));

        assertThat(plan.diasRequeridos()).containsExactly(
                new DiaInventario(Ofertas.HOTEL, SUITE, HOY.plusDays(5), null),
                new DiaInventario(Ofertas.HOTEL, SUITE, HOY.plusDays(6), null),
                new DiaInventario(Ofertas.HOTEL, SUITE, HOY.plusDays(7), null));
        assertThat(plan.fechaSalida()).isEqualTo(HOY.plusDays(8));
    }

    @Test
    void lasReglasDeAlojamientoSeRespondenALaVez() {
        SolicitudReserva todoMal = new SolicitudReserva(Ofertas.HOTEL, null, HOY.minusDays(1), HOY.minusDays(1),
                LocalTime.NOON, null, 2, BigDecimal.ONE);

        assertThat(campos(todoMal, Ofertas.hotel())).containsExactly(
                Map.entry("fechaEntrada", MotivoInvalido.FECHA_PASADA),
                Map.entry("fechaSalida", MotivoInvalido.RANGO_INVALIDO),
                Map.entry("horaInicio", MotivoInvalido.NO_PERMITIDO),
                Map.entry("idUnidad", MotivoInvalido.REQUERIDO),
                Map.entry("cantidad", MotivoInvalido.REQUERIDO));
        assertThat(campos(new SolicitudReserva(Ofertas.HOTEL, DOBLE, HOY.plusDays(1), null, null, 1, 1,
                BigDecimal.ONE), Ofertas.hotel())).containsExactly(Map.entry("fechaSalida", MotivoInvalido.REQUERIDO));
    }

    @Test
    void alojamientoAdmiteHasta14Noches() {
        assertThat(planificar(Ofertas.alojamiento(DOBLE, HOY.plusDays(1), 14, 1, 1, "2100000")).diasRequeridos())
                .hasSize(14);
        assertThat(campos(Ofertas.alojamiento(DOBLE, HOY.plusDays(1), 15, 1, 1, "2250000"), Ofertas.hotel()))
                .containsExactly(Map.entry("fechaSalida", MotivoInvalido.MAXIMO_NOCHES));
    }

    @Test
    void conEntradaHoySePuedeReservarHastaLaHoraDelCheckIn() {
        SolicitudReserva hoy = Ofertas.alojamiento(DOBLE, HOY, 1, 1, 1, "150000");
        Instant checkIn = HorarioReservas.instante(HOY, LocalTime.of(15, 0));

        assertThat(politica.planificar(hoy, Ofertas.hotel(), checkIn).fechaEntrada()).isEqualTo(HOY);
        assertThat(catchThrowableOfType(DatosReservaInvalidosException.class,
                () -> politica.planificar(hoy, Ofertas.hotel(), checkIn.plusSeconds(1))).getCampos())
                .containsExactly(Map.entry("fechaEntrada", MotivoInvalido.FECHA_PASADA));
    }

    @Test
    void unaUnidadQueElAlojamientoNoTieneEsUnidadNoDisponible() {
        SolicitudReserva otra = Ofertas.alojamiento("6b00000000000000000000ff", HOY.plusDays(2), 1, 1, 1, "150000");

        assertThatThrownBy(() -> planificar(otra)).isInstanceOf(UnidadNoDisponibleException.class)
                .hasMessageContaining("6b00000000000000000000ff");
    }

    @Test
    void laCapacidadEsLaDeLaUnidadElegida() {
        // Doble: 2 huéspedes por habitación y 12 habitaciones; Suite: 4 por habitación y 3 habitaciones.
        assertThat(planificar(Ofertas.alojamiento(SUITE, HOY.plusDays(2), 1, 1, 4, "260000")).personas()).isEqualTo(4);
        assertThat(campos(Ofertas.alojamiento(DOBLE, HOY.plusDays(2), 1, 1, 3, "150000"), Ofertas.hotel()))
                .containsExactly(Map.entry("personas", MotivoInvalido.EXCEDE_CAPACIDAD));
        assertThat(campos(Ofertas.alojamiento(SUITE, HOY.plusDays(2), 1, 4, 4, "1040000"), Ofertas.hotel()))
                .containsExactly(Map.entry("cantidad", MotivoInvalido.EXCEDE_CAPACIDAD));
    }

    // ------------------------------------------------------------------ tour

    @Test
    void tourCobraPorPersonaYTerminaSegunSuDuracion() {
        ReservaPlanificada plan = planificar(Ofertas.tour(HOY.plusDays(1), LocalTime.of(8, 0), 3, "240000"));

        assertThat(plan.montoTotal()).isEqualByComparingTo("240000");
        assertThat(plan.unidadCobro()).isEqualTo(UnidadCobro.Persona);
        assertThat(plan.cantidad()).isEqualTo(3);
        assertThat(plan.idUnidad()).isNull();
        assertThat(plan.diasRequeridos()).containsExactly(
                new DiaInventario(Ofertas.TOUR, null, HOY.plusDays(1), LocalTime.of(8, 0)));
        assertThat(Duration.between(plan.fechaInicio(), plan.fechaFin())).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void lasReglasDeTourSeRespondenALaVez() {
        SolicitudReserva todoMal = new SolicitudReserva(Ofertas.TOUR, DOBLE, HOY.plusDays(1), HOY.plusDays(2), null,
                2, 13, BigDecimal.ONE);

        assertThat(campos(todoMal, Ofertas.tour())).containsExactly(
                Map.entry("horaInicio", MotivoInvalido.REQUERIDO),
                Map.entry("fechaSalida", MotivoInvalido.NO_PERMITIDO),
                Map.entry("idUnidad", MotivoInvalido.NO_PERMITIDO),
                Map.entry("cantidad", MotivoInvalido.NO_PERMITIDO),
                Map.entry("personas", MotivoInvalido.EXCEDE_AFORO));
    }

    @Test
    void unHorarioDeHoyYaPasadoNoSeReserva() {
        assertThat(campos(Ofertas.tour(HOY, LocalTime.of(12, 0), 1, "80000"), Ofertas.tour()))
                .containsExactly(Map.entry("fechaEntrada", MotivoInvalido.FECHA_PASADA));
        assertThat(planificar(Ofertas.tour(HOY, LocalTime.of(14, 0), 1, "80000")).fechaEntrada()).isEqualTo(HOY);
    }

    @Test
    void unTourSinPrecioNoEsReservable() {
        OfertaReservable sinPrecio = new OfertaReservable(Ofertas.TOUR, Ofertas.PRESTADOR, "Tour", "Tour", null, 10,
                BigDecimal.ONE, List.of());

        assertThatThrownBy(() -> politica.planificar(Ofertas.tour(HOY.plusDays(1), LocalTime.NOON, 1, "1"), sinPrecio,
                AHORA)).isInstanceOf(OfertaNoDisponibleException.class);
    }

    // ------------------------------------------------------------------ comunes

    @Test
    void gastronomiaNoEsReservable() {
        assertThat(campos(Ofertas.tour(HOY.plusDays(1), LocalTime.NOON, 1, "50000"), Ofertas.restaurante()))
                .containsExactly(Map.entry("idServicio", MotivoInvalido.CATEGORIA_NO_RESERVABLE));
    }

    @Test
    void rf37_siElPrecioEsperadoNoCoincideSeRespondeElVigente() {
        PrecioCambiadoException error = catchThrowableOfType(PrecioCambiadoException.class, () -> politica.planificar(
                Ofertas.alojamiento(SUITE, HOY.plusDays(2), 2, 1, 2, "300000"),
                Ofertas.hotel(new BigDecimal("150000"), new BigDecimal("280000")), AHORA));

        assertThat(error.getPrecioTotal()).isEqualByComparingTo("560000");
        assertThat(error.getPrecioUnitario()).isEqualByComparingTo("280000");
        // Otro formato del mismo número no es un cambio de precio.
        assertThat(planificar(Ofertas.alojamiento(DOBLE, HOY.plusDays(2), 1, 1, 1, "150000.00")).montoTotal())
                .isEqualByComparingTo("150000");
    }

    @Test
    void losErroresDeFormatoVanAntesQueLaUnidadYEstaAntesQueElPrecio() {
        SolicitudReserva sinSalidaYOtraUnidad = new SolicitudReserva(Ofertas.HOTEL, "6b00000000000000000000ff",
                HOY.plusDays(1), null, null, 1, 1, BigDecimal.ONE);
        SolicitudReserva otraUnidadYPrecioMalo = Ofertas.alojamiento("6b00000000000000000000ff", HOY.plusDays(1), 1,
                1, 1, "1");

        assertThatThrownBy(() -> planificar(sinSalidaYOtraUnidad)).isInstanceOf(DatosReservaInvalidosException.class);
        assertThatThrownBy(() -> planificar(otraUnidadYPrecioMalo)).isInstanceOf(UnidadNoDisponibleException.class);
    }

    private ReservaPlanificada planificar(SolicitudReserva solicitud) {
        OfertaReservable oferta = solicitud.idServicio().equals(Ofertas.TOUR) ? Ofertas.tour() : Ofertas.hotel();
        return politica.planificar(solicitud, oferta, AHORA);
    }

    private Map<String, MotivoInvalido> campos(SolicitudReserva solicitud, OfertaReservable oferta) {
        DatosReservaInvalidosException error = catchThrowableOfType(DatosReservaInvalidosException.class,
                () -> politica.planificar(solicitud, oferta, AHORA));
        assertThat(error).as("se esperaba DATOS_INVALIDOS").isNotNull();
        return error.getCampos();
    }
}
