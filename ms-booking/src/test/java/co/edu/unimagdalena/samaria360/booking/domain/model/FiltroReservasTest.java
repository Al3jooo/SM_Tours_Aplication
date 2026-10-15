package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** RF12 del lado del turista: la actividad cruza el rango [desde, hasta] de Santa Marta, y los estados. */
class FiltroReservasTest {

    private static final FiltroReservas DICIEMBRE = new FiltroReservas(LocalDate.of(2030, 12, 1),
            LocalDate.of(2030, 12, 31), Set.of());

    @Test
    void unaEstadiaQueEmpiezaAntesYTerminaDentroAparece() {
        assertThat(DICIEMBRE.incluye(reserva(en(2030, 11, 30, 15, 0), en(2030, 12, 2, 12, 0)))).isTrue();
    }

    @Test
    void unaEstadiaQueEmpiezaDentroYTerminaDespuesAparece() {
        assertThat(DICIEMBRE.incluye(reserva(en(2030, 12, 30, 15, 0), en(2031, 1, 2, 12, 0)))).isTrue();
    }

    @Test
    void unTourElDiaHastaApareceYElDiaSiguienteNo() {
        assertThat(DICIEMBRE.incluye(reserva(en(2030, 12, 31, 20, 0), en(2031, 1, 1, 0, 0)))).isTrue();
        assertThat(DICIEMBRE.incluye(reserva(en(2031, 1, 1, 0, 0), en(2031, 1, 1, 4, 0)))).isFalse();
    }

    @Test
    void unaReservaQueTerminaExactamenteALasCeroHorasDeDesdeNoAparece() {
        assertThat(DICIEMBRE.incluye(reserva(en(2030, 11, 30, 20, 0), en(2030, 12, 1, 0, 0)))).isFalse();
        assertThat(DICIEMBRE.incluye(reserva(en(2030, 11, 30, 20, 0), en(2030, 12, 1, 0, 1)))).isTrue();
    }

    @Test
    void conUnSoloExtremoElRangoQuedaAbierto() {
        FiltroReservas desde = validar("2030-12-01", null, null);
        FiltroReservas hasta = validar(null, "2030-12-31", null);

        assertThat(desde.empiezaAntesDe()).isNull();
        assertThat(desde.incluye(reserva(en(2035, 1, 1, 8, 0), en(2035, 1, 1, 12, 0)))).isTrue();
        assertThat(hasta.terminaDespuesDe()).isNull();
        assertThat(hasta.incluye(reserva(en(2020, 1, 1, 8, 0), en(2020, 1, 1, 12, 0)))).isTrue();
    }

    @Test
    void losEstadosVanSeparadosPorComa() {
        FiltroReservas filtro = validar(null, null, "Cancelada, Completada");

        assertThat(filtro.estados()).containsExactlyInAnyOrder(EstadoReserva.Cancelada, EstadoReserva.Completada);
        assertThat(filtro.incluye(reserva(en(2030, 12, 5, 8, 0), en(2030, 12, 5, 12, 0)))).isFalse();
    }

    @Test
    void validaElRangoElMaximoDe366DiasYLosEstados() {
        assertThat(validar("2030-01-01", "2031-01-01", null).hasta()).isEqualTo(LocalDate.of(2031, 1, 1));
        assertThat(campos("2030-01-01", "2031-01-02", null)).containsExactly(Map.entry("hasta", MotivoInvalido.MAXIMO_DIAS));
        assertThat(campos("2030-12-31", "2030-12-01", null))
                .containsExactly(Map.entry("hasta", MotivoInvalido.RANGO_INVALIDO));
        assertThat(campos("1/12/2030", null, "Pendiente")).containsExactly(
                Map.entry("desde", MotivoInvalido.FORMATO), Map.entry("estado", MotivoInvalido.NO_PERMITIDO));
    }

    private static FiltroReservas validar(String desde, String hasta, String estados) {
        ValidadorCampos validador = new ValidadorCampos();
        FiltroReservas filtro = FiltroReservas.validar(desde, hasta, estados, validador);
        validador.lanzarSiHayErrores();
        return filtro;
    }

    private static Map<String, MotivoInvalido> campos(String desde, String hasta, String estados) {
        DatosReservaInvalidosException error = catchThrowableOfType(DatosReservaInvalidosException.class,
                () -> validar(desde, hasta, estados));
        assertThat(error).isNotNull();
        return error.getCampos();
    }

    private static Instant en(int anio, int mes, int dia, int hora, int minuto) {
        return HorarioReservas.instante(LocalDate.of(anio, mes, dia), LocalTime.of(hora, minuto));
    }

    private static Reserva reserva(Instant inicio, Instant fin) {
        return new Reserva(UUID.randomUUID(), UUID.randomUUID(), Ofertas.PRESTADOR, Ofertas.TOUR, "Tour",
                CategoriaReservable.Tour, null, null, EstadoReserva.Confirmada, 1, 1, BigDecimal.TEN,
                UnidadCobro.Persona, BigDecimal.TEN, "COP", inicio.atZone(HorarioReservas.ZONA).toLocalDate(), null,
                inicio.atZone(HorarioReservas.ZONA).toLocalTime(), inicio, fin, Ofertas.AHORA, null, UUID.randomUUID(),
                "h");
    }
}
