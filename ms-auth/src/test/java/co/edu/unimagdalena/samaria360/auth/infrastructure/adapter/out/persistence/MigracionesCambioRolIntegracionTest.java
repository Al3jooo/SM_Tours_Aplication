package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import com.jayway.jsonpath.JsonPath;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Migraciones de «Cambiar rol de cuenta» (RF45): V15 (backfill del outbox para Gestores y Administradores, DEC-108) y
 * V16 (la revisión del prestador apunta a {@code usuario}, DEC-109). Cada prueba usa su propio PostgreSQL: un volumen
 * nuevo o una base que ya estaba en V14.
 */
class MigracionesCambioRolIntegracionTest {

    private static final String IMAGEN = "postgres:16-alpine";

    @Test
    void v15_enUnVolumenNuevoConSemillaEncolaUnEventoPorCadaGestorYAdministrador() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGEN)) {
            postgres.start();
            assertThat(flyway(postgres, null, true).migrate().success).isTrue();
            JdbcTemplate jdbc = jdbc(postgres);

            List<UUID> personal = jdbc.queryForList("SELECT u.id_usuario FROM usuario u JOIN rol r"
                    + " ON r.id_rol = u.id_rol WHERE r.nombre IN ('Gestor', 'Administrador')", UUID.class);
            assertThat(personal).hasSize(2); // gestor@ y admin@ de la semilla (V2)
            List<Map<String, Object>> eventos = jdbc.queryForList("SELECT id_usuario, tipo, carga::text AS carga"
                    + " FROM outbox_evento");
            assertThat(eventos).hasSize(personal.size());
            for (Map<String, Object> evento : eventos) {
                UUID id = (UUID) evento.get("id_usuario");
                String carga = (String) evento.get("carga");
                assertThat(personal).contains(id);
                assertThat(evento.get("tipo")).isEqualTo("CuentaActualizada");
                assertThat(JsonPath.<String>read(carga, "$.idUsuario")).isEqualTo(id.toString());
                assertThat(JsonPath.<String>read(carga, "$.rol")).isIn("Gestor", "Administrador");
                assertThat(JsonPath.<String>read(carga, "$.estadoCuenta")).isEqualTo("Activo");
                assertThat(JsonPath.<Integer>read(carga, "$.version")).isEqualTo(1);
                assertThat(jdbc.queryForObject("SELECT version_cuenta FROM usuario WHERE id_usuario = ?", Long.class,
                        id)).isEqualTo(1);
                assertThat(jdbc.queryForList("SELECT en.destino FROM outbox_entrega en JOIN outbox_evento e"
                        + " ON e.id_evento = en.id_evento WHERE e.id_usuario = ? AND en.entregado_en IS NULL",
                        String.class, id)).containsExactlyInAnyOrder("ms-catalog", "ms-booking");
            }
            // Los demás roles no cambian de versión.
            assertThat(jdbc.queryForObject("SELECT max(version_cuenta) FROM usuario u JOIN rol r ON r.id_rol = u.id_rol"
                    + " WHERE r.nombre IN ('Turista', 'Prestador')", Long.class)).isZero();
        }
    }

    @Test
    void v15_sinSemillaYSinPersonalNoEncolaNada() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGEN)) {
            postgres.start();
            assertThat(flyway(postgres, null, false).migrate().success).isTrue();

            assertThat(jdbc(postgres).queryForObject("SELECT count(*) FROM outbox_evento", Integer.class)).isZero();
        }
    }

    /**
     * Una base que ya estaba en V14, con un Administrador que cambió de estado antes (versión 3) y revisó a un
     * prestador. V15 lo encola con la versión 4. Hasta V15, borrar su fila de {@code administrador} falla por la FK de
     * V11; con V16 se puede, y la revisión conserva quién la hizo.
     */
    @Test
    void v15YV16_sobreUnaBaseExistenteNoRompenNadaYLiberanLaFilaDelAdministrador() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(IMAGEN)) {
            postgres.start();
            assertThat(flyway(postgres, "14", true).migrate().success).isTrue();
            JdbcTemplate jdbc = jdbc(postgres);
            UUID admin = UUID.randomUUID();
            UUID prestador = UUID.randomUUID();
            jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                    + " intentos_fallidos, estado_cuenta, version_cuenta) VALUES (?, 4, 'otro-admin@samaria360.test',"
                    + " 'x', CURRENT_DATE, 0, 'Activo', 3)", admin);
            jdbc.update("INSERT INTO administrador (id_usuario) VALUES (?)", admin);
            jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                    + " intentos_fallidos, estado_cuenta) VALUES (?, 2, 'revisado@hotel.test', 'x', CURRENT_DATE, 0,"
                    + " 'Activo')", prestador);
            jdbc.update("INSERT INTO prestador (id_usuario, rnt, telefono, nombre_representante_legal,"
                    + " correo_corporativo, estado_validacion, acepto_politicas, revisado_en, revisado_por)"
                    + " VALUES (?, 'RNT-REV', '1', 'Rev', 'revisado@hotel.test', 'Activo', TRUE, now(), ?)",
                    prestador, admin);

            assertThat(flyway(postgres, "15", true).migrate().success).isTrue();
            assertThat(jdbc.queryForObject("SELECT version_cuenta FROM usuario WHERE id_usuario = ?", Long.class, admin))
                    .isEqualTo(4);
            String carga = jdbc.queryForObject("SELECT carga::text FROM outbox_evento WHERE id_usuario = ?",
                    String.class, admin);
            assertThat(JsonPath.<Integer>read(carga, "$.version")).isEqualTo(4);
            assertThatThrownBy(() -> jdbc.update("DELETE FROM administrador WHERE id_usuario = ?", admin))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("fk_prestador_revisado_por");

            assertThat(flyway(postgres, null, true).migrate().success).isTrue();
            assertThat(jdbc.update("DELETE FROM administrador WHERE id_usuario = ?", admin)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT revisado_por FROM prestador WHERE id_usuario = ?", UUID.class,
                    prestador)).isEqualTo(admin);
            // Sigue habiendo integridad: un revisor que no es un usuario no se acepta.
            assertThatThrownBy(() -> jdbc.update("UPDATE prestador SET revisado_por = ? WHERE id_usuario = ?",
                    UUID.randomUUID(), prestador)).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success"
                    + " ORDER BY installed_rank", String.class)).endsWith("14", "15", "16");
        }
    }

    private static Flyway flyway(PostgreSQLContainer<?> postgres, String objetivo, boolean conSemilla) {
        var configuracion = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations(conSemilla ? new String[]{"classpath:db/migration", "classpath:db/semilla"}
                        : new String[]{"classpath:db/migration"});
        if (objetivo != null) {
            configuracion.target(objetivo);
        }
        return configuracion.load();
    }

    private static JdbcTemplate jdbc(PostgreSQLContainer<?> postgres) {
        return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword()));
    }
}
