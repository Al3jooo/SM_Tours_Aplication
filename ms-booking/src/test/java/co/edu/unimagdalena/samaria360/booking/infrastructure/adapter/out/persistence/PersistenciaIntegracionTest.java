package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.booking.application.port.out.ClaveIdempotenciaRepetidaException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Migración V1 contra PostgreSQL 16 (CHECK, NULLS NOT DISTINCT) y la detección de la clave repetida en el flush. */
@PruebaIntegracion
class PersistenciaIntegracionTest {

    private static final LocalDate FECHA = RelojAjustable.HOY.plusDays(40);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ReservaPersistenceAdapter reservas;
    @Autowired
    private PlatformTransactionManager transacciones;

    // ------------------------------------------------------------------ inventario_cupo

    @Test
    void losCuposNoPuedenSerNegativosNiSuperarElTotal() {
        String servicio = CatalogoSimulado.nuevoId();
        noche(servicio, CatalogoSimulado.nuevoId(), FECHA, 3, 3);

        assertViolacion(() -> jdbc.update("UPDATE inventario_cupo SET cupos_disponibles = -1"
                + " WHERE id_servicio_mongo = ?", servicio), "ck_inventario_cupo_disponibles");
        assertViolacion(() -> jdbc.update("UPDATE inventario_cupo SET cupos_disponibles = 4"
                + " WHERE id_servicio_mongo = ?", servicio), "ck_inventario_cupo_disponibles");
        assertViolacion(() -> noche(servicio, CatalogoSimulado.nuevoId(), FECHA, 0, 0), "ck_inventario_cupo_totales");
    }

    @Test
    void laFormaDeLaFilaDependeDeLaCategoria() {
        assertViolacion(() -> jdbc.update("INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha,"
                + " hora_inicio, cupos_totales, cupos_disponibles) VALUES (?, 'Alojamiento', NULL, ?, NULL, 1, 1)",
                CatalogoSimulado.nuevoId(), FECHA), "ck_inventario_cupo_forma");
        assertViolacion(() -> jdbc.update("INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha,"
                + " hora_inicio, cupos_totales, cupos_disponibles) VALUES (?, 'Tour', ?, ?, '08:00', 1, 1)",
                CatalogoSimulado.nuevoId(), CatalogoSimulado.nuevoId(), FECHA), "ck_inventario_cupo_forma");
    }

    @Test
    void laMismaNochePuedeTenerDosUnidadesPeroNoLaMismaUnidadDosVeces() {
        String servicio = CatalogoSimulado.nuevoId();
        String unidad = CatalogoSimulado.nuevoId();
        noche(servicio, unidad, FECHA, 3, 3);

        assertThatCode(() -> noche(servicio, CatalogoSimulado.nuevoId(), FECHA, 12, 12)).doesNotThrowAnyException();
        // hora_inicio es NULL en las dos: con NULLS NOT DISTINCT sí chocan.
        assertViolacion(() -> noche(servicio, unidad, FECHA, 3, 3), "uq_inventario_cupo_dia");
    }

    @Test
    void unTourNoRepiteElMismoHorario() {
        String tour = CatalogoSimulado.nuevoId();
        String sql = "INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha, hora_inicio,"
                + " cupos_totales, cupos_disponibles) VALUES (?, 'Tour', NULL, ?, ?, 12, 12)";
        jdbc.update(sql, tour, FECHA, LocalTime.of(8, 0));
        jdbc.update(sql, tour, FECHA, LocalTime.of(14, 0));

        assertViolacion(() -> jdbc.update(sql, tour, FECHA, LocalTime.of(8, 0)), "uq_inventario_cupo_dia");
    }

    // ------------------------------------------------------------------ reserva

    @Test
    void laFormaDeLaReservaDependeDeLaCategoriaYElEstadoEsCerrado() {
        assertViolacion(() -> insertarConJdbc("Alojamiento", null, "Noche", "Confirmada"), "ck_reserva_forma");
        assertViolacion(() -> insertarConJdbc("Tour", CatalogoSimulado.nuevoId(), "Persona", "Confirmada"),
                "ck_reserva_forma");
        assertViolacion(() -> insertarConJdbc("Tour", null, "Persona", "Pendiente"), "ck_reserva_estado");
    }

