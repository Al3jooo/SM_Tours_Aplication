package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ComandoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResultadoReserva;
import co.edu.unimagdalena.samaria360.booking.application.port.out.OfertaMaterializadaPort;
import co.edu.unimagdalena.samaria360.booking.application.service.MaterializarInventarioService;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.InventarioDePrueba;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable.HOY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Materialización bajo demanda contra PostgreSQL (DEC-152 a DEC-158): días de operación, unidades, ON CONFLICT DO
 * NOTHING, la marca de V5 y la concurrencia con otra materialización y con reservas. Reemplaza la prueba del generador
 * de inventario demo (D5, DEC-156): la oferta de la semilla produce las mismas filas.
 */
@PruebaIntegracion
class MaterializacionIntegracionTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DOS = LocalTime.of(14, 0);
    private static final int HILOS = 10;

    @Autowired
    private MaterializarInventarioService materializacion;
    @Autowired
    private OfertaMaterializadaPort marcas;
    @Autowired
    private ReservarServicioUseCase reservar;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private InventarioDePrueba inventario;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void reiniciarReloj() {
        reloj.reiniciar();
    }

    @Test
    void unTourConDiasParcialesSoloTieneFilasEnSusDiasConElAforoPorSalida() {
        String tour = catalogo.publicarTour("Tour fin de semana", 15, "80000", List.of("Sabado", "Domingo"),
                List.of("08:00", "14:00"));

        materializacion.asegurar(List.of(tour), HOY.plusDays(59));

        assertThat(filas(tour)).isEqualTo(17 * 2);
        assertThat(jdbc.queryForList("SELECT DISTINCT extract(isodow FROM fecha)::int FROM inventario_cupo"
                + " WHERE id_servicio_mongo = ?", Integer.class, tour)).containsExactlyInAnyOrder(6, 7);
        assertThat(inventario.horario(tour, HOY, DOS)).isEqualTo(15);
        assertThat(filasDel(tour, HOY.plusDays(3))).as("miércoles: no opera").isZero();
        assertThat(marcas.materializadaHasta(List.of(tour))).containsEntry(tour, HOY.plusDays(59));
    }

    @Test
    void unAlojamientoTieneUnaFilaPorUnidadYNocheConSusHabitaciones() {
        String doble = CatalogoSimulado.nuevoId();
        String suite = CatalogoSimulado.nuevoId();
        String familiar = CatalogoSimulado.nuevoId();
        String hotel = catalogo.publicarHotel("Hotel", List.of("Viernes", "Sabado"),
                new Unidad(doble, "Doble", 12, 2, "150000"), new Unidad(suite, "Suite", 3, 4, "260000"),
                new Unidad(familiar, "Familiar", 2, 6, "300000"));

        materializacion.asegurar(List.of(hotel), HOY.plusDays(59));

        assertThat(filas(hotel)).isEqualTo(16 * 3);
        LocalDate viernes = HOY.plusDays(5);
        assertThat(viernes.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(inventario.noche(hotel, doble, viernes)).isEqualTo(12);
        assertThat(inventario.noche(hotel, suite, viernes)).isEqualTo(3);
        assertThat(inventario.noche(hotel, familiar, viernes.plusDays(1))).isEqualTo(2);
        assertThat(filasDel(hotel, viernes.plusDays(2))).as("domingo: no opera").isZero();
    }

    @Test
    void materializarDosVecesNoDuplicaNiCambiaLosCuposDeFilasConReservas() {
        String tour = catalogo.publicarTour("Tour", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00"));
        materializacion.asegurar(List.of(tour), HOY.plusDays(59));
        LocalDate dia = HOY.plusDays(10);
        ResultadoReserva reserva = reservar.reservar(tour(tour, dia, 3));
        jdbc.update("DELETE FROM oferta_materializada WHERE id_servicio_mongo = ?", tour);

        materializacion.asegurar(List.of(tour), HOY.plusDays(59));

        assertThat(reserva.repetida()).isFalse();
        assertThat(filas(tour)).isEqualTo(60);
        assertThat(inventario.horario(tour, dia, OCHO)).isEqualTo(9);
        assertThat(catalogo.llamadas(tour)).as("una vez por cada materialización y otra al reservar").isEqualTo(3);
    }

    @Test
    void laMarcaSoloAvanza() {
        String servicio = CatalogoSimulado.nuevoId();

        marcas.registrar(servicio, HOY.plusDays(59), reloj.ahora());
        marcas.registrar(servicio, HOY.plusDays(10), reloj.ahora());

        assertThat(marcas.materializadaHasta(List.of(servicio, CatalogoSimulado.nuevoId())))
                .containsExactly(Map.entry(servicio, HOY.plusDays(59)));
    }

    @Test
    void dosMaterializacionesSimultaneasNoDuplicanFilas() throws Exception {
        String hotel = catalogo.publicarHotel("Hotel", CatalogoSimulado.TODOS_LOS_DIAS,
                new Unidad(CatalogoSimulado.nuevoId(), "Doble", 12, 2, "150000"),
                new Unidad(CatalogoSimulado.nuevoId(), "Suite", 3, 4, "260000"));

        List<Object> resultados = aLaVez(HILOS, i -> () -> {
            materializacion.asegurar(List.of(hotel), HOY.plusDays(59));
            return "ok";
        });

        assertThat(resultados).containsOnly("ok");
        assertThat(filas(hotel)).isEqualTo(60 * 2);
        assertThat(marcas.materializadaHasta(List.of(hotel))).containsEntry(hotel, HOY.plusDays(59));
    }

    @Test
    void materializarMientrasSeReservaNoPierdeCuposNiDuplicaFilas() throws Exception {
        String tour = catalogo.publicarTour("Tour", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00"));
        LocalDate dia = HOY.plusDays(20);

        // La mitad reserva (y materializa al preparar); la otra mitad materializa sin la marca.
        List<Object> resultados = aLaVez(HILOS, i -> i % 2 == 0
                ? () -> reservar.reservar(tour(tour, dia, 2))
                : () -> {
                    jdbc.update("DELETE FROM oferta_materializada WHERE id_servicio_mongo = ?", tour);
                    materializacion.asegurar(List.of(tour), HOY.plusDays(59));
                    return "materializada";
                });

        assertThat(resultados).filteredOn(ResultadoReserva.class::isInstance).hasSize(HILOS / 2);
        assertThat(resultados).filteredOn("materializada"::equals).hasSize(HILOS / 2);
        assertThat(filas(tour)).isEqualTo(60);
        assertThat(inventario.horario(tour, dia, OCHO)).isEqualTo(12 - 2 * (HILOS / 2));
        assertThat(inventario.reservasDe(tour)).isEqualTo(HILOS / 2);
    }

    /** DEC-156: con la oferta que M008 rellenó (todos los días; 08:00 y 14:00), las filas son las del generador D5. */
    @Test
    void conLaOfertaDeLaSemillaElInventarioEsElDelGeneradorD5() {
        String doble = CatalogoSimulado.nuevoId();
        String suite = CatalogoSimulado.nuevoId();
        String hotel = catalogo.publicarHotel("Hotel Casa Tayrona", CatalogoSimulado.TODOS_LOS_DIAS,
                new Unidad(doble, "Doble", 12, 2, "150000"), new Unidad(suite, "Suite familiar", 3, 4, "260000"));
        String tour = catalogo.publicarTour("Tour de Café en Minca", 15, "150000", CatalogoSimulado.TODOS_LOS_DIAS,
                List.of("08:00", "14:00"));

        materializacion.asegurar(List.of(hotel, tour), HOY.plusDays(59));

        // Lo que comprobaba GeneradorInventarioIntegracionTest: 60 × 2 en cada uno, con la capacidad del catálogo.
        assertThat(filas(hotel)).isEqualTo(120);
        assertThat(filas(tour)).isEqualTo(120);
        assertThat(inventario.noche(hotel, suite, HOY)).isEqualTo(3);
        assertThat(inventario.noche(hotel, doble, HOY.plusDays(59))).isEqualTo(12);
        assertThat(inventario.horario(tour, HOY, OCHO)).isEqualTo(15);
        assertThat(inventario.horario(tour, HOY.plusDays(59), DOS)).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario_cupo WHERE id_servicio_mongo IN (?, ?)"
                + " AND (fecha < ? OR fecha > ? OR cupos_disponibles <> cupos_totales)", Integer.class, hotel, tour,
                HOY, HOY.plusDays(59))).isZero();
    }

    // ------------------------------------------------------------------ auxiliares

    private int filas(String servicio) {
        return jdbc.queryForObject("SELECT count(*) FROM inventario_cupo WHERE id_servicio_mongo = ?", Integer.class,
                servicio);
    }

    private int filasDel(String servicio, LocalDate fecha) {
        return jdbc.queryForObject("SELECT count(*) FROM inventario_cupo WHERE id_servicio_mongo = ? AND fecha = ?",
                Integer.class, servicio, fecha);
    }

    private static ComandoReserva tour(String tour, LocalDate fecha, int personas) {
        return new ComandoReserva(UUID.randomUUID(), UUID.randomUUID(), tour, null, fecha.toString(), null, "08:00",
                null, personas, new BigDecimal(80000L * personas));
    }

    /** Lanza {@code n} tareas que arrancan a la vez; devuelve el resultado o la excepción de cada una. */
    private static List<Object> aLaVez(int n, IntFunction<Callable<?>> tarea) throws Exception {
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
}
