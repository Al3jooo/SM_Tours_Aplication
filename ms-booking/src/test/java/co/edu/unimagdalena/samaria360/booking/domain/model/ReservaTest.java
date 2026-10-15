package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.CancelacionFueraDePlazoException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoCancelableException;
import co.edu.unimagdalena.samaria360.booking.domain.service.PoliticaReserva;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RF20 (confirmación con datos congelados) y RF44 (cancelación hasta 24 h antes, límite inclusivo). */
class ReservaTest {

    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private final PoliticaReserva politica = new PoliticaReserva(HorarioReservas.POR_DEFECTO);

    @Test
    void confirmarCongelaElPrecioYLosNombresYGeneraElId() {
        Reserva reserva = suite(LocalDate.of(2030, 3, 20));

        assertThat(reserva.idReserva()).isNotNull();
        assertThat(reserva.estado()).isEqualTo(EstadoReserva.Confirmada);
        assertThat(reserva.nombreServicio()).isEqualTo("Hotel Casa Tayrona");
        assertThat(reserva.idUnidad()).isEqualTo(Ofertas.SUITE);
        assertThat(reserva.nombreUnidad()).isEqualTo("Suite familiar");
        assertThat(reserva.precioUnitario()).isEqualByComparingTo("260000");
        assertThat(reserva.montoTotal()).isEqualByComparingTo("520000");
        assertThat(reserva.moneda()).isEqualTo("COP");
        assertThat(reserva.fechaCreacion()).isEqualTo(Ofertas.AHORA);
        assertThat(suite(LocalDate.of(2030, 3, 20)).idReserva()).isNotEqualTo(reserva.idReserva());
    }

    @Test
    void rf44_conExactamente24HorasSePuedeCancelarYUnSegundoDespuesNo() {
        Reserva reserva = suite(LocalDate.of(2030, 3, 20));
        assertThat(reserva.cancelableHasta()).isEqualTo(reserva.fechaInicio().minus(Duration.ofHours(24)));

        assertThatCode(() -> reserva.verificarCancelable(reserva.cancelableHasta())).doesNotThrowAnyException();
        assertThatThrownBy(() -> reserva.verificarCancelable(reserva.cancelableHasta().plusSeconds(1)))
                .isInstanceOf(CancelacionFueraDePlazoException.class)
                .hasMessageContaining("tiempo límite");
    }

    @Test
    void enUnAlojamientoLaFechaProgramadaEsElCheckInDeLaEntrada() {
        Reserva reserva = suite(LocalDate.of(2030, 3, 20));

        assertThat(reserva.fechaInicio()).isEqualTo(HorarioReservas.instante(LocalDate.of(2030, 3, 20),
                HorarioReservas.POR_DEFECTO.checkIn()));
        assertThat(reserva.fechaFin()).isEqualTo(HorarioReservas.instante(LocalDate.of(2030, 3, 22),
                HorarioReservas.POR_DEFECTO.checkOut()));
    }

    @Test
    void unaReservaCompletadaNoSeCancela() {
        Reserva completada = new Reserva(UUID.randomUUID(), TURISTA, Ofertas.PRESTADOR, Ofertas.HOTEL, "Hotel",
                CategoriaReservable.Alojamiento, Ofertas.DOBLE, "Doble", EstadoReserva.Completada, 1, 2,
                java.math.BigDecimal.TEN, UnidadCobro.Noche, java.math.BigDecimal.TEN, "COP", Ofertas.HOY,
                Ofertas.HOY.plusDays(1), null, Ofertas.AHORA.plus(Duration.ofDays(30)),
                Ofertas.AHORA.plus(Duration.ofDays(31)), Ofertas.AHORA, null, UUID.randomUUID(), "h");

        assertThatThrownBy(() -> completada.verificarCancelable(Ofertas.AHORA))
                .isInstanceOf(ReservaNoCancelableException.class);
    }

    @Test
    void cancelarDevuelveOtraReservaConLaFechaDeCancelacion() {
        Reserva reserva = suite(LocalDate.of(2030, 3, 20));

        Reserva cancelada = reserva.cancelada(Ofertas.AHORA);

        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.Cancelada);
        assertThat(cancelada.fechaCancelacion()).isEqualTo(Ofertas.AHORA);
        assertThat(reserva.estado()).isEqualTo(EstadoReserva.Confirmada);
        assertThat(cancelada.idReserva()).isEqualTo(reserva.idReserva());
    }

    private Reserva suite(LocalDate entrada) {
        ReservaPlanificada plan = politica.planificar(
                Ofertas.alojamiento(Ofertas.SUITE, entrada, 2, 1, 3, "520000"), Ofertas.hotel(), Ofertas.AHORA);
        return Reserva.confirmar(plan, TURISTA, UUID.randomUUID(), "hash", Ofertas.AHORA);
    }
}