    @Test
    void precision1_laClaveRepetidaSeDetectaEnElFlushDentroDeLaTransaccion() {
        UUID turista = UUID.randomUUID();
        UUID clave = UUID.randomUUID();
        TransactionTemplate transaccion = new TransactionTemplate(transacciones);
        transaccion.executeWithoutResult(estado -> reservas.insertar(reserva(turista, clave)));

        AtomicReference<Throwable> enLaTransaccion = new AtomicReference<>();
        AtomicReference<Boolean> transaccionActiva = new AtomicReference<>();
        transaccion.executeWithoutResult(estado -> {
            try {
                reservas.insertar(reserva(turista, clave));
            } catch (RuntimeException error) {
                enLaTransaccion.set(error);
                transaccionActiva.set(!estado.isCompleted());
            }
            estado.setRollbackOnly();
        });

        assertThat(enLaTransaccion.get()).isInstanceOf(ClaveIdempotenciaRepetidaException.class);
        assertThat(transaccionActiva.get()).as("se detectó antes del commit").isTrue();
        assertThat(reservas.buscarPorClave(turista, clave)).isPresent();
    }

    @Test
    void insertarNoHaceMergeNiSelectPrevio() {
        // Con save() y un id asignado, Spring Data haría merge: un SELECT y después el INSERT. Con persist, un id que
        // ya existe choca contra la clave primaria en el flush.
        UUID turista = UUID.randomUUID();
        Reserva original = reserva(turista, UUID.randomUUID());
        new TransactionTemplate(transacciones).executeWithoutResult(estado -> reservas.insertar(original));
        Reserva mismoId = new Reserva(original.idReserva(), turista, original.idPrestador(), original.idServicio(),
                "Otro nombre", original.categoria(), null, null, EstadoReserva.Confirmada, 1, 1, BigDecimal.TEN,
                UnidadCobro.Persona, BigDecimal.TEN, "COP", FECHA, null, LocalTime.of(8, 0), original.fechaInicio(),
                original.fechaFin(), original.fechaCreacion(), null, UUID.randomUUID(), original.hashSolicitud());

        assertThatThrownBy(() -> new TransactionTemplate(transacciones)
                .executeWithoutResult(estado -> reservas.insertar(mismoId)))
                .hasStackTraceContaining("pk_reserva");
        assertThat(reservas.buscarDeTurista(original.idReserva(), turista).orElseThrow().nombreServicio())
                .isEqualTo("Tour de prueba");
    }

    @Test
    void v2YV3CreanLosIndicesDeMisReservasYDeRf52() {
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_reserva_turista_inicio'",
                String.class)).contains("(id_turista, fecha_inicio DESC, id_reserva DESC)");
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_reserva_confirmada_fin'",
                String.class)).contains("(fecha_fin)").contains("WHERE ((estado)::text = 'Confirmada'::text)");
    }

    // ------------------------------------------------------------------ auxiliares

    private void noche(String servicio, String unidad, LocalDate fecha, int totales, int disponibles) {
        jdbc.update("INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha, hora_inicio,"
                + " cupos_totales, cupos_disponibles) VALUES (?, 'Alojamiento', ?, ?, NULL, ?, ?)",
                servicio, unidad, fecha, totales, disponibles);
    }

    private void insertarConJdbc(String categoria, String unidad, String cobro, String estado) {
        jdbc.update("INSERT INTO reserva (id_reserva, id_turista, id_prestador, id_servicio_mongo, nombre_servicio,"
                        + " categoria, id_unidad, nombre_unidad, estado, cantidad_solicitada, personas, precio_unitario,"
                        + " unidad_cobro, monto_total, moneda, fecha_entrada, fecha_salida, hora_inicio, fecha_inicio,"
                        + " fecha_fin, fecha_creacion, clave_idempotencia, hash_solicitud) VALUES (?, ?, ?, ?, 'X', ?,"
                        + " ?, ?, ?, 1, 1, 10, ?, 10, 'COP', ?, NULL, '08:00', now() + interval '10 days',"
                        + " now() + interval '11 days', now(), ?, ?)",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CatalogoSimulado.nuevoId(), categoria,
                unidad, unidad == null ? null : "Unidad", estado, cobro, FECHA, UUID.randomUUID(), "0".repeat(64));
    }

    private static Reserva reserva(UUID turista, UUID clave) {
        java.time.Instant inicio = RelojAjustable.INICIO.plus(Duration.ofDays(40));
        return new Reserva(UUID.randomUUID(), turista, CatalogoSimulado.PRESTADOR, CatalogoSimulado.nuevoId(),
                "Tour de prueba", CategoriaReservable.Tour, null, null, EstadoReserva.Confirmada, 2, 2,
                new BigDecimal("80000"), UnidadCobro.Persona, new BigDecimal("160000"), "COP", FECHA, null,
                LocalTime.of(8, 0), inicio, inicio.plus(Duration.ofHours(4)), RelojAjustable.INICIO, null, clave,
                "a".repeat(64));
    }

    private static void assertViolacion(Runnable sentencia, String restriccion) {
        assertThatThrownBy(sentencia::run).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(restriccion);
    }
}
