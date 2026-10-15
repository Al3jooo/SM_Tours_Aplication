package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.CancelarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ComandoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CupoAgotadoException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.FechaNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.InventarioDePrueba;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concurrencia real contra PostgreSQL (D3, D6): UPDATE condicional + CHECK, un día por sentencia y en orden de fecha.
 * Los hilos arrancan a la vez con un {@link CountDownLatch}.
 */
@PruebaIntegracion
class ConcurrenciaIntegracionTest {

    private static final LocalDate ENTRADA = RelojAjustable.HOY.plusDays(30);
    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final int HILOS = 10;

    @Autowired
    private ReservarServicioUseCase reservar;
    @Autowired
    private CancelarReservaUseCase cancelar;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private InventarioDePrueba inventario;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transacciones;

    private String hotel;
    private String doble;
    private String suite;

    @BeforeEach
    void publicarHotel() {
        reloj.reiniciar();
        doble = CatalogoSimulado.nuevoId();
        suite = CatalogoSimulado.nuevoId();
        hotel = catalogo.publicarHotel("Hotel Casa Tayrona", new Unidad(doble, "Doble", 12, 2, "150000"),
                new Unidad(suite, "Suite familiar", 3, 4, "260000"));
    }

    @Test
    void variosHilosPorElUltimoCupoSoloUnoConfirma() throws Exception {
        String tour = catalogo.publicarTour("Tour", 12, "80000");
        inventario.horario(tour, ENTRADA, OCHO, 12, 1);

        List<Object> resultados = aLaVez(HILOS, i -> () -> reservar.reservar(tour(tour, 1)));

        assertThat(resultados).filteredOn(ResultadoReserva.class::isInstance).hasSize(1);
        assertThat(resultados).filteredOn(CupoAgotadoException.class::isInstance).hasSize(HILOS - 1);
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isZero();
        assertThat(inventario.reservasDe(tour)).isEqualTo(1);
    }

    @Test
    void d1_elUltimoCupoDeCadaUnidadSeDisputaPorSeparado() throws Exception {
        inventario.noches(hotel, doble, ENTRADA, 1, 12, 1);
        inventario.noches(hotel, suite, ENTRADA, 1, 3, 1);

        List<Object> resultados = aLaVez(2 * HILOS, i -> () -> reservar.reservar(
                i % 2 == 0 ? alojamiento(doble, 1, 1, "150000") : alojamiento(suite, 1, 1, "260000")));

        List<Reserva> confirmadas = resultados.stream().filter(ResultadoReserva.class::isInstance)
                .map(r -> ((ResultadoReserva) r).reserva()).toList();
        assertThat(confirmadas).extracting(Reserva::idUnidad).containsExactlyInAnyOrder(doble, suite);
        assertThat(resultados).filteredOn(CupoAgotadoException.class::isInstance).hasSize(2 * HILOS - 2);
        assertThat(inventario.noche(hotel, doble, ENTRADA)).isZero();
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isZero();
    }

    @Test
    void d1_agotarUnaUnidadNoAfectaALaOtra() {
        inventario.noches(hotel, doble, ENTRADA, 2, 12, 12);
        inventario.noches(hotel, suite, ENTRADA, 2, 3, 3);

        reservar.reservar(alojamiento(suite, 2, 3, "1560000"));

        assertThatThrownBy(() -> reservar.reservar(alojamiento(suite, 1, 1, "260000")))
                .isInstanceOf(CupoAgotadoException.class);
        assertThat(reservar.reservar(alojamiento(doble, 2, 12, "3600000")).reserva().idUnidad()).isEqualTo(doble);
        assertThat(inventario.noche(hotel, doble, ENTRADA.plusDays(1))).isZero();
    }

