package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CancelarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ComandoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.in.CompletarReservasUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.domain.exception.ReservaNoCancelableException;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.InventarioDePrueba;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF52 contra PostgreSQL. La tarea completa <b>todas</b> las reservas Confirmadas que ya terminaron, así que, para no
 * tocar datos de otras pruebas y poder exigir conteos exactos, esta clase vacía las tablas de reservas antes de cada
 * prueba. Es la única que ejecuta RF52: en las demás el programador está apagado ({@link PruebaIntegracion}) y ninguna
 * llama al caso de uso. Ninguna otra prueba usa reservas creadas fuera de su propio método.
 */
@PruebaIntegracion
class CompletarReservasIntegracionTest {

    private static final LocalDate ENTRADA = RelojAjustable.HOY.plusDays(30);
    private static final LocalTime OCHO = LocalTime.of(8, 0);

    @Autowired
    private ReservarServicioUseCase reservar;
    @Autowired
    private CancelarReservaUseCase cancelar;
    @Autowired
    private CompletarReservasUseCase completar;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private InventarioDePrueba inventario;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void soloLasReservasDeEstaPrueba() {
        reloj.reiniciar();
        jdbc.update("DELETE FROM reserva_cupo_dia");
        jdbc.update("DELETE FROM reserva");
    }

    @Test
    void rf52_completaSoloLasConfirmadasQueTerminaronYEsIdempotente() {
        String tour = catalogo.publicarTour("Tour RF52", 12, "80000");
        LocalDate dia = ENTRADA.plusDays(60);
        inventario.horario(tour, dia, OCHO, 12, 12);
        inventario.horario(tour, dia, LocalTime.of(14, 0), 12, 12);
        Reserva manana = reservar.reservar(tourEn(tour, dia, "08:00")).reserva();
        Reserva tarde = reservar.reservar(tourEn(tour, dia, "14:00")).reserva();
        Reserva cancelada = reservar.reservar(tourEn(tour, dia, "08:00")).reserva();
        cancelar.cancelar(cancelada.idReserva(), cancelada.idTurista());

        reloj.fijar(manana.fechaFin());
        int primera = completar.completar();
        int segunda = completar.completar();

        // La base solo tiene las reservas de esta prueba (@BeforeEach): el conteo es exacto.
        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isZero();
        assertThat(estado(manana)).isEqualTo("Completada");
        assertThat(estado(tarde)).isEqualTo("Confirmada");
        assertThat(estado(cancelada)).isEqualTo("Cancelada");
        assertThat(inventario.horario(tour, dia, OCHO)).isEqualTo(11);
    }

    /**
     * Cancelar contra RF52 sobre la misma reserva, al mismo tiempo: el hilo que cancela ve «a tiempo» (24 h antes) y
     * el de RF52 ve «ya terminó». Gana uno y no queda un estado incoherente: si gana la cancelación, los cupos vuelven
     * una vez; si gana Completada, la cancelación responde RESERVA_NO_CANCELABLE y los cupos no cambian.
     */
    @Test
    void cancelarYCompletarAlMismoTiempoDejanUnSoloEstadoCoherente() throws Exception {
        String tour = catalogo.publicarTour("Tour carrera", 50, "80000");
        LocalDate dia = ENTRADA.plusDays(70);
        inventario.horario(tour, dia, OCHO, 50, 50);
        int canceladas = 0;
        int completadas = 0;
        for (int ronda = 0; ronda < 12; ronda++) {
            Reserva reserva = reservar.reservar(tourEn(tour, dia, "08:00")).reserva();
            int antes = inventario.horario(tour, dia, OCHO);

            List<Object> resultados = ConcurrenciaIntegracionTest.aLaVez(2, i -> () -> {
                try {
                    if (i == 0) {
                        reloj.fijarEnEsteHilo(reserva.cancelableHasta());
                        return cancelar.cancelar(reserva.idReserva(), reserva.idTurista());
                    }
                    reloj.fijarEnEsteHilo(reserva.fechaFin().plusSeconds(60));
                    return completar.completar();
                } finally {
                    reloj.soltarEsteHilo();
                }
            });

            String final_ = estado(reserva);
            if (final_.equals("Cancelada")) {
                canceladas++;
                assertThat(resultados.get(0)).isInstanceOf(Reserva.class);
                assertThat(resultados.get(1)).as("ronda %d", ronda).isEqualTo(0);
                assertThat(inventario.horario(tour, dia, OCHO)).as("ronda %d", ronda).isEqualTo(antes + 1);
            } else {
                completadas++;
                assertThat(final_).isEqualTo("Completada");
                assertThat(resultados.get(0)).as("ronda %d", ronda).isInstanceOf(ReservaNoCancelableException.class);
                assertThat(resultados.get(1)).as("ronda %d", ronda).isEqualTo(1);
                assertThat(inventario.horario(tour, dia, OCHO)).as("ronda %d", ronda).isEqualTo(antes);
            }
        }
        assertThat(canceladas + completadas).isEqualTo(12);
    }

    private String estado(Reserva reserva) {
        return jdbc.queryForObject("SELECT estado FROM reserva WHERE id_reserva = ?", String.class, reserva.idReserva());
    }

    private ComandoReserva tourEn(String tour, LocalDate dia, String hora) {
        return new ComandoReserva(UUID.randomUUID(), UUID.randomUUID(), tour, null, dia.toString(), null, hora, null,
                1, new BigDecimal("80000"));
    }
}