    @Test
    void d1_loDescontadoApuntaSoloAFilasDeLaUnidadYCancelarSoloLiberaEsa() {
        inventario.noches(hotel, doble, ENTRADA, 3, 12, 12);
        inventario.noches(hotel, suite, ENTRADA, 3, 3, 3);
        Reserva reserva = reservar.reservar(alojamiento(suite, 3, 2, "1560000")).reserva();

        List<String> unidades = jdbc.queryForList("SELECT i.id_unidad FROM reserva_cupo_dia d JOIN inventario_cupo i"
                + " ON i.id_inventario = d.id_inventario WHERE d.id_reserva = ?", String.class, reserva.idReserva());
        cancelar.cancelar(reserva.idReserva(), reserva.idTurista());

        assertThat(unidades).hasSize(3).containsOnly(suite);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(2))).isEqualTo(3);
        assertThat(inventario.noche(hotel, doble, ENTRADA.plusDays(2))).isEqualTo(12);
    }

    @Test
    void rangosQueSeCruzanNoSeBloqueanEntreSiNiDescuentanAMedias() throws Exception {
        for (int ronda = 0; ronda < 5; ronda++) {
            LocalDate entrada = ENTRADA.plusDays(ronda * 5L);
            inventario.noches(hotel, suite, entrada, 4, 3, 1);
            ComandoReserva unoATres = alojamientoDesde(entrada, suite, 3, 1, "780000");
            ComandoReserva dosACuatro = alojamientoDesde(entrada.plusDays(1), suite, 3, 1, "780000");

            List<Object> resultados = aLaVez(2, i -> () -> reservar.reservar(i == 0 ? unoATres : dosACuatro));

            assertThat(resultados).filteredOn(ResultadoReserva.class::isInstance).hasSize(1);
            assertThat(resultados).filteredOn(CupoAgotadoException.class::isInstance).hasSize(1);
            int descontadas = 0;
            for (int noche = 0; noche < 4; noche++) {
                descontadas += 1 - inventario.noche(hotel, suite, entrada.plusDays(noche));
            }
            assertThat(descontadas).as("ronda %d", ronda).isEqualTo(3);
        }
    }

    @Test
    void variasNochesSonAtomicas() {
        inventario.noches(hotel, suite, ENTRADA, 1, 3, 3);
        inventario.noches(hotel, suite, ENTRADA.plusDays(1), 1, 3, 0);
        inventario.noches(hotel, suite, ENTRADA.plusDays(2), 1, 3, 3);

        assertThatThrownBy(() -> reservar.reservar(alojamiento(suite, 3, 1, "780000")))
                .isInstanceOf(CupoAgotadoException.class);
        assertThatThrownBy(() -> reservar.reservar(alojamiento(suite, 4, 1, "1040000")))
                .isInstanceOf(FechaNoDisponibleException.class);
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isEqualTo(3);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(2))).isEqualTo(3);
        assertThat(inventario.reservasDe(hotel)).isZero();
    }

    @Test
    void cancelarYVolverAReservarElMismoCupo() {
        inventario.noches(hotel, suite, ENTRADA, 1, 3, 1);
        Reserva primera = reservar.reservar(alojamiento(suite, 1, 1, "260000")).reserva();

        cancelar.cancelar(primera.idReserva(), primera.idTurista());

        assertThat(reservar.reservar(alojamiento(suite, 1, 1, "260000")).reserva().estado())
                .isEqualTo(EstadoReserva.Confirmada);
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isZero();
    }

    @Test
    void d6_variosHilosConLaMismaClaveCreanUnaSolaReserva() throws Exception {
        inventario.noches(hotel, doble, ENTRADA, 1, 12, 12);
        ComandoReserva mismo = alojamiento(doble, 1, 1, "150000");

        List<Object> resultados = aLaVez(HILOS, i -> () -> reservar.reservar(mismo));

        assertThat(resultados).allMatch(ResultadoReserva.class::isInstance);
        assertThat(resultados).map(r -> ((ResultadoReserva) r).reserva().idReserva()).containsOnly(
                ((ResultadoReserva) resultados.get(0)).reserva().idReserva());
        assertThat(resultados).filteredOn(r -> !((ResultadoReserva) r).repetida()).hasSize(1);
        assertThat(inventario.noche(hotel, doble, ENTRADA)).isEqualTo(11);
        assertThat(inventario.reservasDe(hotel)).isEqualTo(1);
    }

    @Test
    void dosCancelacionesSimultaneasLiberanUnaSolaVez() throws Exception {
        inventario.noches(hotel, suite, ENTRADA, 2, 3, 3);
        Reserva reserva = reservar.reservar(alojamiento(suite, 2, 2, "1040000")).reserva();

        List<Object> resultados = aLaVez(HILOS, i -> () -> cancelar.cancelar(reserva.idReserva(), reserva.idTurista()));

        assertThat(resultados).allSatisfy(r -> assertThat(((Reserva) r).estado()).isEqualTo(EstadoReserva.Cancelada));
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isEqualTo(3);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(1))).isEqualTo(3);
    }

    @Test
    void conElCatalogoLentoElInventarioNoQuedaBloqueado() throws Exception {
        inventario.noches(hotel, suite, ENTRADA, 1, 3, 3);
        catalogo.demorar(hotel, Duration.ofMillis(1000));
        ExecutorService hilo = Executors.newSingleThreadExecutor();
        try {
            Future<ResultadoReserva> enCurso = hilo.submit(() -> reservar.reservar(alojamiento(suite, 1, 1, "260000")));
            Thread.sleep(300); // la reserva está esperando al catálogo

            Integer actualizadas = new TransactionTemplate(transacciones).execute(estado -> {
                jdbc.execute("SET LOCAL lock_timeout = '200ms'");
                return jdbc.update("UPDATE inventario_cupo SET cupos_disponibles = cupos_disponibles"
                        + " WHERE id_servicio_mongo = ? AND id_unidad = ?", hotel, suite);
            });

            assertThat(actualizadas).isEqualTo(1);
            assertThat(enCurso.get(10, TimeUnit.SECONDS).reserva().estado()).isEqualTo(EstadoReserva.Confirmada);
        } finally {
            catalogo.normalizar(hotel);
            hilo.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ auxiliares

    /** Lanza {@code n} tareas que arrancan a la vez; devuelve el resultado o la excepción de cada una. */
    private static List<Object> aLaVez(int n, java.util.function.IntFunction<Callable<?>> tarea) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(n);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                Callable<?> llamada = tarea.apply(i);
                futuros.add(hilos.submit(() -> {
                    salida.await();
                    try {
                        return llamada.call();
                    } catch (Exception error) {
                        return error;
                    }
                }));
            }
            salida.countDown();
            List<Object> resultados = new ArrayList<>();
            for (Future<Object> futuro : futuros) {
                resultados.add(futuro.get(30, TimeUnit.SECONDS)); // un bloqueo mutuo terminaría por tiempo
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }

    private ComandoReserva tour(String tour, int personas) {
        return new ComandoReserva(UUID.randomUUID(), UUID.randomUUID(), tour, null, ENTRADA.toString(), null, "08:00",
                null, personas, new BigDecimal(80000L * personas));
    }

    private ComandoReserva alojamiento(String unidad, int noches, int habitaciones, String total) {
        return alojamientoDesde(ENTRADA, unidad, noches, habitaciones, total);
    }

    private ComandoReserva alojamientoDesde(LocalDate entrada, String unidad, int noches, int habitaciones,
                                            String total) {
        return new ComandoReserva(UUID.fromString("00000000-0000-4000-8000-0000000000aa"), UUID.randomUUID(), hotel,
                unidad, entrada.toString(), entrada.plusDays(noches).toString(), null, habitaciones, 1,
                new BigDecimal(total));
    }
}
